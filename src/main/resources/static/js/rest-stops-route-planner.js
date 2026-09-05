/**
 * rest-stops-route-planner.js — 지도 화면의 "경로 계획" 하나로 묶이는 두 흐름을 갖는다:
 * 출발/도착 선택(검색 모달·지도 클릭)과 그 결과로 경로를 찾아 보여주는 것(경로 결과 시트,
 * 경로 폴리라인·경로상 휴게소 마커, 전국 평균 유가). 둘은 원래 개념 보고서에서 별도 모듈로
 * 나뉘어 있었지만, 출발/도착이 바뀔 때마다 경로 재요청·결과 무효화가 서로를 계속 오가는
 * 하나의 연속된 사용자 흐름이라 실제로는 한 모듈로 두는 게 더 읽기 쉽다.
 *
 * 지도 위에 직접 그리는 모든 것(출발/도착 마커, 지도 클릭 드래프트 마커, 경로 폴리라인,
 * 경로상 휴게소 마커, 팝업)은 getMapView()가 반환하는 rest-stops-map-view.js 인스턴스의
 * map/naverMaps 원본에 직접 접근한다 — mapView가 생성되기 전에 이 모듈이 먼저 만들어지는
 * 순서 때문에 getMapView는 지연 접근자(lazy getter)다.
 */
import { closeDialogById, openDialogById, showApiUnavailableAlert, hideGlobalLoading, showGlobalLoading, setText } from './utils.js';
import { formatText } from './rest-stop-detail-formatters.js';
import { createCandidateListItem } from './candidate-list-item.js';
import { createRouteRestStopRequest } from './route-rest-stop-request.js';
import { createNationalOilPriceRequest } from './national-oil-price-request.js';
import { createPlaceSearchRequest } from './place-search-request.js';
import { ROUTE_POINT_TARGET, createRoutePointSelection } from './route-point-selection.js';
import { createRouteRestStopView, renderNationalOilPriceState } from './route-rest-stop-view.js';
import { isMobileDetailSheet } from './rest-stops-detail-panel.js';
import { trackScreenView } from './analytics.js';

export function routePointLabel(point, fallback) {
    if (!point) {
        return fallback;
    }

    const name = formatText(point.name, fallback);
    const address = typeof point.address === 'string' ? point.address.trim() : '';
    return address === '' ? name : `${name} · ${address}`;
}

export function routeMapSelectionMessage(target, hasDraft) {
    const pointName = target === ROUTE_POINT_TARGET.ORIGIN ? '출발' : '도착';
    return hasDraft
        ? `선택한 ${pointName} 위치를 확정해주세요.`
        : `지도에서 ${pointName} 위치를 선택하세요.`;
}

export function canRequestRouteAutomatically(origin, destination) {
    return Boolean(origin && destination)
        && Number.isFinite(origin.latitude)
        && Number.isFinite(origin.longitude)
        && Number.isFinite(destination.latitude)
        && Number.isFinite(destination.longitude);
}

export function isRouteGlobalLoadingState(state) {
    return state?.status === 'loading';
}

export function initRestStopRoutePlanner(document, window, {
    signal,
    getMapView,
    openDetailPanel,
    getBottomSheetController
} = {}) {
    const routePointSelection = createRoutePointSelection();
    let routePolylines = [];
    let routeMarkers = [];
    let currentRouteData;
    let selectedRouteIndex = 0;
    let originMarker;
    let destinationMarker;
    let currentRouteRestStops = [];
    let currentNationalOilPriceSummary;
    let routeMapClickListener;
    let routeMapDraftMarker;
    let routeMapSelectionTrigger;
    let automaticRouteRequestSignature;

    const routeView = createRouteRestStopView({ onSelectRestStop: selectRouteRestStop });
    const routeRequest = createRouteRestStopRequest({ onState: renderRouteState });
    const nationalOilPriceRequest = createNationalOilPriceRequest({ onState: handleNationalOilPriceState });
    const placeSearchRequest = createPlaceSearchRequest({ onState: renderPlaceSearchState });

    function bindRouteSearch() {
        document.getElementById('routeOriginChangeButton')?.addEventListener('click', openRouteOriginModal, { signal });
        document.getElementById('routeOriginModalClose')?.addEventListener('click', closeRouteOriginModal, { signal });
        document.getElementById('routeOriginModal')?.addEventListener('click', (event) => {
            if (event.target === event.currentTarget) {
                closeRouteOriginModal();
            }
        }, { signal });
        document.getElementById('routeOriginCurrentButton')?.addEventListener('click', selectCurrentLocationAsOrigin, {
            signal
        });
        document.getElementById('routeOriginSearchButton')?.addEventListener('click', searchOrigin, { signal });
        document.getElementById('routeOriginSearchInput')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                searchOrigin();
            }
        }, { signal });
        document.getElementById('routeOriginMapButton')?.addEventListener('click', (event) => {
            beginRouteMapSelection(ROUTE_POINT_TARGET.ORIGIN, event.currentTarget);
        }, { signal });
        document.getElementById('routeDestinationSearchButton')?.addEventListener('click', searchDestination, { signal });
        document.getElementById('routeDestinationInput')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                searchDestination();
            }
        }, { signal });
        document.getElementById('routeDestinationInput')?.addEventListener('input', clearEditedDestination, { signal });
        document.getElementById('routeDestinationMapButton')?.addEventListener('click', (event) => {
            beginRouteMapSelection(ROUTE_POINT_TARGET.DESTINATION, event.currentTarget);
        }, { signal });
        document.getElementById('routePointSummaryToggle')?.addEventListener('click', toggleRoutePointFields, { signal });
        document.getElementById('routeMapSelectionConfirm')?.addEventListener('click', confirmRouteMapSelection, { signal });
        document.getElementById('routeMapSelectionCancel')?.addEventListener('click', cancelRouteMapSelection, { signal });
        document.getElementById('routeResultOpen')?.addEventListener('click', openRouteResultModal, { signal });
        document.getElementById('routeResultModalClose')?.addEventListener('click', closeRouteResultModal, { signal });
        document.getElementById('routeResultModal')?.addEventListener('click', (event) => {
            if (event.target === event.currentTarget) {
                closeRouteResultModal();
            }
        }, { signal });
        document.getElementById('placeCandidateModalClose')?.addEventListener('click', closePlaceCandidateModal, {
            signal
        });
        document.getElementById('placeCandidateModal')?.addEventListener('click', (event) => {
            if (event.target === event.currentTarget) {
                closePlaceCandidateModal();
            }
        }, { signal });
        updateRoutePointSummary();
    }

    function openRouteOriginModal() {
        openDialogById('routeOriginModal');
    }

    function closeRouteOriginModal() {
        closeDialogById('routeOriginModal');
    }

    function selectCurrentLocationAsOrigin() {
        if (!navigator.geolocation) {
            setRouteStatus('이 브라우저는 위치 기능을 지원하지 않습니다.');
            return;
        }

        setRouteStatus('현재 위치를 확인하는 중입니다...');
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const point = { latitude: position.coords.latitude, longitude: position.coords.longitude };
                getMapView().setCurrentLocation(point);
                getMapView().hideLocateError();
                selectCurrentLocationOrigin({ moveMap: true });
                closeRouteOriginModal();
                setRouteStatus('현재 위치를 출발지로 설정했습니다.');
                requestRouteAutomatically();
            },
            (error) => setRouteStatus(locateErrorMessage(error)),
            { enableHighAccuracy: false, maximumAge: 300000, timeout: 5000 }
        );
    }

    function locateErrorMessage(error) {
        if (error?.code === 1) {
            return '위치 권한이 거부되어 현재 위치를 표시할 수 없습니다.';
        }

        return '현재 위치를 가져오지 못했습니다. 잠시 후 다시 시도해주세요.';
    }

    function initializeMobileCurrentLocationOrigin() {
        const currentLocation = getMapView().getCurrentLocation();
        if (!isMobileDetailSheet(window) || !currentLocation || routePointSelection.getOrigin()) {
            return;
        }

        selectCurrentLocationOrigin({ moveMap: false });
        getMapView().showCurrentLocationMarker(
            new (getMapView().getNaverMaps()).LatLng(currentLocation.latitude, currentLocation.longitude)
        );
        setRoutePointFieldsExpanded(true);
        setRouteStatus('현재 위치를 출발지로 설정했습니다. 도착지를 선택해주세요.');
    }

    function selectCurrentLocationOrigin({ moveMap = false } = {}) {
        const previousSignature = currentRouteSelectionSignature();
        const selectedOrigin = routePointSelection.select(ROUTE_POINT_TARGET.ORIGIN, {
            name: '현재 위치',
            ...getMapView().getCurrentLocation()
        });
        renderEndpointMarker(ROUTE_POINT_TARGET.ORIGIN, selectedOrigin);
        if (moveMap) {
            getMapView().panTo(selectedOrigin);
        }
        renderSelectedOrigin();
        invalidateRouteResultIfSelectionChanged(previousSignature);
        return selectedOrigin;
    }

    function openRouteResultModal() {
        openDialogById('routeResultModal', {
            guard: () => currentRouteRestStops.length > 0,
            onOpened: () => trackScreenView('route_results')
        });
    }

    function closeRouteResultModal() {
        closeDialogById('routeResultModal', () => getBottomSheetController?.()?.resetHeight());
    }

    function toggleRouteResultButton(visible) {
        document.getElementById('routeResultOpen')?.classList.toggle('d-none', !visible);
    }

    function searchOrigin() {
        const query = document.getElementById('routeOriginSearchInput')?.value.trim() ?? '';
        searchPlace(ROUTE_POINT_TARGET.ORIGIN, query);
    }

    function searchDestination() {
        const query = document.getElementById('routeDestinationInput')?.value.trim() ?? '';
        searchPlace(ROUTE_POINT_TARGET.DESTINATION, query);
    }

    function searchPlace(target, query) {
        if (query === '') {
            setRouteStatus(`${routePointName(target)} 검색어를 입력해주세요.`);
            return;
        }

        routePointSelection.setSearchTarget(target);
        placeSearchRequest?.load(query);
    }

    function clearEditedDestination() {
        if (!routePointSelection.getDestination()) {
            updateRoutePointSummary();
            return;
        }

        const previousSignature = currentRouteSelectionSignature();
        routePointSelection.clear(ROUTE_POINT_TARGET.DESTINATION);
        invalidateRouteResultIfSelectionChanged(previousSignature);
        setRouteStatus('변경한 도착지를 검색하고 후보를 선택해주세요.');
        updateRoutePointSummary();
    }

    function requestSelectedRoute() {
        const origin = routePointSelection.getOrigin();
        if (!origin) {
            setRouteStatus('출발지를 설정해주세요.');
            openRouteOriginModal();
            return;
        }

        const destination = routePointSelection.getDestination();
        if (!destination) {
            const query = document.getElementById('routeDestinationInput')?.value.trim() ?? '';
            if (query !== '') {
                searchDestination();
                return;
            }
            setRouteStatus('도착지를 검색하거나 지도에서 선택해주세요.');
            return;
        }

        routeRequest?.load(
            origin.latitude,
            origin.longitude,
            null,
            destination.latitude,
            destination.longitude,
            destination.name
        );
    }

    function requestRouteAutomatically() {
        if (!routePointSelection.canRequestRoute()) {
            return;
        }

        const origin = routePointSelection.getOrigin();
        const destination = routePointSelection.getDestination();
        const signature = routeRequestSignature(origin, destination);
        if (signature === automaticRouteRequestSignature) {
            return;
        }

        automaticRouteRequestSignature = signature;
        requestSelectedRoute();
    }

    function routeRequestSignature(origin, destination) {
        return [
            origin.latitude,
            origin.longitude,
            destination.latitude,
            destination.longitude,
            destination.name ?? ''
        ].join('|');
    }

    function currentRouteSelectionSignature() {
        if (!routePointSelection.canRequestRoute()) {
            return undefined;
        }
        const origin = routePointSelection.getOrigin();
        const destination = routePointSelection.getDestination();
        return routeRequestSignature(origin, destination);
    }

    function invalidateRouteResultIfSelectionChanged(previousSignature) {
        if (previousSignature === currentRouteSelectionSignature()) {
            return;
        }

        automaticRouteRequestSignature = undefined;
        routeRequest?.invalidate();
        clearRouteOverlays();
        routeView.renderList([], null);
        toggleRouteResultButton(false);
        closeRouteResultModal();
    }

    function renderSelectedOrigin() {
        const output = document.getElementById('routeOriginValue');
        const button = document.getElementById('routeOriginChangeButton');
        const origin = routePointSelection.getOrigin();
        if (output) {
            output.textContent = routePointLabel(origin, '출발지를 설정하세요');
            output.classList.toggle('is-selected', Boolean(origin));
        }
        if (button) {
            button.textContent = origin ? '변경' : '설정';
        }
        updateRoutePointSummary();
    }

    function renderSelectedDestination() {
        const input = document.getElementById('routeDestinationInput');
        const destination = routePointSelection.getDestination();
        if (input && destination) {
            input.value = routePointLabel(destination, '');
        }
        updateRoutePointSummary();
    }

    function toggleRoutePointFields() {
        const form = document.querySelector('.route-point-search');
        setRoutePointFieldsExpanded(!form?.classList.contains('is-expanded'));
    }

    function setRoutePointFieldsExpanded(expanded) {
        const form = document.querySelector('.route-point-search');
        const toggle = document.getElementById('routePointSummaryToggle');
        const action = document.getElementById('routePointSummaryAction');
        form?.classList.toggle('is-expanded', expanded);
        toggle?.setAttribute('aria-expanded', expanded ? 'true' : 'false');
        if (action) {
            action.textContent = expanded ? '접기' : '수정';
        }
    }

    function updateRoutePointSummary() {
        const origin = routePointSelection.getOrigin();
        const destination = routePointSelection.getDestination();
        const destinationQuery = document.getElementById('routeDestinationInput')?.value.trim() ?? '';

        setText('routeOriginSummary', routePointSummaryLabel(origin, '출발지 설정'));
        setText(
            'routeDestinationSummary',
            destination
                ? routePointSummaryLabel(destination, '도착지 입력')
                : formatText(destinationQuery, '도착지 입력')
        );
    }

    function routePointSummaryLabel(point, fallback) {
        if (!point) {
            return fallback;
        }
        return formatText(point.name, fallback);
    }

    function routePointName(target) {
        return target === ROUTE_POINT_TARGET.ORIGIN ? '출발지' : '도착지';
    }

    function renderPlaceSearchState(state) {
        const pointName = routePointName(routePointSelection.getSearchTarget());
        if (state.status === 'loading') {
            setRouteStatus(`${pointName}를 검색하는 중입니다...`);
            return;
        }

        if (state.status === 'success') {
            if (state.candidates.length === 0) {
                setRouteStatus('검색 결과가 없습니다. 다른 검색어를 입력해보세요.');
                return;
            }
            closeRouteOriginModal();
            renderCandidates(state.candidates);
            openPlaceCandidateModal();
            setRouteStatus(`후보 ${state.candidates.length}곳 중 ${pointName}를 선택하세요.`);
            return;
        }

        if (state.status === 'external-unavailable') {
            showApiUnavailableAlert();
            setRouteStatus(`일시적으로 ${pointName}를 검색하지 못했습니다. 잠시 후 다시 시도해주세요.`);
            return;
        }

        setRouteStatus(`${pointName} 검색에 실패했습니다. 잠시 후 다시 시도해주세요.`);
    }

    function renderCandidates(candidates) {
        const list = document.getElementById('placeCandidateList');
        const title = document.getElementById('placeCandidateModalTitle');
        if (!list) {
            return;
        }

        if (title) {
            title.textContent = `${routePointName(routePointSelection.getSearchTarget())} 선택`;
        }
        list.replaceChildren();
        candidates.forEach((candidate) => list.appendChild(createCandidateItem(candidate)));
    }

    function createCandidateItem(candidate) {
        return createCandidateListItem(document, {
            itemClassName: 'route-result-item route-candidate-item',
            buttonClassName: 'route-candidate-button',
            primaryClassName: 'route-result-name',
            secondaryClassName: 'route-result-meta',
            primaryText: formatText(candidate?.name, '이름 정보 없음'),
            secondaryText: formatText(candidate?.address, '주소 정보 없음'),
            onSelect: () => selectPlaceCandidate(candidate)
        });
    }

    function selectPlaceCandidate(candidate) {
        const target = routePointSelection.getSearchTarget();
        const previousSignature = currentRouteSelectionSignature();
        const selected = routePointSelection.select(target, candidate);
        closePlaceCandidateModal();

        renderEndpointMarker(target, selected);
        getMapView().panTo(selected);

        if (target === ROUTE_POINT_TARGET.ORIGIN) {
            renderSelectedOrigin();
        }
        if (target === ROUTE_POINT_TARGET.DESTINATION) {
            renderSelectedDestination();
            setRoutePointFieldsExpanded(false);
        }
        invalidateRouteResultIfSelectionChanged(previousSignature);
        setRouteStatus(`${routePointName(target)}를 설정했습니다.`);
        requestRouteAutomatically();
    }

    function openPlaceCandidateModal() {
        openDialogById('placeCandidateModal');
    }

    function closePlaceCandidateModal() {
        closeDialogById('placeCandidateModal');
    }

    function bindRouteMapClick() {
        removeRouteMapClickListener();
        const map = getMapView().getMap();
        const naverMaps = getMapView().getNaverMaps();
        if (!map || !naverMaps) {
            return;
        }

        routeMapClickListener = naverMaps.Event.addListener(map, 'click', handleRouteMapClick);
    }

    function removeRouteMapClickListener() {
        const naverMaps = getMapView().getNaverMaps();
        if (routeMapClickListener && naverMaps?.Event) {
            naverMaps.Event.removeListener(routeMapClickListener);
        }
        routeMapClickListener = undefined;
    }

    function beginRouteMapSelection(target, trigger) {
        if (!getMapView().getMap() || !getMapView().getNaverMaps()) {
            setRouteStatus('지도를 불러온 뒤 위치를 선택해주세요.');
            return;
        }

        closeRouteOriginModal();
        closePlaceCandidateModal();
        routePointSelection.beginMapSelection(target);
        routeMapSelectionTrigger = target === ROUTE_POINT_TARGET.ORIGIN
            ? document.getElementById('routeOriginChangeButton')
            : trigger;
        clearRouteMapDraftMarker();

        const toolbar = document.getElementById('routeMapSelectionToolbar');
        const confirm = document.getElementById('routeMapSelectionConfirm');
        toolbar?.classList.remove('d-none');
        if (confirm) {
            confirm.disabled = true;
        }
        updateRouteMapSelectionMessage(false);
        document.getElementById('restStopMap')?.focus();
    }

    function handleRouteMapClick(event) {
        if (!routePointSelection.getMapTarget()) {
            return;
        }

        const latitude = event?.coord?.lat();
        const longitude = event?.coord?.lng();
        const draft = routePointSelection.updateMapDraft({ latitude, longitude });
        renderRouteMapDraftMarker(draft);
        updateRouteMapSelectionMessage(true);

        const confirm = document.getElementById('routeMapSelectionConfirm');
        if (confirm) {
            confirm.disabled = false;
        }
    }

    function renderRouteMapDraftMarker(point) {
        const naverMaps = getMapView().getNaverMaps();
        const position = new naverMaps.LatLng(point.latitude, point.longitude);
        if (routeMapDraftMarker) {
            routeMapDraftMarker.setPosition(position);
            return;
        }

        routeMapDraftMarker = new naverMaps.Marker({
            map: getMapView().getMap(),
            position,
            icon: {
                content: '<div class="route-map-draft-marker"></div>',
                anchor: new naverMaps.Point(12, 24)
            },
            zIndex: 1100
        });
    }

    function updateRouteMapSelectionMessage(hasDraft) {
        setText(
            'routeMapSelectionMessage',
            routeMapSelectionMessage(routePointSelection.getMapTarget(), hasDraft)
        );
    }

    function confirmRouteMapSelection() {
        const target = routePointSelection.getMapTarget();
        const previousSignature = currentRouteSelectionSignature();
        const selected = routePointSelection.confirmMapSelection();
        if (!selected) {
            return;
        }

        if (target === ROUTE_POINT_TARGET.ORIGIN) {
            renderSelectedOrigin();
        }
        if (target === ROUTE_POINT_TARGET.DESTINATION) {
            renderSelectedDestination();
        }
        finishRouteMapSelection();
        invalidateRouteResultIfSelectionChanged(previousSignature);
        setRouteStatus(`${routePointName(target)}를 지도 위치로 설정했습니다.`);
        requestRouteAutomatically();
    }

    function cancelRouteMapSelection() {
        const target = routePointSelection.getMapTarget();
        if (!target) {
            return;
        }

        finishRouteMapSelection();
        setRouteStatus(`${routePointName(target)} 지도 선택을 취소했습니다.`);
    }

    function finishRouteMapSelection() {
        routePointSelection.cancelMapSelection();
        clearRouteMapDraftMarker();
        document.getElementById('routeMapSelectionToolbar')?.classList.add('d-none');
        setText('routeMapSelectionMessage', '');

        const confirm = document.getElementById('routeMapSelectionConfirm');
        if (confirm) {
            confirm.disabled = true;
        }
        routeMapSelectionTrigger?.focus();
        routeMapSelectionTrigger = undefined;
    }

    function clearRouteMapDraftMarker() {
        routeMapDraftMarker?.setMap(null);
        routeMapDraftMarker = undefined;
    }

    /**
     * renderNationalOilPriceState(route-rest-stop-view.js)는 전국 평균 유가 위젯 DOM만 갱신하는
     * 순수 렌더러다 — 필터를 다시 누를 때 쓸 최신 요약값은 이 모듈과 routeView 양쪽이 각자
     * 필요할 때 쓸 수 있게 여기서 currentNationalOilPriceSummary와 routeView 캐시를 함께 갱신한다.
     */
    function handleNationalOilPriceState(state) {
        currentNationalOilPriceSummary = state.status === 'success' ? state.data : null;
        routeView.setNationalOilPriceSummary(currentNationalOilPriceSummary);
        renderNationalOilPriceState(state);
    }

    function renderRouteState(state) {
        setRouteGlobalLoading(isRouteGlobalLoadingState(state), '경로를 찾는 중입니다...');

        if (state.status === 'loading') {
            setRouteStatus('경로를 찾는 중입니다...');
            return;
        }

        if (state.status === 'idle') {
            return;
        }

        if (state.status === 'success') {
            renderRoute(state.data);
            return;
        }

        clearRouteOverlays();
        routeView.renderList([], null);
        toggleRouteResultButton(false);
        closeRouteResultModal();
        if (state.status === 'not-found') {
            setRouteStatus(state.message || '목적지 또는 경로를 찾지 못했습니다.');
            return;
        }

        if (state.status === 'external-unavailable') {
            showApiUnavailableAlert();
            setRouteStatus('일시적으로 경로를 가져오지 못했습니다. 잠시 후 다시 시도해주세요.');
            return;
        }

        setRouteStatus('경로를 가져오지 못했습니다. 잠시 후 다시 시도해주세요.');
    }

    function renderRoute(data) {
        clearRouteOverlays();
        currentRouteData = data;
        selectedRouteIndex = 0;
        routeView.reset();
        renderRouteSelection();
        openRouteResultModal();
        getMapView().setMarkerMode('route');
    }

    function selectRoute(index) {
        const routes = Array.isArray(currentRouteData?.routes) ? currentRouteData.routes : [];
        if (!routes[index] || index === selectedRouteIndex) {
            return;
        }

        selectedRouteIndex = index;
        renderRouteSelection();
    }

    function renderRouteSelection() {
        const naverMaps = getMapView().getNaverMaps();
        const map = getMapView().getMap();
        const routes = Array.isArray(currentRouteData?.routes) ? currentRouteData.routes : [];
        const selected = routes[selectedRouteIndex];

        clearRoutePolylines();
        renderRoutePolylines(routes, selectedRouteIndex);
        renderEndpointMarkers(currentRouteData?.destination);
        routeView.renderOptionCards(routes, selectedRouteIndex, selectRoute);

        clearRouteMarkers();
        const restStops = Array.isArray(selected?.restStops) ? selected.restStops : [];
        currentNationalOilPriceSummary = null;
        restStops.forEach((restStop) => {
            const position = new naverMaps.LatLng(restStop.latitude, restStop.longitude);
            const marker = new naverMaps.Marker({
                map,
                position,
                icon: {
                    content: '<div class="route-rest-stop-marker"></div>',
                    anchor: new naverMaps.Point(7, 7)
                },
                zIndex: 900
            });

            naverMaps.Event.addListener(marker, 'click', () => {
                if (routePointSelection.getMapTarget()) {
                    return;
                }
                getMapView().openPopupAt(restStop, { latitude: restStop.latitude, longitude: restStop.longitude });
            });

            routeMarkers.push(marker);
        });

        currentRouteRestStops = restStops;
        routeView.renderList(restStops, currentNationalOilPriceSummary);
        nationalOilPriceRequest?.load();
        const destinationName = currentRouteData?.destination?.name ?? '목적지';
        setRouteStatus(`${destinationName}까지 경로상 휴게소 ${restStops.length}곳`);

        const button = document.getElementById('routeResultOpen');
        if (button) {
            button.textContent = `경로 결과 ${restStops.length}곳`;
        }
        toggleRouteResultButton(restStops.length > 0);
    }

    function renderRoutePolylines(routes, selectedIndex) {
        const naverMaps = getMapView().getNaverMaps();
        const map = getMapView().getMap();
        routes.forEach((route, index) => {
            const path = Array.isArray(route?.summary?.path) ? route.summary.path : [];
            const latLngs = path
                .filter((point) => Array.isArray(point) && point.length === 2)
                .map((point) => new naverMaps.LatLng(point[1], point[0]));
            if (latLngs.length === 0) {
                return;
            }

            const isSelected = index === selectedIndex;
            const polyline = new naverMaps.Polyline({
                map,
                path: latLngs,
                strokeColor: isSelected ? '#0d6efd' : '#adb5bd',
                strokeWeight: isSelected ? 5 : 3,
                strokeOpacity: isSelected ? 0.85 : 0.6,
                strokeStyle: isSelected ? 'solid' : 'shortdash'
            });
            naverMaps.Event.addListener(polyline, 'click', () => selectRoute(index));
            routePolylines.push(polyline);

            if (isSelected) {
                fitMapToPath(latLngs);
            }
        });
    }

    function renderEndpointMarker(target, point) {
        const map = getMapView().getMap();
        const naverMaps = getMapView().getNaverMaps();
        if (!map || !naverMaps || !point) {
            return;
        }
        if (!Number.isFinite(point.latitude) || !Number.isFinite(point.longitude)) {
            return;
        }

        const position = new naverMaps.LatLng(point.latitude, point.longitude);
        const isOrigin = target === ROUTE_POINT_TARGET.ORIGIN;
        const className = isOrigin ? 'route-origin-marker' : 'route-destination-marker';
        const label = isOrigin ? '출발' : '도착';

        if (isOrigin && originMarker) {
            originMarker.setMap(null);
        }
        if (!isOrigin && destinationMarker) {
            destinationMarker.setMap(null);
        }

        const marker = new naverMaps.Marker({
            map,
            position,
            icon: {
                content: `<div class="route-endpoint-marker ${className}">${label}</div>`,
                anchor: new naverMaps.Point(18, 28)
            },
            zIndex: 1000
        });

        if (isOrigin) {
            originMarker = marker;
        } else {
            destinationMarker = marker;
        }
    }

    function renderEndpointMarkers(destination) {
        renderEndpointMarker(ROUTE_POINT_TARGET.ORIGIN, routePointSelection.getOrigin());
        renderEndpointMarker(ROUTE_POINT_TARGET.DESTINATION, destination);
    }

    function fitMapToPath(latLngs) {
        const naverMaps = getMapView().getNaverMaps();
        const bounds = new naverMaps.LatLngBounds(latLngs[0], latLngs[0]);
        latLngs.forEach((latLng) => bounds.extend(latLng));
        getMapView().getMap().fitBounds(bounds);
    }

    function selectRouteRestStop(restStop) {
        closeRouteResultModal();

        if (Number.isFinite(restStop?.latitude) && Number.isFinite(restStop?.longitude)) {
            getMapView().openPopupAt(restStop, { latitude: restStop.latitude, longitude: restStop.longitude }, {
                fromRouteResult: true
            });
            return;
        }

        openDetailPanel?.({
            serviceAreaCode: restStop?.serviceAreaCode,
            unitName: restStop?.unitName
        }, {
            fromRouteResult: true
        });
    }

    function clearRoutePolylines() {
        routePolylines.forEach((polyline) => polyline.setMap(null));
        routePolylines = [];
    }

    function clearRouteMarkers() {
        routeMarkers.forEach((marker) => marker.setMap(null));
        routeMarkers = [];
    }

    function clearRouteOverlays() {
        nationalOilPriceRequest?.invalidate();
        clearRoutePolylines();

        if (originMarker) {
            originMarker.setMap(null);
            originMarker = undefined;
        }

        if (destinationMarker) {
            destinationMarker.setMap(null);
            destinationMarker = undefined;
        }

        clearRouteMarkers();
        currentRouteRestStops = [];
        currentNationalOilPriceSummary = null;
        currentRouteData = undefined;
        selectedRouteIndex = 0;
        routeView.reset();

        const routeOptions = document.getElementById('routeOptions');
        if (routeOptions) {
            routeOptions.replaceChildren();
            routeOptions.classList.add('d-none');
        }
    }

    function setRouteStatus(message) {
        setText('routeSearchStatus', message);
    }

    function setRouteGlobalLoading(loading, message) {
        if (loading) {
            showGlobalLoading(message);
            return;
        }

        hideGlobalLoading();
    }

    return {
        bindRouteSearch,
        bindRouteMapClick,
        initializeMobileCurrentLocationOrigin,
        isMapClickActive: () => Boolean(routePointSelection.getMapTarget()),
        cancelMapSelection: cancelRouteMapSelection,
        openRouteResultModal
    };
}

import { hideGlobalLoading, setText, showApiUnavailableAlert, showGlobalLoading } from './utils.js';
import {
    availableDataTags,
    CONVENIENCE_FALLBACK,
    formatAvailability,
    formatFoodBadges,
    formatFoodCost,
    formatFreightOperation,
    formatOilPrice,
    formatOperationTime,
    formatParkingBreakdown,
    formatParkingCount,
    formatRefreshedAt,
    formatTotalParkingCount,
    formatSalesRankingMonth,
    formatText,
    hasFoodMenu,
    hasFoodSections,
    isMissingValue,
    normalizeSalesRankingStoreName,
    orderFoodMenus,
    parseConvenience,
    sortSalesRankingProducts,
    sortSalesRankingStores
} from './rest-stop-detail-formatters.js';
import { createRestStopDetailRequest } from './rest-stop-detail-request.js';
import { createRouteRestStopRequest } from './route-rest-stop-request.js';
import { createNationalOilPriceRequest } from './national-oil-price-request.js';
import { createPlaceSearchRequest } from './place-search-request.js';
import { createRestStopNameSearchRequest } from './rest-stop-name-search-request.js';
import { createRestStopCompareRequest } from './rest-stop-compare-request.js';
import { createRouteRestStopImage, renderDetailImage } from './rest-stop-images.js';
import {
    ROUTE_POINT_TARGET,
    createRoutePointSelection
} from './route-point-selection.js';

const MAP_CONFIG_ENDPOINT = '/api/map-config';
const REST_STOPS_ENDPOINT = '/api/rest-stops';
const NAVER_MAPS_SCRIPT_ID = 'naverMapsScript';
const NAVER_MAPS_SCRIPT_URL = 'https://oapi.map.naver.com/openapi/v3/maps.js';
const SEOUL_CENTER = {
    latitude: 37.5665,
    longitude: 126.978
};
const DEFAULT_ZOOM = 11;
const MOBILE_DETAIL_SHEET_MEDIA = '(max-width: 991.98px)';

const GEOLOCATION_OPTIONS = {
    enableHighAccuracy: false,
    maximumAge: 300000,
    timeout: 5000
};

let map;
let naverMaps;
let selectedInfoWindow;
let selectedRestStopName = '';
let selectedServiceAreaCode = '';
let currentDetail;
let detailRequest;
let detailPanelEventController;
let mapInitializationId = 0;
let currentLocation;
let currentLocationMarker;
let foodExpanded = false;
let currentFoodMenus = [];
let currentFoodSections = [];
let routeRequest;
let placeSearchRequest;
let restStopNameSearchRequest;
let restStopCompareSearchRequestA;
let restStopCompareSearchRequestB;
let restStopCompareRequest;
let restStopCompareSelectionA;
let restStopCompareSelectionB;
let routePolyline;
let routeMarkers = [];
let allRestStopMarkers = [];
let markerMode = 'all';
let originMarker;
let destinationMarker;
let currentRouteRestStops = [];
let currentNationalOilPriceSummary;
let nationalOilPriceRequest;
let detailOpenedFromRouteResult = false;
let routePointSelection = createRoutePointSelection();
let routeMapClickListener;
let routeMapDraftMarker;
let routeMapSelectionTrigger;
let automaticRouteRequestSignature;

export async function initRestStopMap() {
    const mapElement = document.getElementById('restStopMap');
    if (!mapElement) {
        return;
    }

    const initializationId = ++mapInitializationId;
    removeRouteMapClickListener();
    clearRouteMapDraftMarker();
    routePointSelection = createRoutePointSelection();
    detailRequest?.invalidate();
    detailPanelEventController?.abort();
    detailPanelEventController = new globalThis.AbortController();
    detailRequest = createRestStopDetailRequest({ onState: renderDetailState });
    routeRequest = createRouteRestStopRequest({ onState: renderRouteState });
    nationalOilPriceRequest = createNationalOilPriceRequest({ onState: renderNationalOilPriceState });
    placeSearchRequest = createPlaceSearchRequest({ onState: renderPlaceSearchState });
    restStopNameSearchRequest = createRestStopNameSearchRequest({ onState: renderRestStopNameSearchState });
    restStopCompareSearchRequestA = createRestStopNameSearchRequest({
        onState: (state) => renderRestStopCompareCandidates('A', state)
    });
    restStopCompareSearchRequestB = createRestStopNameSearchRequest({
        onState: (state) => renderRestStopCompareCandidates('B', state)
    });
    restStopCompareRequest = createRestStopCompareRequest({ onState: renderRestStopCompareState });
    bindDetailPanelEvents();
    bindDetailSheetPresentation();

    try {
        const mapConfig = await fetchMapConfig();
        if (initializationId !== mapInitializationId) {
            return;
        }

        if (!mapConfig.naverMapsNcpKeyId) {
            showMapError('네이버 지도 API 키 설정이 필요합니다. NAVER_MAPS_NCP_KEY_ID 값을 확인해주세요.', '지도 설정 필요');
            return;
        }

        setText('restStopMapStatus', '위치 확인 중');
        await loadNaverMapsScript(mapConfig.naverMapsNcpKeyId);
        if (initializationId !== mapInitializationId) {
            return;
        }

        naverMaps = window.naver?.maps;
        if (!naverMaps) {
            showMapError('네이버 지도 스크립트를 불러오지 못했습니다. 지도 API 키와 서비스 URL 등록을 확인해주세요.', '지도 로딩 실패');
            return;
        }

        const initialCenter = await resolveInitialCenter();
        if (initializationId !== mapInitializationId) {
            return;
        }

        map = createMap(mapElement, naverMaps, initialCenter);
        bindLocateControl();
        bindRouteSearch();
        bindRestStopNameSearch();
        bindRestStopCompare();
        bindRouteMapClick();
        bindMarkerModeToggle();
        initializeMobileCurrentLocationOrigin();

        setText('restStopMapStatus', '휴게소 불러오는 중');
        const restStopResult = await fetchRestStops();
        if (initializationId !== mapInitializationId) {
            return;
        }

        if (restStopResult.status === 'external-unavailable') {
            showApiUnavailableAlert();
            renderRestStops(restStopResult.restStops);
            return;
        }

        renderRestStops(restStopResult.restStops);
    } catch (error) {
        if (initializationId !== mapInitializationId) {
            return;
        }

        console.error(error);
        showMapError('휴게소 위치를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
    }
}

async function fetchMapConfig() {
    const response = await fetch(MAP_CONFIG_ENDPOINT);
    const body = await response.json();

    if (!response.ok || body.code !== 'SUCCESS') {
        throw new Error(`Map config API failed: ${body.code}`);
    }

    return body.data ?? {};
}

function loadNaverMapsScript(ncpKeyId) {
    if (window.naver?.maps) {
        return Promise.resolve();
    }

    document.getElementById(NAVER_MAPS_SCRIPT_ID)?.remove();

    const script = document.createElement('script');
    script.id = NAVER_MAPS_SCRIPT_ID;
    script.src = `${NAVER_MAPS_SCRIPT_URL}?ncpKeyId=${encodeURIComponent(ncpKeyId)}`;
    script.async = true;
    document.head.appendChild(script);

    return waitForScriptLoad(script);
}

function waitForScriptLoad(script) {
    return new Promise((resolve, reject) => {
        script.addEventListener('load', resolve, { once: true });
        script.addEventListener('error', reject, { once: true });
    });
}

function resolveInitialCenter() {
    return new Promise((resolve) => {
        let resolved = false;
        const fallbackTimer = setTimeout(() => resolveOnce(SEOUL_CENTER), 4000);
        const resolveOnce = (center) => {
            if (resolved) {
                return;
            }

            resolved = true;
            clearTimeout(fallbackTimer);
            resolve(center);
        };

        if (!navigator.geolocation) {
            resolveOnce(SEOUL_CENTER);
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (position) => {
                currentLocation = {
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude
                };
                resolveOnce(currentLocation);
            },
            () => resolveOnce(SEOUL_CENTER),
            {
                enableHighAccuracy: false,
                maximumAge: 300000,
                timeout: 3000
            }
        );
    });
}

function createMap(mapElement, mapsApi, initialCenter) {
    return new mapsApi.Map(mapElement, {
        center: new mapsApi.LatLng(initialCenter.latitude, initialCenter.longitude),
        mapDataControl: false,
        scaleControl: true,
        zoom: DEFAULT_ZOOM,
        zoomControl: true,
        zoomControlOptions: {
            position: mapsApi.Position.TOP_LEFT
        }
    });
}

async function fetchRestStops() {
    const response = await fetch(REST_STOPS_ENDPOINT);
    const body = await response.json();

    if (body.code === 'EXTERNAL_API_UNAVAILABLE') {
        return { status: 'external-unavailable', restStops: [] };
    }

    if (!response.ok || body.code !== 'SUCCESS') {
        throw new Error(`Rest stop API failed: ${body.code}`);
    }

    return {
        status: 'success',
        restStops: Array.isArray(body.data) ? body.data : []
    };
}

function renderRestStops(restStops) {
    let markerCount = 0;
    allRestStopMarkers = [];

    restStops.forEach((restStop) => {
        const latitude = Number.parseFloat(restStop.yValue);
        const longitude = Number.parseFloat(restStop.xValue);

        if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
            return;
        }

        const position = new naverMaps.LatLng(latitude, longitude);
        const marker = new naverMaps.Marker({
            map,
            position,
            icon: {
                content: '<div class="route-rest-stop-marker"></div>',
                anchor: new naverMaps.Point(7, 7)
            }
        });
        const infoWindow = new naverMaps.InfoWindow({
            content: createPopupContent(restStop)
        });

        naverMaps.Event.addListener(marker, 'click', () => {
            if (routePointSelection.getMapTarget()) {
                return;
            }
            if (selectedInfoWindow) {
                selectedInfoWindow.close();
            }

            infoWindow.open(map, marker);
            selectedInfoWindow = infoWindow;
            openDetailPanel(restStop);
            map.panTo(position);
        });

        allRestStopMarkers.push(marker);
        markerCount += 1;
    });

    setText('restStopMapStatus', `${markerCount.toLocaleString()}개 표시`);
    applyMarkerMode();
}

function applyMarkerMode() {
    const target = markerMode === 'all' ? map : null;
    allRestStopMarkers.forEach((marker) => marker.setMap(target));
    updateMarkerModeButton();
}

function setMarkerMode(mode) {
    markerMode = mode;
    applyMarkerMode();
}

function toggleMarkerMode() {
    setMarkerMode(markerMode === 'all' ? 'route' : 'all');
}

function updateMarkerModeButton() {
    const button = document.getElementById('markerModeToggle');
    if (!button) {
        return;
    }
    button.textContent = markerMode === 'all' ? '전체 휴게소 보기' : '경로상 휴게소 보기';
}

function bindMarkerModeToggle() {
    if (!detailPanelEventController) {
        return;
    }

    document.getElementById('markerModeToggle')?.addEventListener('click', toggleMarkerMode, {
        signal: detailPanelEventController.signal
    });
    updateMarkerModeButton();
}

export function createPopupContent(restStop, options = {}) {
    const routeName = formatText(restStop.routeName, '노선 정보 없음');

    return `
        <div class="rest-stop-map-popup-card">
            <div class="rest-stop-map-popup-kicker">선택한 휴게소</div>
            <strong class="rest-stop-map-popup-name">${escapeHtml(restStop.unitName)}</strong>
            <div class="rest-stop-map-popup-route">${escapeHtml(routeName)}</div>
            ${popupDataSection(options)}
        </div>
    `;
}

function popupDataSection({ status = 'loading', tags } = {}) {
    if (status === 'success') {
        if (Array.isArray(tags) && tags.length > 0) {
            const pills = tags
                .map((tag) => `<span class="rest-stop-map-popup-tag">${escapeHtml(tag.label)}</span>`)
                .join('');
            return `<div class="rest-stop-map-popup-tags">${pills}</div>`;
        }
        return '<div class="rest-stop-map-popup-hint">등록된 정보 없음</div>';
    }

    if (status === 'loading') {
        return '<div class="rest-stop-map-popup-hint">정보를 불러오는 중…</div>';
    }

    return '<div class="rest-stop-map-popup-hint">정보를 불러오지 못했어요</div>';
}

function updateSelectedPopup(restStop, options) {
    if (!selectedInfoWindow) {
        return;
    }

    selectedInfoWindow.setContent(createPopupContent(restStop, options));
}

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

export function shouldShowRouteResultBackButton(openedFromRouteResult, isMobileSheet) {
    return openedFromRouteResult === true && isMobileSheet === true;
}

function bindDetailPanelEvents() {
    const closeButton = document.getElementById('restStopDetailClose');
    if (!closeButton || !detailPanelEventController) {
        return;
    }

    closeButton.addEventListener('click', () => {
        closeDetailPanel({ restoreMapFocus: true });
    }, { signal: detailPanelEventController.signal });
    document.getElementById('restStopDetailRouteBack')?.addEventListener('click', returnToRouteResultModal, {
        signal: detailPanelEventController.signal
    });
    document.getElementById('restStopOilRefreshButton')?.addEventListener('click', refreshOilInfo, {
        signal: detailPanelEventController.signal
    });
    document.getElementById('restStopFoodToggle')?.addEventListener('click', toggleFoodMenu, {
        signal: detailPanelEventController.signal
    });
    document.getElementById('restStopFoodOpen')?.addEventListener('click', openFoodModal, {
        signal: detailPanelEventController.signal
    });
    document.getElementById('restStopFoodModalClose')?.addEventListener('click', closeFoodModal, {
        signal: detailPanelEventController.signal
    });
    document.getElementById('restStopFoodModal')?.addEventListener('click', (event) => {
        if (event.target === event.currentTarget) {
            closeFoodModal();
        }
    }, { signal: detailPanelEventController.signal });
    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') {
            return;
        }
        if (routePointSelection.getMapTarget()) {
            event.preventDefault();
            cancelRouteMapSelection();
            return;
        }
        if (document.getElementById('restStopFoodModal')?.open) {
            return;
        }
        if (document.getElementById('routeResultModal')?.open) {
            return;
        }
        if (document.getElementById('placeCandidateModal')?.open) {
            return;
        }
        if (document.getElementById('routeOriginModal')?.open) {
            return;
        }
        const panel = document.getElementById('restStopDetailPanel');
        if (panel && !panel.classList.contains('d-none')) {
            closeDetailPanel({ restoreMapFocus: true });
        }
    }, { signal: detailPanelEventController.signal });
}

function bindDetailSheetPresentation() {
    if (!detailPanelEventController) {
        return;
    }

    window.addEventListener('resize', updateDetailSheetPresentation, {
        signal: detailPanelEventController.signal
    });
}

function isMobileDetailSheet() {
    return window.matchMedia(MOBILE_DETAIL_SHEET_MEDIA).matches;
}

function updateDetailSheetPresentation() {
    const panel = document.getElementById('restStopDetailPanel');
    const isOpen = panel && !panel.classList.contains('d-none');
    document.body.classList.toggle('rest-stop-detail-sheet-open', Boolean(isOpen && isMobileDetailSheet()));
    updateRouteResultBackButton();
}

function openDetailPanel(restStop, { fromRouteResult = false } = {}) {
    const panel = document.getElementById('restStopDetailPanel');
    if (!panel || !detailRequest) {
        return;
    }

    detailOpenedFromRouteResult = fromRouteResult;
    selectedRestStopName = restStop.unitName;
    selectedServiceAreaCode = restStop.serviceAreaCode;
    currentDetail = undefined;
    panel.classList.remove('d-none');
    updateDetailSheetPresentation();
    detailRequest.load(restStop.serviceAreaCode);
}

function closeDetailPanel({ restoreMapFocus = false } = {}) {
    detailRequest?.invalidate();
    closeFoodModal();

    const panel = document.getElementById('restStopDetailPanel');
    if (panel) {
        panel.classList.add('d-none');
        panel.setAttribute('aria-busy', 'false');
    }
    detailOpenedFromRouteResult = false;
    updateDetailSheetPresentation();

    if (selectedInfoWindow) {
        selectedInfoWindow.close();
        selectedInfoWindow = undefined;
    }

    if (restoreMapFocus) {
        document.getElementById('restStopMap')?.focus();
    }
}

function updateRouteResultBackButton() {
    const button = document.getElementById('restStopDetailRouteBack');
    if (!button) {
        return;
    }

    button.classList.toggle(
        'd-none',
        !shouldShowRouteResultBackButton(detailOpenedFromRouteResult, isMobileDetailSheet())
    );
}

function returnToRouteResultModal() {
    closeDetailPanel();
    openRouteResultModal();
}

function renderDetailState(state) {
    const panel = document.getElementById('restStopDetailPanel');
    const status = document.getElementById('restStopDetailStatus');
    const content = document.getElementById('restStopDetailContent');
    if (!panel || !status || !content) {
        return;
    }

    panel.classList.remove('d-none');
    updateDetailSheetPresentation();
    setDetailName(selectedRestStopName);
    panel.setAttribute('aria-busy', state.status === 'loading' ? 'true' : 'false');

    if (state.status === 'success') {
        if (state.externalUnavailable) {
            showApiUnavailableAlert();
        }

        status.textContent = '';
        status.classList.add('d-none');
        content.classList.remove('d-none');
        renderDetail(state.data);
        updateSelectedPopup(
            { unitName: state.data.unitName || selectedRestStopName, routeName: state.data.routeName },
            { status: 'success', tags: availableDataTags(state.data) }
        );
        return;
    }

    if (state.status === 'external-unavailable') {
        showApiUnavailableAlert();
    }

    content.classList.add('d-none');
    status.classList.remove('d-none');
    status.textContent = detailStatusMessage(state.status);
    updateSelectedPopup({ unitName: selectedRestStopName }, popupOptionsForState(state.status));
}

function popupOptionsForState(status) {
    if (status === 'loading') {
        return { status: 'loading' };
    }
    if (status === 'not-found') {
        return { status: 'success', tags: [] };
    }
    return { status: 'error' };
}

function detailStatusMessage(status) {
    if (status === 'loading') {
        return '상세 정보를 불러오는 중입니다.';
    }
    if (status === 'not-found') {
        return '상세 정보가 없습니다.';
    }
    return '상세 정보를 불러오지 못했습니다.';
}

function renderDetail(detail) {
    currentDetail = detail;
    renderDetailImage(document, detail);
    setDetailName(detail.unitName, selectedRestStopName);
    setDetailValue('restStopDetailRoute', detail.routeName, '노선 정보 없음');
    setDetailValue('restStopDetailDirection', detail.direction, '방향 정보 없음');
    setDetailValue('restStopDetailAddress', detail.address, '주소 정보 없음');
    renderConvenience(detail.convenience);
    setDetailValue('restStopDetailMaintenance', detail.maintenanceYn, '알 수 없음', formatAvailability);
    setDetailValue('restStopDetailFreight', detail.truckSaYn, '알 수 없음', formatFreightOperation);
    renderParkingInfo(detail.compactCarParkingCount, detail.fullSizeCarParkingCount, detail.disabledParkingCount);
    renderEvChargerInfo(detail.evChargerCount);
    renderSalesRanking(detail.salesRanking);
    renderOilInfo(detail.oilInfo);
    renderFoodMenu(detail.foodMenu);
}

function renderSalesRanking(salesRanking) {
    const section = document.getElementById('restStopSalesRankingSection');
    const month = document.getElementById('restStopSalesRankingMonth');
    const storeColumn = document.getElementById('restStopStoreRankingColumn');
    const productColumn = document.getElementById('restStopProductRankingColumn');
    const storeList = document.getElementById('restStopStoreRankingList');
    const productList = document.getElementById('restStopProductRankingList');
    if (!section || !month || !storeColumn || !productColumn || !storeList || !productList) {
        return;
    }

    const visibleStores = sortSalesRankingStores(salesRanking?.storeRankings);
    const visibleProducts = sortSalesRankingProducts(salesRanking?.products);
    const hasRanking = (visibleStores.length > 0 || visibleProducts.length > 0)
        && !isMissingValue(salesRanking?.baseYearMonth);
    section.classList.toggle('d-none', !hasRanking);
    if (!hasRanking) {
        storeList.replaceChildren();
        productList.replaceChildren();
        storeColumn.classList.add('d-none');
        productColumn.classList.add('d-none');
        month.textContent = '';
        return;
    }

    month.textContent = formatSalesRankingMonth(salesRanking.baseYearMonth);
    storeColumn.classList.toggle('d-none', visibleStores.length === 0);
    productColumn.classList.toggle('d-none', visibleProducts.length === 0);
    storeList.replaceChildren(...visibleStores.map((store) => createSalesRankingItem(store, 'storeName')));
    productList.replaceChildren(...visibleProducts.map((product) => createSalesRankingItem(product, 'productName')));
}

function createSalesRankingItem(ranking, nameKey) {
    const item = document.createElement('li');
    item.className = 'rest-stop-sales-ranking-item';

    const rank = document.createElement('span');
    rank.className = 'rest-stop-sales-ranking-rank';
    rank.textContent = `${ranking.rank}`;
    item.appendChild(rank);

    const name = document.createElement('span');
    name.className = 'rest-stop-sales-ranking-name';
    name.textContent = nameKey === 'storeName'
        ? normalizeSalesRankingStoreName(ranking[nameKey])
        : ranking[nameKey];
    item.appendChild(name);

    return item;
}

function setDetailName(value, fallbackValue) {
    const element = document.getElementById('restStopDetailName');
    if (!element) {
        return;
    }

    element.textContent = formatText(value, formatText(fallbackValue, '이름 정보 없음'));
    element.classList.toggle(
        'rest-stop-detail-missing',
        isMissingValue(value) && isMissingValue(fallbackValue)
    );
}

function setDetailValue(id, rawValue, fallback, formatter = (value) => formatText(value, fallback)) {
    const element = document.getElementById(id);
    if (!element) {
        return;
    }

    const formattedValue = formatter(rawValue);
    element.textContent = formattedValue;
    element.classList.toggle('rest-stop-detail-missing', isMissingValue(rawValue));
}

function renderParkingInfo(compactCount, fullSizeCount, disabledCount) {
    const totalElement = document.getElementById('restStopDetailTotalParking');
    if (totalElement) {
        const allMissing = isMissingValue(compactCount) && isMissingValue(fullSizeCount) && isMissingValue(disabledCount);
        totalElement.textContent = formatTotalParkingCount(compactCount, fullSizeCount, disabledCount);
        totalElement.classList.toggle('rest-stop-detail-missing', allMissing);
    }

    const breakdownElement = document.getElementById('restStopDetailParkingBreakdown');
    if (breakdownElement) {
        const allMissing = isMissingValue(fullSizeCount) && isMissingValue(compactCount) && isMissingValue(disabledCount);
        breakdownElement.textContent = formatParkingBreakdown(fullSizeCount, compactCount, disabledCount);
        breakdownElement.classList.toggle('rest-stop-detail-missing', allMissing);
    }
}

function renderConvenience(value) {
    const list = document.getElementById('restStopDetailConvenience');
    const fallback = document.getElementById('restStopDetailConvenienceFallback');
    if (!list || !fallback) {
        return;
    }

    list.replaceChildren();
    const conveniences = parseConvenience(value);
    conveniences.forEach((convenience) => {
        const item = document.createElement('li');
        item.textContent = convenience;
        list.appendChild(item);
    });

    list.classList.toggle('d-none', conveniences.length === 0);
    fallback.classList.toggle('d-none', conveniences.length > 0);
    fallback.textContent = conveniences.length === 0 ? CONVENIENCE_FALLBACK : '';
}

function bindLocateControl() {
    if (!detailPanelEventController) {
        return;
    }

    document.getElementById('restStopLocateButton')?.addEventListener('click', locateCurrentPosition, {
        signal: detailPanelEventController.signal
    });
}

function locateCurrentPosition() {
    if (!map || !naverMaps) {
        return;
    }

    if (!navigator.geolocation) {
        showLocateError('이 브라우저는 위치 기능을 지원하지 않습니다.');
        return;
    }

    navigator.geolocation.getCurrentPosition(
        (position) => {
            currentLocation = {
                latitude: position.coords.latitude,
                longitude: position.coords.longitude
            };
            const latLng = new naverMaps.LatLng(currentLocation.latitude, currentLocation.longitude);
            map.setCenter(latLng);
            showCurrentLocationMarker(latLng);
            hideLocateError();
        },
        (error) => showLocateError(locateErrorMessage(error)),
        GEOLOCATION_OPTIONS
    );
}

function showCurrentLocationMarker(latLng) {
    if (currentLocationMarker) {
        currentLocationMarker.setPosition(latLng);
        return;
    }

    currentLocationMarker = new naverMaps.Marker({
        map,
        position: latLng,
        icon: {
            content: '<div class="current-location-marker"></div>',
            anchor: new naverMaps.Point(8, 8)
        },
        zIndex: 1000
    });
}

function locateErrorMessage(error) {
    if (error?.code === 1) {
        return '위치 권한이 거부되어 현재 위치를 표시할 수 없습니다.';
    }

    return '현재 위치를 가져오지 못했습니다. 잠시 후 다시 시도해주세요.';
}

function showLocateError(message) {
    const errorElement = document.getElementById('restStopMapError');
    if (!errorElement) {
        return;
    }

    errorElement.textContent = message;
    errorElement.classList.remove('d-none');
}

function hideLocateError() {
    document.getElementById('restStopMapError')?.classList.add('d-none');
}

function renderFoodMenu(foodMenu) {
    const openButton = document.getElementById('restStopFoodOpen');
    if (!openButton) {
        return;
    }

    const visible = hasFoodMenu(foodMenu);
    openButton.classList.toggle('d-none', !visible);
    if (!visible) {
        currentFoodMenus = [];
        currentFoodSections = [];
        closeFoodModal();
        return;
    }

    currentFoodMenus = foodMenu.menus;
    currentFoodSections = Array.isArray(foodMenu.sections) ? foodMenu.sections : [];
    foodExpanded = false;
}

function openFoodModal() {
    const modal = document.getElementById('restStopFoodModal');
    if (!modal || currentFoodMenus.length === 0) {
        return;
    }

    foodExpanded = false;
    renderFoodList();
    modal.showModal();
}

function closeFoodModal() {
    const modal = document.getElementById('restStopFoodModal');
    if (modal?.open) {
        modal.close();
    }
}

function renderFoodList() {
    const list = document.getElementById('restStopFoodList');
    if (!list) {
        return;
    }

    const sections = currentFoodSections.filter((section) => Array.isArray(section?.menus) && section.menus.length > 0);
    if (!foodExpanded && sections.length > 0) {
        list.replaceChildren();
        sections.forEach((section) => list.appendChild(createFoodSection(section)));
        renderFoodToggle(currentFoodMenus.length > 0);
        return;
    }

    const representatives = currentFoodMenus.filter((menu) => menu?.representative);
    const hasRepresentatives = representatives.length > 0;
    const menus = foodExpanded || !hasRepresentatives ? orderFoodMenus(currentFoodMenus) : representatives;

    list.replaceChildren();
    menus.forEach((menu) => list.appendChild(createFoodMenuItem(menu)));
    renderFoodToggle(hasFoodSections({ sections }) || (hasRepresentatives && currentFoodMenus.length > representatives.length));
}

function renderFoodToggle(canExpand) {
    const toggle = document.getElementById('restStopFoodToggle');
    if (!toggle) {
        return;
    }

    toggle.classList.toggle('d-none', !canExpand);
    toggle.setAttribute('aria-expanded', foodExpanded ? 'true' : 'false');
    toggle.textContent = foodExpanded ? '추천 메뉴 보기' : '전체 메뉴 보기';
}

function createFoodSection(section) {
    const item = document.createElement('li');
    item.className = 'rest-stop-food-section';

    const title = document.createElement('h4');
    title.className = 'rest-stop-food-section-title';
    title.textContent = formatText(section?.title, '추천 메뉴');
    item.appendChild(title);

    const list = document.createElement('ul');
    list.className = 'rest-stop-food-section-list';
    section.menus.forEach((menu) => list.appendChild(createFoodMenuItem(menu)));
    item.appendChild(list);

    return item;
}

function createFoodMenuItem(menu) {
    const item = document.createElement('li');
    item.className = 'rest-stop-food-item';

    const name = document.createElement('p');
    name.className = 'rest-stop-food-name';
    name.textContent = formatText(menu?.foodName, '이름 정보 없음');
    formatFoodBadges(menu).forEach((badgeLabel) => name.appendChild(createFoodBadge(badgeLabel)));
    item.appendChild(name);

    const cost = document.createElement('p');
    cost.className = 'rest-stop-food-cost';
    cost.textContent = formatFoodCost(menu?.foodCost);
    item.appendChild(cost);

    if (!isMissingValue(menu?.description)) {
        const description = document.createElement('p');
        description.className = 'rest-stop-food-description';
        description.textContent = menu.description;
        item.appendChild(description);
    }

    return item;
}

function createFoodBadge(label) {
    const badge = document.createElement('span');
    badge.className = 'rest-stop-food-badge';
    badge.textContent = label;
    return badge;
}

function toggleFoodMenu() {
    foodExpanded = !foodExpanded;
    renderFoodList();
}

async function refreshOilInfo() {
    const button = document.getElementById('restStopOilRefreshButton');
    const status = document.getElementById('restStopOilRefreshStatus');
    if (!detailRequest || !button || !status) {
        return;
    }

    button.disabled = true;
    status.textContent = '실시간 요금을 확인하는 중입니다.';

    const result = await detailRequest.refreshOilPrice(selectedServiceAreaCode);

    button.disabled = false;
    if (result.status === 'success') {
        currentDetail = {
            ...currentDetail,
            oilInfo: result.data
        };
        renderOilInfo(result.data);
        return;
    }

    if (result.status === 'external-unavailable') {
        showApiUnavailableAlert();
    }

    status.textContent = oilRefreshStatusMessage(result.status);
    status.classList.add('rest-stop-detail-missing');
}

function oilRefreshStatusMessage(status) {
    if (status === 'not-found') {
        return '갱신할 주유소 정보가 없습니다.';
    }

    return '요금 정보를 갱신하지 못했습니다.';
}

export function renderOilInfo(oilInfo = {}) {
    const section = document.getElementById('restStopOilSection');
    if (!section) {
        return;
    }

    section.classList.remove('d-none');

    setDetailValue('restStopOilGasolinePrice', oilInfo?.gasolinePrice, '정보 없음', formatOilPrice);
    setDetailValue('restStopOilDieselPrice', oilInfo?.dieselPrice, '정보 없음', formatOilPrice);
    setDetailValue('restStopOilLpgPrice', oilInfo?.lpgPrice, '정보 없음', formatOilPrice);
    setDetailValue('restStopOilCompany', oilInfo?.oilCompany, '정보 없음');
    setDetailValue('restStopOilTelNo', oilInfo?.telNo, '정보 없음');
    renderOilRefreshStatus(oilInfo?.lastRefreshedAt);
    renderOilConveniences(oilInfo?.oilStationConveniences);
}

function renderOilRefreshStatus(lastRefreshedAt) {
    const status = document.getElementById('restStopOilRefreshStatus');
    if (!status) {
        return;
    }

    status.textContent = formatRefreshedAt(lastRefreshedAt);
    status.classList.toggle('rest-stop-detail-missing', isMissingValue(lastRefreshedAt));
}

function renderOilConveniences(conveniences) {
    const tags = document.getElementById('restStopOilConvenienceTags');
    const fallback = document.getElementById('restStopOilConvenienceFallback');
    const details = document.getElementById('restStopOilConvenienceDetails');
    if (!tags || !fallback || !details) {
        return;
    }

    tags.replaceChildren();
    details.replaceChildren();

    const oilConveniences = Array.isArray(conveniences) ? conveniences : [];
    oilConveniences.forEach((convenience) => {
        tags.appendChild(createOilConvenienceTag(convenience));
        details.appendChild(createOilConvenienceDetail(convenience));
    });

    const hasConveniences = oilConveniences.length > 0;
    tags.classList.toggle('d-none', !hasConveniences);
    details.classList.toggle('d-none', !hasConveniences);
    fallback.classList.toggle('d-none', hasConveniences);
    fallback.textContent = hasConveniences ? '' : '주유소 편의시설 정보 없음';
}

function createOilConvenienceTag(convenience) {
    const item = document.createElement('li');
    item.textContent = formatText(convenience?.name, '이름 정보 없음');
    return item;
}

function createOilConvenienceDetail(convenience) {
    const item = document.createElement('li');

    const name = document.createElement('p');
    name.className = 'rest-stop-oil-convenience-name';
    name.textContent = formatText(convenience?.name, '이름 정보 없음');
    item.appendChild(name);

    const description = document.createElement('p');
    description.className = 'rest-stop-oil-convenience-description';
    description.textContent = formatText(convenience?.description, '상세 정보 없음');
    description.classList.toggle('rest-stop-detail-missing', isMissingValue(convenience?.description));
    item.appendChild(description);

    const time = document.createElement('p');
    time.className = 'rest-stop-oil-convenience-time';
    time.textContent = formatOperationTime(convenience?.startTime, convenience?.endTime);
    time.classList.toggle(
        'rest-stop-detail-missing',
        isMissingValue(convenience?.startTime) || isMissingValue(convenience?.endTime)
    );
    item.appendChild(time);

    return item;
}

function bindRouteSearch() {
    if (!detailPanelEventController) {
        return;
    }

    const signal = detailPanelEventController.signal;
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
    document.getElementById('routeResultOpen')?.addEventListener('click', openRouteResultModal, {
        signal
    });
    document.getElementById('routeResultModalClose')?.addEventListener('click', closeRouteResultModal, {
        signal
    });
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

function bindRestStopNameSearch() {
    if (!detailPanelEventController) {
        return;
    }

    const signal = detailPanelEventController.signal;
    document.getElementById('restStopNameSearchButton')?.addEventListener('click', searchRestStopByName, { signal });
    document.getElementById('restStopNameSearchInput')?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            searchRestStopByName();
        }
    }, { signal });
    document.getElementById('restStopSearchModalClose')?.addEventListener('click', closeRestStopSearchModal, {
        signal
    });
    document.getElementById('restStopSearchModal')?.addEventListener('click', (event) => {
        if (event.target === event.currentTarget) {
            closeRestStopSearchModal();
        }
    }, { signal });
}

function setRestStopNameSearchStatus(message) {
    const status = document.getElementById('restStopNameSearchStatus');
    if (!status) {
        return;
    }
    status.hidden = message === '';
    status.textContent = message;
}

function searchRestStopByName() {
    const query = document.getElementById('restStopNameSearchInput')?.value.trim() ?? '';
    if (query === '') {
        setRestStopNameSearchStatus('휴게소명을 입력해주세요.');
        return;
    }
    restStopNameSearchRequest?.load(query);
}

function renderRestStopNameSearchState(state) {
    if (state.status === 'loading') {
        setRestStopNameSearchStatus('검색하는 중입니다...');
        return;
    }

    if (state.status === 'success') {
        if (state.restStops.length === 0) {
            setRestStopNameSearchStatus('검색 결과가 없습니다.');
            return;
        }
        if (state.restStops.length === 1) {
            setRestStopNameSearchStatus('');
            selectRestStopSearchResult(state.restStops[0]);
            return;
        }
        setRestStopNameSearchStatus('');
        renderRestStopSearchCandidates(state.restStops);
        openRestStopSearchModal();
        return;
    }

    setRestStopNameSearchStatus('검색에 실패했습니다. 잠시 후 다시 시도해주세요.');
}

function renderRestStopSearchCandidates(restStops) {
    const list = document.getElementById('restStopSearchList');
    if (!list) {
        return;
    }
    list.replaceChildren();
    restStops.forEach((restStop) => list.appendChild(createRestStopSearchCandidateItem(restStop)));
}

function createRestStopSearchCandidateItem(restStop) {
    const item = document.createElement('li');
    item.className = 'route-result-item route-candidate-item';

    const button = document.createElement('button');
    button.className = 'route-candidate-button';
    button.type = 'button';

    const name = document.createElement('p');
    name.className = 'route-result-name';
    name.textContent = formatText(restStop?.unitName, '이름 정보 없음');
    button.appendChild(name);

    const meta = document.createElement('p');
    meta.className = 'route-result-meta';
    meta.textContent = formatText(restStop?.routeName, '노선 정보 없음');
    button.appendChild(meta);

    button.addEventListener('click', () => selectRestStopSearchResult(restStop));
    item.appendChild(button);

    return item;
}

function selectRestStopSearchResult(restStop) {
    closeRestStopSearchModal();

    const latitude = Number.parseFloat(restStop.yValue);
    const longitude = Number.parseFloat(restStop.xValue);
    if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
        openRestStopPopupAt(restStop, new naverMaps.LatLng(latitude, longitude));
        return;
    }
    openDetailPanel(restStop);
}

function openRestStopSearchModal() {
    const modal = document.getElementById('restStopSearchModal');
    if (modal && !modal.open) {
        modal.showModal();
    }
}

function closeRestStopSearchModal() {
    const modal = document.getElementById('restStopSearchModal');
    if (modal?.open) {
        modal.close();
    }
}

function bindRestStopCompare() {
    if (!detailPanelEventController) {
        return;
    }

    const signal = detailPanelEventController.signal;
    document.getElementById('restStopCompareOpen')?.addEventListener('click', openRestStopCompareModal, { signal });
    document.getElementById('restStopCompareModalClose')?.addEventListener('click', closeRestStopCompareModal, {
        signal
    });
    document.getElementById('restStopCompareModal')?.addEventListener('click', (event) => {
        if (event.target === event.currentTarget) {
            closeRestStopCompareModal();
        }
    }, { signal });
    ['A', 'B'].forEach((side) => {
        document.getElementById(`restStopCompareInput${side}`)?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                searchRestStopCompareSide(side);
            }
        }, { signal });
    });
}

function searchRestStopCompareSide(side) {
    const query = document.getElementById(`restStopCompareInput${side}`)?.value.trim() ?? '';
    if (query === '') {
        return;
    }
    const request = side === 'A' ? restStopCompareSearchRequestA : restStopCompareSearchRequestB;
    request?.load(query);
}

function renderRestStopCompareCandidates(side, state) {
    const list = document.getElementById(`restStopCompareCandidates${side}`);
    if (!list) {
        return;
    }
    list.replaceChildren();
    if (state.status !== 'success') {
        return;
    }
    state.restStops.forEach((restStop) => list.appendChild(createRestStopCompareCandidateItem(side, restStop)));
}

function createRestStopCompareCandidateItem(side, restStop) {
    const item = document.createElement('li');
    item.className = 'rest-stop-compare-picker-candidate';

    const button = document.createElement('button');
    button.className = 'rest-stop-compare-picker-candidate-button';
    button.type = 'button';
    button.textContent = `${formatText(restStop?.unitName, '이름 정보 없음')} (${formatText(restStop?.routeName, '노선 정보 없음')})`;
    button.addEventListener('click', () => selectRestStopCompareSide(side, restStop));

    item.appendChild(button);
    return item;
}

function selectRestStopCompareSide(side, restStop) {
    if (side === 'A') {
        restStopCompareSelectionA = restStop;
    } else {
        restStopCompareSelectionB = restStop;
    }

    const input = document.getElementById(`restStopCompareInput${side}`);
    if (input) {
        input.value = '';
    }
    document.getElementById(`restStopCompareCandidates${side}`)?.replaceChildren();

    const selected = document.getElementById(`restStopCompareSelected${side}`);
    if (selected) {
        selected.hidden = false;
        selected.textContent = `${formatText(restStop?.unitName, '이름 정보 없음')} · ${formatText(restStop?.routeName, '노선 정보 없음')}`;
    }

    runRestStopCompareIfReady();
}

function runRestStopCompareIfReady() {
    if (!restStopCompareSelectionA || !restStopCompareSelectionB) {
        return;
    }

    if (restStopCompareSelectionA.serviceAreaCode === restStopCompareSelectionB.serviceAreaCode) {
        setRestStopCompareStatus('같은 휴게소는 비교할 수 없습니다. 다른 휴게소를 선택해주세요.');
        return;
    }

    restStopCompareRequest?.load(
        restStopCompareSelectionA.serviceAreaCode,
        restStopCompareSelectionB.serviceAreaCode
    );
}

function setRestStopCompareStatus(message) {
    const status = document.getElementById('restStopCompareStatus');
    if (!status) {
        return;
    }
    status.hidden = message === '';
    status.textContent = message;
}

function renderRestStopCompareState(state) {
    if (state.status === 'loading') {
        setRestStopCompareStatus('비교하는 중입니다...');
        return;
    }

    if (state.status === 'success') {
        setRestStopCompareStatus('');
        renderRestStopCompareResult(state.comparison);
        return;
    }

    setRestStopCompareStatus(formatText(state.message, '비교 결과를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'));
}

export function buildRestStopCompareRows(response) {
    if (!response) {
        return [];
    }

    const sideA = response.sideA ?? {};
    const sideB = response.sideB ?? {};
    const result = response.result ?? {};

    return [
        compareTextRow('휘발유', formatOilPrice(sideA.gasolinePrice), formatOilPrice(sideB.gasolinePrice), result.gasolineWinner),
        compareTextRow('경유', formatOilPrice(sideA.dieselPrice), formatOilPrice(sideB.dieselPrice), result.dieselWinner),
        compareTextRow('LPG', formatOilPrice(sideA.lpgPrice), formatOilPrice(sideB.lpgPrice), result.lpgWinner),
        compareTextRow('주차', formatParkingCount(sideA.parkingCount), formatParkingCount(sideB.parkingCount), result.parkingWinner),
        compareChipsRow('부대시설', sideA.facilities, sideB.facilities, result.facilityWinner)
    ];
}

function compareTextRow(label, textA, textB, winner) {
    return {
        label,
        type: 'text',
        a: { text: textA, isWinner: winner === 'A' },
        b: { text: textB, isWinner: winner === 'B' }
    };
}

function compareChipsRow(label, chipsA, chipsB, winner) {
    return {
        label,
        type: 'chips',
        a: { chips: Array.isArray(chipsA) ? chipsA : [], isWinner: winner === 'A' },
        b: { chips: Array.isArray(chipsB) ? chipsB : [], isWinner: winner === 'B' }
    };
}

function renderRestStopCompareResult(response) {
    const container = document.getElementById('restStopCompareResult');
    const picker = document.getElementById('restStopComparePicker');
    if (!container || !response) {
        return;
    }

    container.replaceChildren();
    picker.hidden = true;
    container.hidden = false;

    const resetButton = document.createElement('button');
    resetButton.type = 'button';
    resetButton.className = 'btn btn-sm btn-outline-secondary rest-stop-compare-reset';
    resetButton.textContent = '다시 선택';
    resetButton.addEventListener('click', resetRestStopCompareModal);
    container.appendChild(resetButton);

    container.appendChild(createRestStopCompareHeader(response));
    container.appendChild(createRestStopCompareTable(response));
}

function createRestStopCompareHeader(response) {
    const recommendedSide = response.result?.recommendedSide ?? null;
    const header = document.createElement('div');
    header.className = 'rest-stop-compare-header';
    header.appendChild(createRestStopCompareHeaderSide(response.sideA, recommendedSide === 'A'));

    const mark = document.createElement('span');
    mark.className = 'rest-stop-compare-vs-mark';
    mark.textContent = 'VS';
    header.appendChild(mark);

    header.appendChild(createRestStopCompareHeaderSide(response.sideB, recommendedSide === 'B'));
    return header;
}

function createRestStopCompareHeaderSide(side, isRecommended) {
    const wrapper = document.createElement('div');
    wrapper.className = 'rest-stop-compare-header-side';

    const photo = document.createElement('div');
    photo.className = `rest-stop-compare-photo${isRecommended ? ' rest-stop-compare-photo-pick' : ''}`;
    if (side?.listImageUrl) {
        const image = document.createElement('img');
        image.src = side.listImageUrl;
        image.alt = '';
        photo.appendChild(image);
    } else {
        photo.textContent = '없음';
    }
    wrapper.appendChild(photo);

    const text = document.createElement('div');
    text.className = 'rest-stop-compare-header-text';
    if (isRecommended) {
        const badge = document.createElement('span');
        badge.className = 'rest-stop-compare-pick-badge';
        badge.textContent = '추천';
        text.appendChild(badge);
    }

    const name = document.createElement('p');
    name.className = `rest-stop-compare-name${isRecommended ? ' rest-stop-compare-name-pick' : ''}`;
    name.textContent = formatText(side?.unitName, '이름 정보 없음');
    text.appendChild(name);

    const route = document.createElement('p');
    route.className = 'rest-stop-compare-route';
    route.textContent = formatText(side?.routeName, '노선 정보 없음');
    text.appendChild(route);

    wrapper.appendChild(text);
    return wrapper;
}

function createRestStopCompareTable(response) {
    const table = document.createElement('div');
    table.className = 'rest-stop-compare-table';
    buildRestStopCompareRows(response).forEach((row) => table.appendChild(createRestStopCompareRowElement(row)));
    return table;
}

function createRestStopCompareRowElement(row) {
    const rowElement = document.createElement('div');
    rowElement.className = 'rest-stop-compare-row';
    rowElement.appendChild(createRestStopCompareValueElement(row, 'a'));

    const label = document.createElement('div');
    label.className = 'rest-stop-compare-label';
    label.textContent = row.label;
    rowElement.appendChild(label);

    rowElement.appendChild(createRestStopCompareValueElement(row, 'b'));
    return rowElement;
}

function createRestStopCompareValueElement(row, side) {
    const value = row[side];
    const valueElement = document.createElement('div');
    valueElement.className =
        `rest-stop-compare-value rest-stop-compare-value-${side}${value.isWinner ? ' rest-stop-compare-value-win' : ''}`;

    if (row.type === 'chips') {
        valueElement.classList.add('rest-stop-compare-value-chips');
        value.chips.forEach((chip) => valueElement.appendChild(createRestStopCompareChip(chip, value.isWinner)));
        return valueElement;
    }

    if (value.isWinner) {
        const check = document.createElement('span');
        check.className = 'rest-stop-compare-check';
        check.textContent = '✓';
        valueElement.appendChild(check);
    }
    valueElement.appendChild(document.createTextNode(value.text));
    return valueElement;
}

function createRestStopCompareChip(label, isWinner) {
    const chip = document.createElement('span');
    chip.className = `rest-stop-compare-chip${isWinner ? ' rest-stop-compare-chip-win' : ''}`;
    chip.textContent = label;
    return chip;
}

function resetRestStopCompareModal() {
    restStopCompareSelectionA = undefined;
    restStopCompareSelectionB = undefined;
    restStopCompareRequest?.invalidate();
    restStopCompareSearchRequestA?.invalidate();
    restStopCompareSearchRequestB?.invalidate();

    ['A', 'B'].forEach((side) => {
        const input = document.getElementById(`restStopCompareInput${side}`);
        if (input) {
            input.value = '';
        }
        document.getElementById(`restStopCompareCandidates${side}`)?.replaceChildren();
        const selected = document.getElementById(`restStopCompareSelected${side}`);
        if (selected) {
            selected.hidden = true;
            selected.textContent = '';
        }
    });
    setRestStopCompareStatus('');

    const picker = document.getElementById('restStopComparePicker');
    const result = document.getElementById('restStopCompareResult');
    if (picker) {
        picker.hidden = false;
    }
    if (result) {
        result.hidden = true;
        result.replaceChildren();
    }
}

function openRestStopCompareModal() {
    resetRestStopCompareModal();
    const modal = document.getElementById('restStopCompareModal');
    if (modal && !modal.open) {
        modal.showModal();
    }
}

function closeRestStopCompareModal() {
    const modal = document.getElementById('restStopCompareModal');
    if (modal?.open) {
        modal.close();
    }
}

function openRouteOriginModal() {
    const modal = document.getElementById('routeOriginModal');
    if (modal && !modal.open) {
        modal.showModal();
    }
}

function closeRouteOriginModal() {
    const modal = document.getElementById('routeOriginModal');
    if (modal?.open) {
        modal.close();
    }
}

function selectCurrentLocationAsOrigin() {
    if (!navigator.geolocation) {
        setRouteStatus('이 브라우저는 위치 기능을 지원하지 않습니다.');
        return;
    }

    setRouteStatus('현재 위치를 확인하는 중입니다...');
    navigator.geolocation.getCurrentPosition(
        (position) => {
            currentLocation = {
                latitude: position.coords.latitude,
                longitude: position.coords.longitude
            };
            hideLocateError();
            selectCurrentLocationOrigin({ moveMap: true });
            closeRouteOriginModal();
            setRouteStatus('현재 위치를 출발지로 설정했습니다.');
            requestRouteAutomatically();
        },
        (error) => setRouteStatus(locateErrorMessage(error)),
        GEOLOCATION_OPTIONS
    );
}

function initializeMobileCurrentLocationOrigin() {
    if (!isMobileDetailSheet() || !currentLocation || routePointSelection.getOrigin()) {
        return;
    }

    selectCurrentLocationOrigin({ moveMap: false });
    showCurrentLocationMarker(new naverMaps.LatLng(currentLocation.latitude, currentLocation.longitude));
    setRoutePointFieldsExpanded(true);
    setRouteStatus('현재 위치를 출발지로 설정했습니다. 도착지를 선택해주세요.');
}

function selectCurrentLocationOrigin({ moveMap = false } = {}) {
    const previousSignature = currentRouteSelectionSignature();
    const selectedOrigin = routePointSelection.select(ROUTE_POINT_TARGET.ORIGIN, {
        name: '현재 위치',
        ...currentLocation
    });
    renderEndpointMarker(ROUTE_POINT_TARGET.ORIGIN, selectedOrigin);
    if (moveMap) {
        moveMapToPoint(selectedOrigin);
    }
    renderSelectedOrigin();
    invalidateRouteResultIfSelectionChanged(previousSignature);
    return selectedOrigin;
}

function openRouteResultModal() {
    const modal = document.getElementById('routeResultModal');
    if (modal && currentRouteRestStops.length > 0 && !modal.open) {
        modal.showModal();
    }
}

function closeRouteResultModal() {
    const modal = document.getElementById('routeResultModal');
    if (modal?.open) {
        modal.close();
    }
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
    const origin = routePointSelection.getOrigin();
    const destination = routePointSelection.getDestination();
    if (!shouldRequestRouteAutomatically(origin, destination)) {
        return;
    }

    const signature = routeRequestSignature(origin, destination);
    if (signature === automaticRouteRequestSignature) {
        return;
    }

    automaticRouteRequestSignature = signature;
    requestSelectedRoute();
}

export function canRequestRouteAutomatically(origin, destination) {
    return Boolean(origin && destination)
        && Number.isFinite(origin.latitude)
        && Number.isFinite(origin.longitude)
        && Number.isFinite(destination.latitude)
        && Number.isFinite(destination.longitude);
}

export function shouldRequestRouteAutomatically(origin, destination) {
    return canRequestRouteAutomatically(origin, destination);
}

export function isRouteGlobalLoadingState(state) {
    return state?.status === 'loading';
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
    const origin = routePointSelection.getOrigin();
    const destination = routePointSelection.getDestination();
    if (!canRequestRouteAutomatically(origin, destination)) {
        return undefined;
    }
    return routeRequestSignature(origin, destination);
}

function invalidateRouteResultIfSelectionChanged(previousSignature) {
    if (previousSignature === currentRouteSelectionSignature()) {
        return;
    }

    automaticRouteRequestSignature = undefined;
    routeRequest?.invalidate();
    clearRouteOverlays();
    renderRouteList([], null);
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
    const item = document.createElement('li');
    item.className = 'route-result-item route-candidate-item';

    const button = document.createElement('button');
    button.className = 'route-candidate-button';
    button.type = 'button';

    const name = document.createElement('p');
    name.className = 'route-result-name';
    name.textContent = formatText(candidate?.name, '이름 정보 없음');
    button.appendChild(name);

    const meta = document.createElement('p');
    meta.className = 'route-result-meta';
    meta.textContent = formatText(candidate?.address, '주소 정보 없음');
    button.appendChild(meta);

    button.addEventListener('click', () => selectPlaceCandidate(candidate));
    item.appendChild(button);

    return item;
}

function selectPlaceCandidate(candidate) {
    const target = routePointSelection.getSearchTarget();
    const previousSignature = currentRouteSelectionSignature();
    const selected = routePointSelection.select(target, candidate);
    closePlaceCandidateModal();

    renderEndpointMarker(target, selected);
    moveMapToPoint(selected);

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
    const modal = document.getElementById('placeCandidateModal');
    if (modal && !modal.open) {
        modal.showModal();
    }
}

function closePlaceCandidateModal() {
    const modal = document.getElementById('placeCandidateModal');
    if (modal?.open) {
        modal.close();
    }
}

function bindRouteMapClick() {
    removeRouteMapClickListener();
    if (!map || !naverMaps) {
        return;
    }

    routeMapClickListener = naverMaps.Event.addListener(map, 'click', handleRouteMapClick);
}

function removeRouteMapClickListener() {
    if (routeMapClickListener && naverMaps?.Event) {
        naverMaps.Event.removeListener(routeMapClickListener);
    }
    routeMapClickListener = undefined;
}

function beginRouteMapSelection(target, trigger) {
    if (!map || !naverMaps) {
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
    const position = new naverMaps.LatLng(point.latitude, point.longitude);
    if (routeMapDraftMarker) {
        routeMapDraftMarker.setPosition(position);
        return;
    }

    routeMapDraftMarker = new naverMaps.Marker({
        map,
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
    renderRouteList([], null);
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

    const path = Array.isArray(data?.route?.path) ? data.route.path : [];
    const latLngs = path
        .filter((point) => Array.isArray(point) && point.length === 2)
        .map((point) => new naverMaps.LatLng(point[1], point[0]));
    if (latLngs.length > 0) {
        routePolyline = new naverMaps.Polyline({
            map,
            path: latLngs,
            strokeColor: '#0d6efd',
            strokeWeight: 5,
            strokeOpacity: 0.85
        });
        fitMapToPath(latLngs);
    }

    renderEndpointMarkers(data?.destination);

    const restStops = Array.isArray(data?.restStops) ? data.restStops : [];
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
            openRestStopPopupAt(restStop, position);
        });

        routeMarkers.push(marker);
    });

    currentRouteRestStops = restStops;
    renderRouteList(restStops, currentNationalOilPriceSummary);
    nationalOilPriceRequest?.load();
    const destinationName = data?.destination?.name ?? '목적지';
    setRouteStatus(`${destinationName}까지 경로상 휴게소 ${restStops.length}곳`);

    const button = document.getElementById('routeResultOpen');
    if (button) {
        button.textContent = `경로 결과 ${restStops.length}곳`;
    }
    toggleRouteResultButton(restStops.length > 0);
    openRouteResultModal();
    setMarkerMode('route');
}

function renderEndpointMarker(target, point) {
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

function moveMapToPoint(point) {
    if (!map || !naverMaps || !point) {
        return;
    }
    if (!Number.isFinite(point.latitude) || !Number.isFinite(point.longitude)) {
        return;
    }
    map.panTo(new naverMaps.LatLng(point.latitude, point.longitude));
}

function renderEndpointMarkers(destination) {
    renderEndpointMarker(ROUTE_POINT_TARGET.ORIGIN, routePointSelection.getOrigin());
    renderEndpointMarker(ROUTE_POINT_TARGET.DESTINATION, destination);
}

function fitMapToPath(latLngs) {
    const bounds = new naverMaps.LatLngBounds(latLngs[0], latLngs[0]);
    latLngs.forEach((latLng) => bounds.extend(latLng));
    map.fitBounds(bounds);
}

function renderRouteList(restStops, nationalOilPriceSummary = currentNationalOilPriceSummary) {
    const list = document.getElementById('routeResultList');
    if (!list) {
        return;
    }

    renderDirectionAlternativeNotice(restStops);
    renderNationalOilPriceSummary(nationalOilPriceSummary);
    renderRouteResultTitle(restStops.length);
    list.replaceChildren();
    restStops.forEach((restStop, index) => list.appendChild(createRouteResultItem(restStop, index)));
}

function renderRouteResultTitle(count) {
    const title = document.getElementById('routeResultModalTitle');
    if (!title) {
        return;
    }

    title.textContent = count > 0 ? `경로상 휴게소 (${count}곳)` : '경로상 휴게소';
}

function renderDirectionAlternativeNotice(restStops) {
    const notice = document.getElementById('routeDirectionAlternativeNotice');
    if (!notice) {
        return;
    }

    const hasDirectionAlternative = restStops.some((restStop) => restStop?.hasDirectionAlternative === true);
    notice.classList.toggle('d-none', !hasDirectionAlternative);
}

export function routeRecommendationLabels(restStop) {
    if (!Array.isArray(restStop?.recommendationTags)) {
        return [];
    }
    return restStop.recommendationTags
        .map((tag) => formatText(tag?.label, ''))
        .filter((label) => label !== '');
}

export function routeNearbyTrafficBadge(restStop) {
    const traffic = restStop?.nearbyTraffic;
    if (!traffic || typeof traffic !== 'object' || !traffic.key || !traffic.label) {
        return null;
    }
    return { key: traffic.key, label: traffic.label };
}

export function formatOilPriceComparison(price, diffFromAverage) {
    if (isMissingValue(price)) {
        return '';
    }

    const delta = formatOilPriceDelta(diffFromAverage);
    if (delta === null) {
        return price;
    }

    return `${price} ${delta.text}`;
}

export function formatOilPriceDelta(diffFromAverage) {
    if (!Number.isFinite(diffFromAverage)) {
        return null;
    }

    const absoluteDiff = Math.abs(diffFromAverage).toLocaleString('ko-KR');
    if (diffFromAverage < 0) {
        return { text: `(-${absoluteDiff})`, tone: 'cheap' };
    }
    if (diffFromAverage > 0) {
        return { text: `(+${absoluteDiff})`, tone: 'expensive' };
    }
    return { text: '(0)', tone: 'same' };
}

export function formatNationalOilPriceSummary(summary) {
    if (!summary || typeof summary !== 'object') {
        return null;
    }

    const items = [
        ['휘발유', summary.gasoline],
        ['경유', summary.diesel],
        ['LPG', summary.lpg]
    ]
        .map(([fallbackLabel, item]) => formatNationalOilPriceItem(fallbackLabel, item))
        .filter((item) => item !== null);
    if (items.length === 0) {
        return null;
    }

    return {
        tradeDate: formatText(summary.tradeDate, '오늘'),
        items
    };
}

function formatNationalOilPriceItem(fallbackLabel, item) {
    if (!item || typeof item !== 'object' || isMissingValue(item.price)) {
        return null;
    }

    return {
        label: fallbackLabel,
        price: item.price,
        ...formatNationalOilDailyDiff(item.dailyDiff)
    };
}

function formatNationalOilDailyDiff(value) {
    const diff = Number.parseFloat(value);
    if (!Number.isFinite(diff)) {
        return { dailyDiff: '-', dailyDiffTone: 'same' };
    }

    const absoluteDiff = Math.abs(diff).toLocaleString('ko-KR');
    if (diff < 0) {
        return { dailyDiff: `↓ ${absoluteDiff}원`, dailyDiffTone: 'favorable' };
    }
    if (diff > 0) {
        return { dailyDiff: `↑ ${absoluteDiff}원`, dailyDiffTone: 'unfavorable' };
    }
    return { dailyDiff: '0원', dailyDiffTone: 'same' };
}

export function renderNationalOilPriceState(state) {
    if (state.status === 'success') {
        currentNationalOilPriceSummary = state.data;
        renderNationalOilPriceSummary(state.data);
        return;
    }

    if (state.status === 'external-unavailable') {
        showApiUnavailableAlert();
    }

    currentNationalOilPriceSummary = null;
    renderNationalOilPriceSummary(null);
}

function renderNationalOilPriceSummary(summary) {
    const container = document.getElementById('routeNationalOilPriceSummary');
    if (!container) {
        return;
    }

    const formattedSummary = formatNationalOilPriceSummary(summary);
    container.replaceChildren();
    container.classList.toggle('d-none', formattedSummary === null);
    if (formattedSummary === null) {
        return;
    }

    const header = document.createElement('div');
    header.className = 'route-national-oil-summary-header';

    const title = document.createElement('p');
    title.className = 'route-national-oil-summary-title';
    title.textContent = '전국 평균 유가';
    header.appendChild(title);

    const date = document.createElement('p');
    date.className = 'route-national-oil-summary-date';
    date.textContent = formattedSummary.tradeDate;
    header.appendChild(date);
    container.appendChild(header);

    const list = document.createElement('dl');
    list.className = 'route-national-oil-summary-list';
    formattedSummary.items.forEach((item) => {
        const chip = document.createElement('div');
        chip.className = 'route-national-oil-chip';

        const term = document.createElement('dt');
        term.textContent = item.label;
        chip.appendChild(term);

        const description = document.createElement('dd');
        const price = document.createElement('span');
        price.className = 'route-national-oil-chip-price';
        price.textContent = item.price;
        description.appendChild(price);

        const diff = document.createElement('span');
        diff.className = `route-national-oil-chip-diff route-national-oil-chip-diff-${item.dailyDiffTone}`;
        diff.textContent = item.dailyDiff;
        description.appendChild(diff);

        chip.appendChild(description);
        list.appendChild(chip);
    });
    container.appendChild(list);
}

export function formatRouteComparisonSummary(restStop) {
    const summary = restStop?.comparisonSummary;
    if (!summary || typeof summary !== 'object') {
        return [];
    }

    const priceParts = [
        ['휘발유', summary.gasolinePrice, summary.gasolinePriceDiffFromAverage],
        ['경유', summary.dieselPrice, summary.dieselPriceDiffFromAverage],
        ['LPG', summary.lpgPrice, summary.lpgPriceDiffFromAverage]
    ]
        .map(([label, value, diff]) => [label, formatOilPriceComparison(value, diff)])
        .filter(([, value]) => value !== '')
        .map(([label, value]) => `${label} ${value}`);
    const countParts = [];
    if (Number.isFinite(summary.totalParkingCount) && summary.totalParkingCount > 0) {
        countParts.push(`주차 ${summary.totalParkingCount.toLocaleString()}대`);
    }
    if (Number.isFinite(summary.foodMenuCount) && summary.foodMenuCount > 0) {
        countParts.push(`먹거리 ${summary.foodMenuCount.toLocaleString()}개`);
    }
    if (Number.isFinite(summary.facilityCount) && summary.facilityCount > 0) {
        countParts.push(`시설 ${summary.facilityCount.toLocaleString()}개`);
    }

    return [priceParts, countParts]
        .filter((parts) => parts.length > 0)
        .map((parts) => parts.join(' · '));
}

export function formatEvChargerAvailability(restStop) {
    return restStop?.hasEvCharger === true ? '전기차 충전 가능' : '';
}

export function formatEvChargerCount(count) {
    const numericCount = Number(count);
    if (!Number.isFinite(numericCount) || numericCount <= 0) {
        return '';
    }

    return `${numericCount.toLocaleString()}대`;
}

function renderEvChargerInfo(count) {
    const badge = document.getElementById('restStopDetailEvCharger');
    const value = document.getElementById('restStopDetailEvChargerText');
    if (!badge || !value) {
        return;
    }

    const formattedCount = formatEvChargerCount(count);
    value.textContent = formattedCount;
    badge.classList.toggle('d-none', formattedCount === '');
}

function routeResultFuelItems(restStop) {
    const summary = restStop?.comparisonSummary;
    if (!summary || typeof summary !== 'object') {
        return [];
    }

    return [
        ['휘발유', summary.gasolinePrice, summary.gasolinePriceDiffFromAverage],
        ['경유', summary.dieselPrice, summary.dieselPriceDiffFromAverage],
        ['LPG', summary.lpgPrice, summary.lpgPriceDiffFromAverage]
    ]
        .map(([label, price, diff]) => ({
            label,
            missing: isMissingValue(price),
            price: formatText(price, 'X'),
            delta: formatOilPriceDelta(diff)
        }));
}

function createRouteResultItem(restStop, index) {
    const item = document.createElement('li');
    item.className = 'route-result-item';

    let appendTarget = item;
    const image = createRouteRestStopImage(document, restStop);
    if (image) {
        item.classList.add('route-result-item-with-image');
        const imageWrapper = document.createElement('div');
        imageWrapper.className = 'route-result-image-wrapper';
        imageWrapper.appendChild(image);
        item.appendChild(imageWrapper);

        appendTarget = document.createElement('div');
        appendTarget.className = 'route-result-content';
        item.appendChild(appendTarget);
    }

    const header = document.createElement('div');
    header.className = 'route-result-item-header';

    const rank = document.createElement('span');
    rank.className = 'route-result-rank';
    rank.textContent = String(index + 1);
    header.appendChild(rank);

    const name = document.createElement('p');
    name.className = 'route-result-name';
    name.textContent = formatText(restStop?.unitName, '이름 정보 없음');
    header.appendChild(name);
    appendTarget.appendChild(header);

    if (restStop?.hasDirectionAlternative === true) {
        const badge = document.createElement('span');
        badge.className = 'route-direction-alternative-badge';
        badge.textContent = '상·하행 후보';
        appendTarget.appendChild(badge);
    }

    const nearbyTraffic = routeNearbyTrafficBadge(restStop);
    if (nearbyTraffic) {
        const trafficBadge = document.createElement('span');
        trafficBadge.className = `route-result-traffic route-result-traffic-${nearbyTraffic.key}`;
        trafficBadge.textContent = `인근 ${nearbyTraffic.label}`;
        appendTarget.appendChild(trafficBadge);
    }

    const recommendationLabels = routeRecommendationLabels(restStop);
    if (recommendationLabels.length > 0) {
        const tags = document.createElement('div');
        tags.className = 'route-result-tags';
        recommendationLabels.forEach((label) => {
            const tag = document.createElement('span');
            tag.className = 'route-result-tag';
            tag.textContent = label;
            tags.appendChild(tag);
        });
        appendTarget.appendChild(tags);
    }

    const meta = document.createElement('p');
    meta.className = 'route-result-meta';
    meta.textContent = formatText(restStop?.routeName, '노선 정보 없음');
    appendTarget.appendChild(meta);

    const evChargerLabel = formatEvChargerAvailability(restStop);
    if (evChargerLabel) {
        const evCharger = document.createElement('span');
        evCharger.className = 'route-result-ev-charger';
        evCharger.textContent = evChargerLabel;
        appendTarget.appendChild(evCharger);
    }

    const fuels = routeResultFuelItems(restStop);
    if (fuels.length > 0) {
        const fuelList = document.createElement('div');
        fuelList.className = 'route-result-fuels';
        fuels.forEach((fuel) => {
            const fuelItem = document.createElement('span');
            fuelItem.className = 'route-result-fuel';
            const fuelLabel = document.createElement('span');
            fuelLabel.className = 'route-result-fuel-label';
            fuelLabel.textContent = fuel.label;
            fuelItem.appendChild(fuelLabel);

            const fuelPrice = document.createElement('span');
            fuelPrice.className = 'route-result-fuel-price';
            fuelPrice.classList.toggle('route-result-fuel-price-missing', fuel.missing);
            fuelPrice.textContent = fuel.price;
            fuelItem.appendChild(fuelPrice);

            if (fuel.delta !== null) {
                const delta = document.createElement('span');
                delta.className = `route-result-fuel-delta route-result-fuel-delta-${fuel.delta.tone}`;
                delta.textContent = fuel.delta.text;
                fuelItem.appendChild(delta);
            }
            fuelList.appendChild(fuelItem);
        });
        item.appendChild(fuelList);
    }

    const countSummary = formatRouteComparisonSummary(restStop).at(1);
    if (countSummary) {
        const summary = document.createElement('p');
        summary.className = 'route-result-summary';
        summary.textContent = countSummary;
        appendTarget.appendChild(summary);
    }

    item.addEventListener('click', () => selectRouteRestStop(restStop));

    return item;
}

function openRestStopPopupAt(restStop, position, { fromRouteResult = false } = {}) {
    if (selectedInfoWindow) {
        selectedInfoWindow.close();
    }

    const infoWindow = new naverMaps.InfoWindow({
        content: createPopupContent(restStop)
    });
    infoWindow.open(map, position);
    selectedInfoWindow = infoWindow;

    openDetailPanel(restStop, { fromRouteResult });
    map.panTo(position);
}

function selectRouteRestStop(restStop) {
    closeRouteResultModal();

    if (Number.isFinite(restStop?.latitude) && Number.isFinite(restStop?.longitude)) {
        openRestStopPopupAt(restStop, new naverMaps.LatLng(restStop.latitude, restStop.longitude), {
            fromRouteResult: true
        });
        return;
    }

    openDetailPanel({
        serviceAreaCode: restStop?.serviceAreaCode,
        unitName: restStop?.unitName
    }, {
        fromRouteResult: true
    });
}

function clearRouteOverlays() {
    nationalOilPriceRequest?.invalidate();

    if (routePolyline) {
        routePolyline.setMap(null);
        routePolyline = undefined;
    }

    if (originMarker) {
        originMarker.setMap(null);
        originMarker = undefined;
    }

    if (destinationMarker) {
        destinationMarker.setMap(null);
        destinationMarker = undefined;
    }

    routeMarkers.forEach((marker) => marker.setMap(null));
    routeMarkers = [];
    currentRouteRestStops = [];
    currentNationalOilPriceSummary = null;
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

function showMapError(message, status = '불러오기 실패') {
    const errorElement = document.getElementById('restStopMapError');
    if (!errorElement) {
        return;
    }

    errorElement.textContent = message;
    errorElement.classList.remove('d-none');
    setText('restStopMapStatus', status);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

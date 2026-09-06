/**
 * rest-stops-map-view.js — 네이버 지도 인스턴스 부팅, 전체 휴게소 마커 + 마커 모드 토글,
 * 선택된 팝업(InfoWindow) 하나의 열기/갱신/닫기, "내 위치" 버튼과 최근 위치 상태를 갖는다.
 *
 * map/naverMaps 인스턴스 자체는 getMap()/getNaverMaps()로 열어준다 — 경로 계획 모듈이 출발/
 * 도착 마커·경로 폴리라인·지도 클릭 기반 위치 선택을 이 위에 직접 그려야 해서, 그 각각을
 * 이 모듈의 메서드로 감싸는 대신 원본 인스턴스를 그대로 공유한다.
 */
import { setText, showApiUnavailableAlert } from './utils.js';
import { formatText } from './rest-stop-detail-formatters.js';

const MAP_CONFIG_ENDPOINT = '/api/map-config';
const REST_STOPS_ENDPOINT = '/api/rest-stops';
const NAVER_MAPS_SCRIPT_ID = 'naverMapsScript';
const NAVER_MAPS_SCRIPT_URL = 'https://oapi.map.naver.com/openapi/v3/maps.js';
const SEOUL_CENTER = {
    latitude: 37.5665,
    longitude: 126.978
};
const DEFAULT_ZOOM = 11;

const GEOLOCATION_OPTIONS = {
    enableHighAccuracy: false,
    maximumAge: 300000,
    timeout: 5000
};

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

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

export function initRestStopMapView(document, window, {
    signal,
    isMarkerClickSuppressed = () => false,
    openDetailPanel
} = {}) {
    let map;
    let naverMaps;
    let selectedInfoWindow;
    let currentLocation;
    let currentLocationMarker;
    let allRestStopMarkers = [];
    let markerMode = 'all';

    /**
     * 지도 객체까지만 준비한다 — 휴게소 목록 fetch/렌더는 loadRestStops()로 일부러 나눴다.
     * 원래 흐름이 "지도 준비 → 경로 검색/이름 검색/지도 클릭 등 나머지 바인딩 → 그 다음에야
     * 휴게소 목록 fetch"였는데, 하나로 합치면 그 바인딩들이 휴게소 목록 응답을 기다리는 동안
     * 전부 먹통이 된다(느린 네트워크에서 실제로 체감되는 차이).
     */
    async function load(mapElement) {
        try {
            const mapConfig = await fetchMapConfig();
            if (!mapConfig.naverMapsNcpKeyId) {
                showMapError('네이버 지도 API 키 설정이 필요합니다. NAVER_MAPS_NCP_KEY_ID 값을 확인해주세요.', '지도 설정 필요');
                return false;
            }

            setText('restStopMapStatus', '위치 확인 중');
            await loadNaverMapsScript(mapConfig.naverMapsNcpKeyId);

            naverMaps = window.naver?.maps;
            if (!naverMaps) {
                showMapError('네이버 지도 스크립트를 불러오지 못했습니다. 지도 API 키와 서비스 URL 등록을 확인해주세요.', '지도 로딩 실패');
                return false;
            }

            const initialCenter = await resolveInitialCenter();
            map = createMap(mapElement, naverMaps, initialCenter);
            bindLocateControl();
            bindMarkerModeToggle();
            return true;
        } catch (error) {
            console.error(error);
            showMapError('휴게소 위치를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
            return false;
        }
    }

    async function loadRestStops() {
        try {
            setText('restStopMapStatus', '휴게소 불러오는 중');
            const restStopResult = await fetchRestStops();
            if (restStopResult.status === 'external-unavailable') {
                showApiUnavailableAlert();
                renderRestStops(restStopResult.restStops);
                return;
            }

            renderRestStops(restStopResult.restStops);
        } catch (error) {
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
                if (isMarkerClickSuppressed()) {
                    return;
                }
                if (selectedInfoWindow) {
                    selectedInfoWindow.close();
                }

                infoWindow.open(map, marker);
                selectedInfoWindow = infoWindow;
                openDetailPanel?.(restStop);
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
        document.getElementById('markerModeToggle')?.addEventListener('click', toggleMarkerMode, { signal });
        updateMarkerModeButton();
    }

    function bindLocateControl() {
        document.getElementById('restStopLocateButton')?.addEventListener('click', locateCurrentPosition, { signal });
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

    function showMapError(message, status = '불러오기 실패') {
        const errorElement = document.getElementById('restStopMapError');
        if (!errorElement) {
            return;
        }

        errorElement.textContent = message;
        errorElement.classList.remove('d-none');
        setText('restStopMapStatus', status);
    }

    function updateSelectedPopup(restStop, options) {
        if (!selectedInfoWindow) {
            return;
        }

        selectedInfoWindow.setContent(createPopupContent(restStop, options));
    }

    function openPopupAt(restStop, { latitude, longitude }, { fromRouteResult = false } = {}) {
        if (!map || !naverMaps) {
            return;
        }

        if (selectedInfoWindow) {
            selectedInfoWindow.close();
        }

        const position = new naverMaps.LatLng(latitude, longitude);
        const infoWindow = new naverMaps.InfoWindow({
            content: createPopupContent(restStop)
        });
        infoWindow.open(map, position);
        selectedInfoWindow = infoWindow;

        openDetailPanel?.(restStop, { fromRouteResult });
        map.panTo(position);
    }

    function closePopup() {
        if (!selectedInfoWindow) {
            return;
        }
        selectedInfoWindow.close();
        selectedInfoWindow = undefined;
    }

    return {
        load,
        loadRestStops,
        getMap: () => map,
        getNaverMaps: () => naverMaps,
        getCurrentLocation: () => currentLocation,
        setCurrentLocation: (point) => {
            currentLocation = point;
        },
        panTo(point) {
            if (!map || !naverMaps || !point) {
                return;
            }
            if (!Number.isFinite(point.latitude) || !Number.isFinite(point.longitude)) {
                return;
            }
            map.panTo(new naverMaps.LatLng(point.latitude, point.longitude));
        },
        showCurrentLocationMarker,
        hideLocateError,
        setMarkerMode,
        openPopupAt,
        closePopup,
        updateSelectedPopup
    };
}

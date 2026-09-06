/* ===================================================
   rest-stops-map.js — 휴게소 위치 지도 화면 진입점

   실제 동작은 아래 모듈들에 나눠져 있고, 이 파일은 초기화 순서를 엮고 모듈 사이의 콜백만
   연결하는 조립부다: rest-stops-map-view.js(지도·전체 마커·선택된 팝업), rest-stops-route-
   planner.js(출발/도착 선택 + 경로 결과), rest-stops-name-search.js(휴게소명 검색),
   rest-stops-detail-panel.js(상세 패널 프레젠테이션). mapView와 routePlanner는 서로가
   필요해서(마커 클릭 억제 여부 ↔ map/naverMaps 접근) routePlanner를 먼저 만들고 mapView에
   지연 접근자(getMapView)로 넘긴다.
   =================================================== */

import { initRestStopMapView } from './rest-stops-map-view.js';
import {
    initRestStopRoutePlanner,
    routePointLabel,
    routeMapSelectionMessage,
    canRequestRouteAutomatically,
    isRouteGlobalLoadingState
} from './rest-stops-route-planner.js';
import { initRestStopNameSearch } from './rest-stops-name-search.js';
import { initRestStopDetailPanel } from './rest-stops-detail-panel.js';
import { initBottomSheetDrag } from './bottom-sheet.js';

export { createPopupContent } from './rest-stops-map-view.js';
export { routePointLabel, routeMapSelectionMessage, canRequestRouteAutomatically, isRouteGlobalLoadingState };

let mapInitializationId = 0;
let detailPanel;
let detailPanelEventController;
let mapView;
let routePlanner;
let bottomSheetController;

export async function initRestStopMap() {
    const mapElement = document.getElementById('restStopMap');
    if (!mapElement) {
        return;
    }

    const initializationId = ++mapInitializationId;
    detailPanel?.destroy();
    detailPanelEventController?.abort();
    detailPanelEventController = new globalThis.AbortController();
    const signal = detailPanelEventController.signal;

    detailPanel = initRestStopDetailPanel(document, window, {
        mountTarget: document.querySelector('.rest-stop-map-layout'),
        onPopupUpdate: (restStop, options) => mapView?.updateSelectedPopup(restStop, options),
        onRouteBack: () => routePlanner?.openRouteResultModal(),
        onClose: () => mapView?.closePopup()
    });

    // routePlanner가 mapView보다 먼저 만들어져야 해서(둘이 서로를 참조), mapView는 아직 준비되지
    // 않은 시점의 routePlanner에는 지연 접근자(getMapView)로 넘긴다 — 실제 호출은 항상 두 값이
    // 다 정해진 뒤에만 일어난다.
    routePlanner = initRestStopRoutePlanner(document, window, {
        signal,
        getMapView: () => mapView,
        openDetailPanel,
        getBottomSheetController: () => bottomSheetController
    });
    mapView = initRestStopMapView(document, window, {
        signal,
        isMarkerClickSuppressed: () => routePlanner.isMapClickActive(),
        openDetailPanel
    });

    bindPageLevelDetailEvents(signal);
    window.addEventListener('resize', () => detailPanel?.updatePresentation(), { signal });

    const ready = await mapView.load(mapElement);
    if (initializationId !== mapInitializationId) {
        return;
    }
    if (!ready) {
        return;
    }

    routePlanner.bindRouteSearch();
    initRestStopNameSearch(document, {
        signal,
        openRestStopPopupAt: (restStop, point, options) => mapView.openPopupAt(restStop, point, options),
        openDetailPanel
    });
    routePlanner.bindRouteMapClick();
    bottomSheetController = initBottomSheetDrag(document, window);
    routePlanner.initializeMobileCurrentLocationOrigin();

    await mapView.loadRestStops();
}

function openDetailPanel(restStop, { fromRouteResult = false } = {}) {
    detailPanel?.open(restStop, { fromRouteResult });
}

function closeDetailPanel() {
    detailPanel?.close({ restoreMapFocus: true });
}

// 상세 팝업 자체의 이벤트(닫기·먹거리 모달·주유 갱신)는 rest-stop-detail-popup.js가 갖고 있다.
// 여기서는 이 페이지에만 있는 다른 모달들과 함께 있을 때의 Escape 키 우선순위만 다룬다.
function bindPageLevelDetailEvents(signal) {
    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') {
            return;
        }
        if (routePlanner?.isMapClickActive()) {
            event.preventDefault();
            routePlanner.cancelMapSelection();
            return;
        }
        if (detailPanel?.isFoodModalOpen()) {
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
        if (detailPanel?.isOpen()) {
            closeDetailPanel();
        }
    }, { signal });
}

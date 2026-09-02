/**
 * "이름·거리로 찾기"/"목적지로 추천받기" 결과 카드를 눌렀을 때 뜨는 휴게소 상세 팝업.
 *
 * index.html(지도 화면)의 rest-stop-detail-view.js/rest-stop-detail-request.js를 수정 없이
 * 그대로 재사용한다 — 두 모듈 다 지도 화면 전용 로직이 없고, id로 DOM을 직접 조회·조작할
 * 뿐이라 finder.html에 같은 id의 마크업만 있으면 동일하게 동작한다. rest-stops-map.js의
 * openDetailPanel/closeDetailPanel/bindDetailPanelEvents를 finder용으로 옮겨 온 것과 같다 —
 * 다만 지도 화면에만 있는 경로 결과 되돌아가기 버튼·반응형 시트 전환은 여기 없다.
 */

import { createRestStopDetailRequest } from './rest-stop-detail-request.js';
import { createRestStopDetailView } from './rest-stop-detail-view.js';

export function initializeRestStopDetail(document) {
    const detailRequest = createRestStopDetailRequest({
        document,
        onState: (state) => detailView.renderState(state)
    });
    const detailView = createRestStopDetailView({
        refreshOilPrice: (serviceAreaCode) => detailRequest.refreshOilPrice(serviceAreaCode),
        // 부트스트랩 토스트(showApiUnavailableAlert)는 finder에 없고, 같은 내용을
        // #restStopDetailStatus 문구("상세 정보를 불러오지 못했습니다.")가 이미 보여주므로 생략한다.
        onExternalUnavailable: () => {}
    });

    bindEvents();

    function bindEvents() {
        const controller = new globalThis.AbortController();
        const { signal } = controller;

        document.getElementById('restStopDetailClose')?.addEventListener('click', closeDetail, { signal });
        document.getElementById('restStopOilRefreshButton')?.addEventListener('click', () => detailView.refreshOilInfo(), { signal });
        document.getElementById('restStopFoodToggle')?.addEventListener('click', () => detailView.toggleFoodMenu(), { signal });
        document.getElementById('restStopFoodOpen')?.addEventListener('click', () => detailView.openFoodModal(), { signal });
        document.getElementById('restStopFoodModalClose')?.addEventListener('click', () => detailView.closeFoodModal(), { signal });
        document.getElementById('restStopFoodModal')?.addEventListener('click', (event) => {
            if (event.target === event.currentTarget) {
                detailView.closeFoodModal();
            }
        }, { signal });
        document.addEventListener('keydown', (event) => {
            if (event.key !== 'Escape' || document.getElementById('restStopFoodModal')?.open) {
                return;
            }
            if (!document.getElementById('restStopDetailPanel')?.classList.contains('d-none')) {
                closeDetail();
            }
        }, { signal });
    }

    function openDetail(restStop) {
        const panel = document.getElementById('restStopDetailPanel');
        if (!panel) {
            return;
        }

        detailView.open(restStop);
        panel.classList.remove('d-none');
        detailRequest.load(restStop.serviceAreaCode);
    }

    function closeDetail() {
        detailRequest.invalidate();
        detailView.closeFoodModal();
        document.getElementById('restStopDetailPanel')?.classList.add('d-none');
    }

    return { openDetail };
}

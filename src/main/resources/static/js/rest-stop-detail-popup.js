/**
 * 휴게소 상세 팝업 — 지도 화면(index.html)과 finder 화면(finder.html)이 공유하는 컴포넌트.
 *
 * 마크업을 이 모듈이 직접 만들어서 mountTarget에 붙이기 때문에, 어느 페이지든
 * `createRestStopDetailPopup(document, {...})`만 호출하면 된다 — 페이지 템플릿에 상세 팝업
 * 마크업을 따로 작성할 필요가 없다. 실제 렌더링은 그대로 rest-stop-detail-view.js/
 * rest-stop-detail-request.js에 위임한다(둘 다 id로 DOM을 직접 조회·조작할 뿐이라, 마크업이
 * 어떻게 만들어졌든 상관없이 동작한다).
 *
 * 이 모듈이 갖는 것: 마크업, 열기/닫기, 주유 요금 갱신, 먹거리 모달(열기/닫기/토글/배경 클릭 닫기).
 * 이 모듈이 갖지 않는 것(페이지마다 다르므로 호출하는 쪽이 책임진다): 다른 모달과 함께 있는 Escape
 * 키 우선순위, 지도 화면 전용 "경로 결과로 돌아가기" 버튼의 표시 여부, 모바일 시트 프레젠테이션,
 * 닫을 때 지도 포커스를 되돌리는 것 같은 페이지별 후처리. 그런 페이지별 동작은 `onCloseRequest`
 * (닫기 버튼·먹거리 모달 배경 클릭 시 호출)로 위임받아서, 호출하는 쪽이 실제 닫기 방식을 정한다.
 */

import { createRestStopDetailRequest } from './rest-stop-detail-request.js';
import { createRestStopDetailView } from './rest-stop-detail-view.js';

const POPUP_MARKUP = `
<aside id="restStopDetailPanel" class="rest-stop-detail-panel d-none" aria-labelledby="restStopDetailName" aria-busy="false">
    <div class="rest-stop-detail-header">
        <div class="rest-stop-detail-heading">
            <button id="restStopDetailRouteBack" class="rest-stop-detail-back d-none" type="button"
                    aria-label="경로 결과로 돌아가기" aria-controls="routeResultModal">
                <span aria-hidden="true">←</span>
            </button>
            <div class="rest-stop-detail-kicker">선택한 휴게소</div>
            <h2 id="restStopDetailName" class="rest-stop-detail-name"></h2>
            <div class="rest-stop-detail-badges">
                <span id="restStopDetailEvCharger" class="rest-stop-detail-ev-charger d-none">
                    <i aria-hidden="true">⚡</i>
                    <span id="restStopDetailEvChargerText"></span>
                </span>
                <span id="restStopDetailTrafficBadge" class="rest-stop-detail-traffic-badge d-none">
                    <span aria-hidden="true">🚗</span>이용량 상위 10%
                </span>
                <ul id="restStopDetailThemes" class="rest-stop-detail-theme-badges d-none"></ul>
            </div>
        </div>
        <div class="rest-stop-detail-actions">
            <button id="restStopFoodOpen" class="rest-stop-food-open rest-stop-detail-food-open-button d-none" type="button"
                    aria-haspopup="dialog" aria-controls="restStopFoodModal">
                <span aria-hidden="true">🍽️</span>먹거리
            </button>
        </div>
        <button id="restStopDetailClose" class="rest-stop-detail-close-button" type="button" aria-label="상세 정보 닫기">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" aria-hidden="true">
                <line x1="5" y1="5" x2="19" y2="19"></line>
                <line x1="19" y1="5" x2="5" y2="19"></line>
            </svg>
        </button>
    </div>
    <div id="restStopDetailStatus" class="rest-stop-detail-status" aria-live="polite"></div>
    <div id="restStopDetailContent" class="rest-stop-detail-content d-none">
        <figure id="restStopDetailImageWrapper" class="rest-stop-detail-image-wrapper d-none">
            <img id="restStopDetailImage" class="rest-stop-detail-image" alt="">
        </figure>
        <section class="rest-stop-detail-section" aria-labelledby="restStopDetailParkingHeading">
            <h3 id="restStopDetailParkingHeading" class="rest-stop-detail-section-title">
                <span class="rest-stop-detail-section-icon" aria-hidden="true">🅿️</span>주차 정보
            </h3>
            <dl class="rest-stop-detail-list">
                <dt>총 주차 대수</dt>
                <dd id="restStopDetailTotalParking"></dd>
                <dt>대형 / 소형 / 장애인</dt>
                <dd id="restStopDetailParkingBreakdown"></dd>
            </dl>
        </section>
        <section id="restStopRestroomSection" class="rest-stop-detail-section d-none" aria-labelledby="restStopDetailRestroomHeading">
            <h3 id="restStopDetailRestroomHeading" class="rest-stop-detail-section-title">
                <span class="rest-stop-detail-section-icon" aria-hidden="true">🚻</span>화장실 정보
            </h3>
            <div class="rest-stop-detail-stat-grid">
                <div class="rest-stop-detail-stat">
                    <span class="rest-stop-detail-stat-label">🚹 남성</span>
                    <span id="restStopDetailRestroomMale" class="rest-stop-detail-stat-value"></span>
                </div>
                <div class="rest-stop-detail-stat">
                    <span class="rest-stop-detail-stat-label">🚺 여성</span>
                    <span id="restStopDetailRestroomFemale" class="rest-stop-detail-stat-value"></span>
                </div>
            </div>
        </section>
        <section id="restStopUsageSection" class="rest-stop-detail-section d-none" aria-labelledby="restStopDetailUsageHeading">
            <h3 id="restStopDetailUsageHeading" class="rest-stop-detail-section-title">
                <span class="rest-stop-detail-section-icon" aria-hidden="true">📊</span>이용 현황
            </h3>
            <div class="rest-stop-detail-stat-grid">
                <div class="rest-stop-detail-stat">
                    <span class="rest-stop-detail-stat-label">1일 평균 이용객</span>
                    <span id="restStopDetailDailyVisitorCount" class="rest-stop-detail-stat-value"></span>
                </div>
                <div class="rest-stop-detail-stat">
                    <span class="rest-stop-detail-stat-label">1일 평균 통행량</span>
                    <span id="restStopDetailDailyTrafficVolume" class="rest-stop-detail-stat-value"></span>
                </div>
            </div>
        </section>
        <section id="restStopOilSection" class="rest-stop-detail-section" aria-labelledby="restStopDetailOilHeading">
            <div class="rest-stop-detail-section-header">
                <h3 id="restStopDetailOilHeading" class="rest-stop-detail-section-title">
                    <span class="rest-stop-detail-section-icon" aria-hidden="true">⛽</span>주유 정보
                </h3>
                <button id="restStopOilRefreshButton" class="rest-stop-detail-refresh-button" type="button">실시간 요금 갱신</button>
            </div>
            <p id="restStopOilRefreshStatus" class="rest-stop-detail-meta" aria-live="polite"></p>
            <h4 class="rest-stop-detail-subtitle">요금</h4>
            <dl class="rest-stop-detail-list">
                <dt>휘발유</dt>
                <dd id="restStopOilGasolinePrice"></dd>
                <dt>경유</dt>
                <dd id="restStopOilDieselPrice"></dd>
                <dt>LPG</dt>
                <dd id="restStopOilLpgPrice"></dd>
            </dl>
            <h4 class="rest-stop-detail-subtitle">기본 정보</h4>
            <dl class="rest-stop-detail-list">
                <dt>정유사</dt>
                <dd id="restStopOilCompany"></dd>
                <dt>전화번호</dt>
                <dd id="restStopOilTelNo"></dd>
            </dl>
            <h4 class="rest-stop-detail-subtitle">주유소 편의시설</h4>
            <ul id="restStopOilConvenienceTags" class="rest-stop-detail-tags"></ul>
            <p id="restStopOilConvenienceFallback" class="rest-stop-detail-missing d-none"></p>
            <ul id="restStopOilConvenienceDetails" class="rest-stop-oil-convenience-details"></ul>
        </section>
        <section id="restStopSalesRankingSection" class="rest-stop-detail-section d-none" aria-labelledby="restStopSalesRankingHeading">
            <div class="rest-stop-detail-section-header">
                <h3 id="restStopSalesRankingHeading" class="rest-stop-detail-section-title">
                    <span class="rest-stop-detail-section-icon" aria-hidden="true">🛍️</span>인기 판매
                </h3>
                <span id="restStopSalesRankingMonth" class="rest-stop-detail-meta"></span>
            </div>
            <p class="rest-stop-sales-ranking-caption">휴게소 내 판매순위</p>
            <div class="rest-stop-sales-ranking-columns">
                <section id="restStopStoreRankingColumn" class="rest-stop-sales-ranking-column" aria-labelledby="restStopStoreRankingHeading">
                    <h4 id="restStopStoreRankingHeading">인기 매장</h4>
                    <ol id="restStopStoreRankingList" class="rest-stop-sales-ranking-list"></ol>
                </section>
                <section id="restStopProductRankingColumn" class="rest-stop-sales-ranking-column" aria-labelledby="restStopProductRankingHeading">
                    <h4 id="restStopProductRankingHeading">인기 상품</h4>
                    <ol id="restStopProductRankingList" class="rest-stop-sales-ranking-list"></ol>
                </section>
            </div>
        </section>
        <section class="rest-stop-detail-section" aria-labelledby="restStopDetailLocationHeading">
            <h3 id="restStopDetailLocationHeading" class="rest-stop-detail-section-title">
                <span class="rest-stop-detail-section-icon" aria-hidden="true">📍</span>위치 정보
            </h3>
            <dl class="rest-stop-detail-list">
                <dt>노선</dt>
                <dd id="restStopDetailRoute"></dd>
                <dt>방향</dt>
                <dd id="restStopDetailDirection"></dd>
                <dt>주소</dt>
                <dd id="restStopDetailAddress"></dd>
            </dl>
        </section>
        <section class="rest-stop-detail-section" aria-labelledby="restStopDetailConvenienceHeading">
            <h3 id="restStopDetailConvenienceHeading" class="rest-stop-detail-section-title">
                <span class="rest-stop-detail-section-icon" aria-hidden="true">👥</span>편의시설
            </h3>
            <ul id="restStopDetailConvenience" class="rest-stop-detail-tags"></ul>
            <p id="restStopDetailConvenienceFallback" class="rest-stop-detail-missing d-none"></p>
        </section>
        <section class="rest-stop-detail-section" aria-labelledby="restStopDetailOperationHeading">
            <h3 id="restStopDetailOperationHeading" class="rest-stop-detail-section-title">운영 상태</h3>
            <dl class="rest-stop-detail-list">
                <dt>경정비</dt>
                <dd id="restStopDetailMaintenance"></dd>
                <dt>화물휴게소</dt>
                <dd id="restStopDetailFreight"></dd>
            </dl>
        </section>
        <section id="restStopDetailEventSection" class="rest-stop-detail-section d-none" aria-labelledby="restStopDetailEventHeading">
            <h3 id="restStopDetailEventHeading" class="rest-stop-detail-section-title">
                <span class="rest-stop-detail-section-icon" aria-hidden="true">🎉</span>진행 중인 이벤트
            </h3>
            <ul id="restStopDetailEventList" class="rest-stop-detail-event-list"></ul>
        </section>
    </div>
    <dialog id="restStopFoodModal" class="rest-stop-food-modal" aria-labelledby="restStopFoodModalTitle">
        <div class="rest-stop-food-modal-header">
            <h3 id="restStopFoodModalTitle" class="rest-stop-food-modal-title">먹거리</h3>
            <button id="restStopFoodModalClose" class="rest-stop-detail-close-button" type="button" aria-label="먹거리 닫기">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" aria-hidden="true">
                    <line x1="5" y1="5" x2="19" y2="19"></line>
                    <line x1="19" y1="5" x2="5" y2="19"></line>
                </svg>
            </button>
        </div>
        <div class="rest-stop-food-modal-body">
            <button id="restStopFoodToggle" class="rest-stop-detail-food-toggle-button d-none" type="button"
                    aria-expanded="false" aria-controls="restStopFoodList">전체 메뉴 보기</button>
            <ul id="restStopFoodList" class="rest-stop-food-list"></ul>
        </div>
    </dialog>
</aside>`;

export function createRestStopDetailPopup(document, {
    mountTarget = document.body,
    onPopupUpdate,
    onPresentationChange,
    onExternalUnavailable,
    onCloseRequest,
    onRouteBack
} = {}) {
    const root = document.createRange().createContextualFragment(POPUP_MARKUP).firstElementChild;
    mountTarget.appendChild(root);

    const detailRequest = createRestStopDetailRequest({
        document,
        onState: (state) => detailView.renderState(state)
    });
    const detailView = createRestStopDetailView({
        onPopupUpdate,
        onPresentationChange,
        refreshOilPrice: (serviceAreaCode) => detailRequest.refreshOilPrice(serviceAreaCode),
        onExternalUnavailable
    });

    const controller = new globalThis.AbortController();
    bindEvents();

    function bindEvents() {
        const { signal } = controller;

        document.getElementById('restStopDetailClose')?.addEventListener('click', () => onCloseRequest?.(), { signal });
        document.getElementById('restStopDetailRouteBack')?.addEventListener('click', () => onRouteBack?.(), { signal });
        document.getElementById('restStopOilRefreshButton')?.addEventListener('click', () => detailView.refreshOilInfo(), { signal });
        document.getElementById('restStopFoodToggle')?.addEventListener('click', () => detailView.toggleFoodMenu(), { signal });
        document.getElementById('restStopFoodOpen')?.addEventListener('click', () => detailView.openFoodModal(), { signal });
        document.getElementById('restStopFoodModalClose')?.addEventListener('click', () => detailView.closeFoodModal(), { signal });
        document.getElementById('restStopFoodModal')?.addEventListener('click', (event) => {
            if (event.target === event.currentTarget) {
                detailView.closeFoodModal();
            }
        }, { signal });
    }

    function open(restStop) {
        detailView.open(restStop);
        root.classList.remove('d-none');
        detailRequest.load(restStop.serviceAreaCode);
    }

    function close() {
        detailRequest.invalidate();
        detailView.closeFoodModal();
        root.classList.add('d-none');
        root.setAttribute('aria-busy', 'false');
    }

    function isOpen() {
        return !root.classList.contains('d-none');
    }

    function isFoodModalOpen() {
        return Boolean(document.getElementById('restStopFoodModal')?.open);
    }

    function destroy() {
        controller.abort();
        root.remove();
    }

    return {
        root,
        open,
        close,
        isOpen,
        isFoodModalOpen,
        closeFoodModal: () => detailView.closeFoodModal(),
        refreshOilInfo: () => detailView.refreshOilInfo(),
        destroy
    };
}

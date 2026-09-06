/**
 * rest-stops-detail-panel.js — 지도 화면 휴게소 상세 패널의 프레젠테이션 글루. 실제 상세
 * 데이터 조회·렌더링은 rest-stop-detail-popup.js가 갖고, 이 모듈은 그 팝업의 열기/닫기
 * 상태, 모바일 바텀시트 vs 데스크톱 옆 패널 프레젠테이션, "경로 결과로 돌아가기" 버튼
 * 노출 여부만 다룬다.
 *
 * 지도의 InfoWindow 팝업은 이 패널과 항상 쌍으로 열리고 닫힌다 — 닫기 버튼·스와이프·
 * "경로 결과로 돌아가기" 전부 이 모듈 내부 close()를 거치므로, 그 InfoWindow를 닫는 것도
 * onClose 콜백으로 함께 위임받아 매 닫기 경로에서 빠짐없이 호출한다. 다른 모달과 함께
 * 있을 때의 Escape 키 우선순위만 이 모듈이 모르는 페이지 전체 관심사라 호출하는 쪽
 * (rest-stops-map.js)이 isOpen()/isFoodModalOpen()으로 판단한다.
 */
import { createRestStopDetailPopup } from './rest-stop-detail-popup.js';

const MOBILE_DETAIL_SHEET_MEDIA = '(max-width: 991.98px)';

export function isMobileDetailSheet(window) {
    return window.matchMedia(MOBILE_DETAIL_SHEET_MEDIA).matches;
}

export function shouldShowRouteResultBackButton(openedFromRouteResult, isMobileSheet) {
    return openedFromRouteResult === true && isMobileSheet === true;
}

export function initRestStopDetailPanel(document, window, {
    mountTarget,
    onPopupUpdate,
    onRouteBack,
    onClose
} = {}) {
    let openedFromRouteResult = false;

    const detailPopup = createRestStopDetailPopup(document, {
        mountTarget,
        onPopupUpdate,
        onPresentationChange: updatePresentation,
        onCloseRequest: () => close({ restoreMapFocus: true }),
        onRouteBack: () => {
            close();
            onRouteBack?.();
        }
    });

    function updateBackButton() {
        const button = document.getElementById('restStopDetailRouteBack');
        if (!button) {
            return;
        }

        button.classList.toggle(
            'd-none',
            !shouldShowRouteResultBackButton(openedFromRouteResult, isMobileDetailSheet(window))
        );
    }

    function updatePresentation() {
        detailPopup.updatePresentation();
        updateBackButton();
    }

    function open(restStop, { fromRouteResult = false } = {}) {
        openedFromRouteResult = fromRouteResult;
        detailPopup.open(restStop);
        updatePresentation();
    }

    function close({ restoreMapFocus = false } = {}) {
        detailPopup.close();
        openedFromRouteResult = false;
        updatePresentation();
        onClose?.();

        if (restoreMapFocus) {
            document.getElementById('restStopMap')?.focus();
        }
    }

    return {
        open,
        close,
        updatePresentation,
        isOpen: () => detailPopup.isOpen(),
        isFoodModalOpen: () => detailPopup.isFoodModalOpen(),
        destroy: () => detailPopup.destroy()
    };
}

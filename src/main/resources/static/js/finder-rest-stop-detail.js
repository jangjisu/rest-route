/**
 * "이름·거리로 찾기"/"목적지로 추천받기" 결과 카드를 눌렀을 때 뜨는 휴게소 상세 팝업.
 *
 * 실제 팝업(마크업 포함)은 rest-stop-detail-popup.js가 지도 화면(index.html)과 함께 공유한다 —
 * 이 파일은 finder 전용 설정(부트스트랩 토스트 대신 no-op)과 Escape 키 처리만 얹는 얇은 어댑터다.
 */

import { createRestStopDetailPopup } from './rest-stop-detail-popup.js';

export function initializeRestStopDetail(document) {
    const popup = createRestStopDetailPopup(document, {
        // 부트스트랩 토스트(showApiUnavailableAlert, 기본값)는 finder에 없고, 같은 내용을
        // #restStopDetailStatus 문구("상세 정보를 불러오지 못했습니다.")가 이미 보여주므로 생략한다.
        onExternalUnavailable: () => {},
        onCloseRequest: () => popup.close()
    });
    // 지도 화면은 사이드 패널로, finder는 부트스트랩 없는 모바일 화면이라 전체 화면 오버레이로 띄운다
    // (배치만 다르고 마크업·동작은 동일 — finder.css의 .finder-detail-panel).
    popup.root.classList.add('finder-detail-panel');

    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape' || popup.isFoodModalOpen()) {
            return;
        }
        if (popup.isOpen()) {
            popup.close();
        }
    });

    return { openDetail: popup.open };
}

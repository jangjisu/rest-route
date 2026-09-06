/**
 * pointer-drag.js — 세로 드래그 제스처(pointerdown/move/up/cancel)의 공용 뼈대.
 *
 * bottom-sheet.js(바텀시트 리사이즈)와 rest-stop-detail-popup.js(스와이프로 닫기)가 각자
 * 구현하던 pointerdown/move/up 배관을 뽑았다. "이 드래그를 시작해도 되는지"(canStart)와
 * "진행 중/끝났을 때 뭘 할지"(onMove/onEnd)만 호출하는 쪽이 정하고, dragging 여부·시작
 * Y좌표·포인터 capture는 여기서 함께 처리한다.
 *
 * setPointerCapture/releasePointerCapture는 "지금 이 포인터가 실제로 눌려 있는 상태"가
 * 아니면 NotFoundError를 던진다(예: pointercancel 이후, 혹은 실제 기기가 아닌 합성 이벤트) —
 * 있으면 좋은 최적화일 뿐 핵심 로직은 아니라서, 실패해도 드래그 자체는 그대로 진행되게
 * 무시한다.
 */
export function bindVerticalPointerDrag(target, {
    signal,
    canStart = () => true,
    onStart = () => {},
    onMove = () => {},
    onEnd = () => {}
} = {}) {
    let dragging = false;
    let startClientY = 0;

    function safeCapture(fn) {
        try {
            fn();
        } catch {
            // no-op
        }
    }

    function onPointerDown(event) {
        if (!canStart(event)) {
            return;
        }
        dragging = true;
        startClientY = event.clientY;
        onStart(event);
        safeCapture(() => target.setPointerCapture?.(event.pointerId));
    }

    function onPointerMove(event) {
        if (!dragging) {
            return;
        }
        onMove(event.clientY - startClientY, event);
    }

    function onPointerUp(event) {
        if (!dragging) {
            return;
        }
        dragging = false;
        safeCapture(() => target.releasePointerCapture?.(event.pointerId));
        onEnd(event.clientY - startClientY, event);
    }

    target.addEventListener('pointerdown', onPointerDown, { signal });
    target.addEventListener('pointermove', onPointerMove, { signal });
    target.addEventListener('pointerup', onPointerUp, { signal });
    target.addEventListener('pointercancel', onPointerUp, { signal });
}

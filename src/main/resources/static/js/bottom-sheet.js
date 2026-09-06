/* ===================================================
   bottom-sheet.js — 모바일 바텀시트 드래그 리사이즈
   =================================================== */

import { bindVerticalPointerDrag } from './pointer-drag.js';

const MOBILE_SHEET_MEDIA = '(max-width: 575.98px)';
const SNAP_RATIOS = { peek: 0.4, half: 0.62, full: 0.85 };
const MIN_HEIGHT_RATIO = 0.18;
const MAX_HEIGHT_RATIO = 0.92;
const SNAP_TRANSITION = 'max-height .28s cubic-bezier(0.2, 0.8, 0.2, 1)';

export function clampHeight(px, min, max) {
    return Math.min(max, Math.max(min, px));
}

export function nearestSnap(px, viewportHeight, snaps = SNAP_RATIOS) {
    let best = Object.keys(snaps)[0];
    let bestDistance = Infinity;

    for (const [name, ratio] of Object.entries(snaps)) {
        const distance = Math.abs(px - viewportHeight * ratio);
        if (distance < bestDistance) {
            bestDistance = distance;
            best = name;
        }
    }

    return best;
}

export function snapHeightPx(name, viewportHeight, snaps = SNAP_RATIOS) {
    return viewportHeight * snaps[name];
}

export function initBottomSheetDrag(document, window) {
    const dialog = document.getElementById('routeResultModal');
    const handle = document.getElementById('routeResultModalHandle');

    if (!dialog || !handle) {
        return { resetHeight() {} };
    }

    let startHeight = 0;

    const isMobile = () => window.matchMedia(MOBILE_SHEET_MEDIA).matches;
    const viewportHeight = () => window.innerHeight;

    function applyHeightPx(px) {
        const vh = viewportHeight();
        const min = vh * MIN_HEIGHT_RATIO;
        const max = vh * MAX_HEIGHT_RATIO;
        dialog.style.maxHeight = `${clampHeight(px, min, max)}px`;
    }

    function snapTo(name) {
        dialog.style.transition = SNAP_TRANSITION;
        applyHeightPx(snapHeightPx(name, viewportHeight()));
    }

    function resetHeight() {
        dialog.style.maxHeight = '';
        dialog.style.transition = '';
    }

    bindVerticalPointerDrag(handle, {
        canStart: isMobile,
        onStart() {
            startHeight = dialog.getBoundingClientRect().height;
            dialog.style.transition = 'none';
        },
        onMove(deltaY) {
            applyHeightPx(startHeight - deltaY);
        },
        onEnd() {
            const finalHeight = dialog.getBoundingClientRect().height;
            snapTo(nearestSnap(finalHeight, viewportHeight()));
        }
    });

    return { resetHeight, snapTo };
}

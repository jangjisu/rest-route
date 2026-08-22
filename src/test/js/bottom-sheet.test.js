import assert from 'node:assert/strict';
import test from 'node:test';

import {
    clampHeight,
    initBottomSheetDrag,
    nearestSnap,
    snapHeightPx
} from '../../main/resources/static/js/bottom-sheet.js';

test('clampHeight keeps in-range values unchanged', () => {
    assert.equal(clampHeight(400, 100, 800), 400);
});

test('clampHeight clamps values below the minimum', () => {
    assert.equal(clampHeight(50, 100, 800), 100);
});

test('clampHeight clamps values above the maximum', () => {
    assert.equal(clampHeight(900, 100, 800), 800);
});

test('nearestSnap picks peek for a height close to the peek ratio', () => {
    assert.equal(nearestSnap(310, 800, { peek: 0.4, half: 0.62, full: 0.85 }), 'peek');
});

test('nearestSnap picks half for a height close to the half ratio', () => {
    assert.equal(nearestSnap(500, 800, { peek: 0.4, half: 0.62, full: 0.85 }), 'half');
});

test('nearestSnap picks full for a height close to the full ratio', () => {
    assert.equal(nearestSnap(690, 800, { peek: 0.4, half: 0.62, full: 0.85 }), 'full');
});

test('snapHeightPx multiplies the named ratio by the viewport height', () => {
    assert.equal(snapHeightPx('half', 800, { half: 0.62 }), 496);
});

function createHandle() {
    const listeners = {};
    return {
        listeners,
        addEventListener(type, handler) { listeners[type] = handler; },
        setPointerCapture() {}
    };
}

function createDialog(initialHeight) {
    return {
        style: {},
        height: initialHeight,
        getBoundingClientRect() { return { height: this.height }; }
    };
}

function createWindowStub({ isMobile, innerHeight = 800 }) {
    return {
        innerHeight,
        matchMedia: () => ({ matches: isMobile })
    };
}

function createDocumentStub(dialog, handle) {
    const elements = { routeResultModal: dialog, routeResultModalHandle: handle };
    return { getElementById: (id) => elements[id] };
}

test('initBottomSheetDrag returns a no-op controller when the dialog or handle is missing', () => {
    const document = { getElementById: () => null };
    const controller = initBottomSheetDrag(document, createWindowStub({ isMobile: true }));

    assert.doesNotThrow(() => controller.resetHeight());
});

test('initBottomSheetDrag ignores drag gestures outside the mobile breakpoint', () => {
    const dialog = createDialog(500);
    const handle = createHandle();
    const document = createDocumentStub(dialog, handle);
    const window = createWindowStub({ isMobile: false });

    initBottomSheetDrag(document, window);
    handle.listeners.pointerdown({ clientY: 400, pointerId: 1 });
    handle.listeners.pointermove({ clientY: 200 });
    handle.listeners.pointerup({});

    assert.equal(dialog.style.maxHeight, undefined);
});

test('initBottomSheetDrag follows the pointer while dragging on mobile', () => {
    const dialog = createDialog(500);
    const handle = createHandle();
    const document = createDocumentStub(dialog, handle);
    const window = createWindowStub({ isMobile: true, innerHeight: 800 });

    initBottomSheetDrag(document, window);
    handle.listeners.pointerdown({ clientY: 400, pointerId: 1 });
    handle.listeners.pointermove({ clientY: 300 });

    assert.equal(dialog.style.maxHeight, '600px');
});

test('initBottomSheetDrag snaps to the nearest point on release', () => {
    const dialog = createDialog(500);
    const handle = createHandle();
    const document = createDocumentStub(dialog, handle);
    const window = createWindowStub({ isMobile: true, innerHeight: 800 });

    initBottomSheetDrag(document, window);
    handle.listeners.pointerdown({ clientY: 400, pointerId: 1 });
    handle.listeners.pointermove({ clientY: 300 });
    handle.listeners.pointerup({});

    assert.equal(dialog.style.maxHeight, `${800 * 0.62}px`);
    assert.match(dialog.style.transition, /max-height/);
});

test('initBottomSheetDrag resetHeight clears inline height and transition', () => {
    const dialog = createDialog(500);
    const handle = createHandle();
    const document = createDocumentStub(dialog, handle);
    const window = createWindowStub({ isMobile: true, innerHeight: 800 });

    const controller = initBottomSheetDrag(document, window);
    handle.listeners.pointerdown({ clientY: 400, pointerId: 1 });
    handle.listeners.pointermove({ clientY: 200 });
    handle.listeners.pointerup({});
    controller.resetHeight();

    assert.equal(dialog.style.maxHeight, '');
    assert.equal(dialog.style.transition, '');
});

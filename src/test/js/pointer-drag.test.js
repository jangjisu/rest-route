import assert from 'node:assert/strict';
import test from 'node:test';

import { bindVerticalPointerDrag } from '../../main/resources/static/js/pointer-drag.js';

function createTarget() {
    const listeners = {};
    return {
        listeners,
        addEventListener(type, handler) {
            listeners[type] = handler;
        },
        setPointerCapture() {},
        releasePointerCapture() {}
    };
}

test('canStart gates whether a drag begins', () => {
    const target = createTarget();
    const moves = [];
    bindVerticalPointerDrag(target, {
        canStart: () => false,
        onMove: (deltaY) => moves.push(deltaY)
    });

    target.listeners.pointerdown({ clientY: 100, pointerId: 1 });
    target.listeners.pointermove({ clientY: 50 });

    assert.deepEqual(moves, []);
});

test('onMove receives the signed distance from the drag start', () => {
    const target = createTarget();
    const moves = [];
    bindVerticalPointerDrag(target, { onMove: (deltaY) => moves.push(deltaY) });

    target.listeners.pointerdown({ clientY: 100, pointerId: 1 });
    target.listeners.pointermove({ clientY: 130 });
    target.listeners.pointermove({ clientY: 80 });

    assert.deepEqual(moves, [30, -20]);
});

test('onEnd fires once with the final distance and stops further onMove calls', () => {
    const target = createTarget();
    const moves = [];
    let ended;
    bindVerticalPointerDrag(target, {
        onMove: (deltaY) => moves.push(deltaY),
        onEnd: (deltaY) => {
            ended = deltaY;
        }
    });

    target.listeners.pointerdown({ clientY: 100, pointerId: 1 });
    target.listeners.pointermove({ clientY: 150 });
    target.listeners.pointerup({ clientY: 180, pointerId: 1 });
    target.listeners.pointermove({ clientY: 999 });

    assert.equal(ended, 80);
    assert.deepEqual(moves, [50]);
});

test('pointercancel behaves the same as pointerup', () => {
    const target = createTarget();
    let ended;
    bindVerticalPointerDrag(target, {
        onEnd: (deltaY) => {
            ended = deltaY;
        }
    });

    target.listeners.pointerdown({ clientY: 100, pointerId: 1 });
    target.listeners.pointercancel({ clientY: 60, pointerId: 1 });

    assert.equal(ended, -40);
});

test('a failing pointer capture does not stop the drag', () => {
    const target = createTarget();
    target.setPointerCapture = () => {
        throw new Error('NotFoundError');
    };
    target.releasePointerCapture = () => {
        throw new Error('NotFoundError');
    };
    let started = false;
    let ended = false;
    bindVerticalPointerDrag(target, {
        onStart: () => {
            started = true;
        },
        onEnd: () => {
            ended = true;
        }
    });

    assert.doesNotThrow(() => {
        target.listeners.pointerdown({ clientY: 0, pointerId: 1 });
        target.listeners.pointerup({ clientY: 10, pointerId: 1 });
    });
    assert.equal(started, true);
    assert.equal(ended, true);
});

test('a second pointerdown before pointerup restarts the drag from the new origin', () => {
    const target = createTarget();
    const moves = [];
    bindVerticalPointerDrag(target, { onMove: (deltaY) => moves.push(deltaY) });

    target.listeners.pointerdown({ clientY: 100, pointerId: 1 });
    target.listeners.pointerup({ clientY: 120, pointerId: 1 });
    target.listeners.pointerdown({ clientY: 200, pointerId: 2 });
    target.listeners.pointermove({ clientY: 230 });

    assert.deepEqual(moves, [30]);
});

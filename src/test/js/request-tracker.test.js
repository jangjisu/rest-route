import assert from 'node:assert/strict';
import test from 'node:test';

import { createRequestTracker } from '../../main/resources/static/js/request-tracker.js';

test('begin emits state through onState while the request is still current', () => {
    const states = [];
    const tracker = createRequestTracker({ onState: (state) => states.push(state) });

    const request = tracker.begin();
    request.emit({ status: 'loading' });
    request.emit({ status: 'success' });

    assert.deepEqual(states, [{ status: 'loading' }, { status: 'success' }]);
});

test('a later begin() aborts the previous signal and makes it stale', () => {
    const tracker = createRequestTracker({ onState: () => {} });

    const first = tracker.begin();
    const second = tracker.begin();

    assert.equal(first.signal.aborted, true);
    assert.equal(second.signal.aborted, false);
});

test('a stale request no longer reaches onState after a newer begin()', () => {
    const states = [];
    const tracker = createRequestTracker({ onState: (state) => states.push(state) });

    const first = tracker.begin();
    tracker.begin();
    first.emit({ status: 'success', from: 'first' });

    assert.deepEqual(states, []);
});

test('isAborted recognizes AbortError and nothing else', () => {
    const tracker = createRequestTracker({ onState: () => {} });
    const request = tracker.begin();

    assert.equal(request.isAborted({ name: 'AbortError' }), true);
    assert.equal(request.isAborted(new TypeError('boom')), false);
    assert.equal(request.isAborted(undefined), false);
});

test('invalidate aborts the active signal and makes any in-flight request stale', () => {
    const states = [];
    const tracker = createRequestTracker({ onState: (state) => states.push(state) });

    const request = tracker.begin();
    tracker.invalidate();
    request.emit({ status: 'success' });

    assert.equal(request.signal.aborted, true);
    assert.deepEqual(states, []);
});

import assert from 'node:assert/strict';
import test from 'node:test';

import {
    describeButtonClick,
    initAnalytics,
    initButtonClickTracking,
    trackEvent,
    trackScreenView
} from '../../main/resources/static/js/analytics.js';

test('trackScreenView sends a screen_view event with the given screen name', () => {
    const calls = [];
    const gtag = (...args) => calls.push(args);

    const sent = trackScreenView('route_results', {}, gtag);

    assert.equal(sent, true);
    assert.deepEqual(calls, [['event', 'screen_view', { screen_name: 'route_results' }]]);
});

test('trackScreenView merges extra params into the event payload', () => {
    const calls = [];
    const gtag = (...args) => calls.push(args);

    trackScreenView('rest_stop_detail', { service_area_code: 'A123' }, gtag);

    assert.deepEqual(calls, [
        ['event', 'screen_view', { screen_name: 'rest_stop_detail', service_area_code: 'A123' }]
    ]);
});

test('trackScreenView does nothing and returns false when gtag is unavailable', () => {
    const sent = trackScreenView('route_results', {}, undefined);

    assert.equal(sent, false);
});

test('trackScreenView falls back to window.gtag when no function is injected', () => {
    const calls = [];
    globalThis.window = { gtag: (...args) => calls.push(args) };

    const sent = trackScreenView('route_results');

    assert.equal(sent, true);
    assert.deepEqual(calls, [['event', 'screen_view', { screen_name: 'route_results' }]]);

    delete globalThis.window;
});

test('trackEvent sends an arbitrary named event with params', () => {
    const calls = [];
    const gtag = (...args) => calls.push(args);

    const sent = trackEvent('filter_applied', { filter: 'ev_charger' }, gtag);

    assert.equal(sent, true);
    assert.deepEqual(calls, [['event', 'filter_applied', { filter: 'ev_charger' }]]);
});

test('describeButtonClick prefers aria-label, falls back to text, then id', () => {
    const withAriaLabel = fakeButton({ ariaLabel: '현재 위치 사용', text: '  ignored  ', id: 'locateBtn' });
    assert.deepEqual(describeButtonClick(withAriaLabel), {
        button_label: '현재 위치 사용',
        button_id: 'locateBtn'
    });

    const withTextOnly = fakeButton({ text: '  검색  ', id: '' });
    assert.deepEqual(describeButtonClick(withTextOnly), { button_label: '검색', button_id: undefined });

    const withNeither = fakeButton({ id: 'closeModal' });
    assert.deepEqual(describeButtonClick(withNeither), { button_label: 'closeModal', button_id: 'closeModal' });

    const withNothingIdentifying = fakeButton({});
    assert.deepEqual(describeButtonClick(withNothingIdentifying), {
        button_label: 'unknown',
        button_id: undefined
    });
});

function fakeButton({ ariaLabel, text, id }) {
    return {
        id: id ?? '',
        textContent: text ?? '',
        getAttribute: (name) => (name === 'aria-label' ? (ariaLabel ?? null) : null)
    };
}

test('initButtonClickTracking sends button_click for a click on (or inside) a button', () => {
    const calls = [];
    const gtag = (...args) => calls.push(args);
    const button = fakeButton({ text: '경로 찾기', id: 'searchBtn' });
    let registeredHandler;
    const doc = {
        addEventListener: (type, handler) => {
            if (type === 'click') {
                registeredHandler = handler;
            }
        }
    };

    initButtonClickTracking(doc, gtag);
    registeredHandler({ target: { closest: () => button } });

    assert.deepEqual(calls, [
        ['event', 'button_click', { button_label: '경로 찾기', button_id: 'searchBtn' }]
    ]);
});

test('initButtonClickTracking ignores clicks that are not on a button', () => {
    const calls = [];
    const gtag = (...args) => calls.push(args);
    let registeredHandler;
    const doc = {
        addEventListener: (type, handler) => {
            registeredHandler = handler;
        }
    };

    initButtonClickTracking(doc, gtag);
    registeredHandler({ target: { closest: () => null } });

    assert.deepEqual(calls, []);
});

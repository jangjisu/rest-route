import assert from 'node:assert/strict';
import test from 'node:test';

import {
    isMobileDetailSheet,
    shouldShowRouteResultBackButton
} from '../../main/resources/static/js/rest-stops-detail-panel.js';

test('isMobileDetailSheet reflects the media query match state', () => {
    const mobileWindow = { matchMedia: () => ({ matches: true }) };
    const desktopWindow = { matchMedia: () => ({ matches: false }) };

    assert.equal(isMobileDetailSheet(mobileWindow), true);
    assert.equal(isMobileDetailSheet(desktopWindow), false);
});

test('shouldShowRouteResultBackButton requires both mobile sheet and route-result origin', () => {
    assert.equal(shouldShowRouteResultBackButton(true, true), true);
    assert.equal(shouldShowRouteResultBackButton(true, false), false);
    assert.equal(shouldShowRouteResultBackButton(false, true), false);
    assert.equal(shouldShowRouteResultBackButton(false, false), false);
});

import assert from 'node:assert/strict';
import test from 'node:test';

import {
    canRequestRouteAutomatically,
    createPopupContent,
    formatEvChargerAvailability,
    formatEvChargerCount,
    formatOilPriceDelta,
    formatNationalOilPriceSummary,
    formatOilPriceComparison,
    formatRouteComparisonSummary,
    isRouteGlobalLoadingState,
    renderEventSection,
    renderNationalOilPriceState,
    renderOilInfo,
    renderThemeBadges,
    routeMapSelectionMessage,
    routeNearbyTrafficBadge,
    routePointLabel,
    routeRecommendationLabels,
    shouldRequestRouteAutomatically,
    shouldShowRouteResultBackButton
} from '../../main/resources/static/js/rest-stops-map.js';

test('formatEvChargerAvailability only displays an indicator for true values', () => {
    assert.equal(formatEvChargerAvailability({ hasEvCharger: true }), '전기차 충전 가능');
    assert.equal(formatEvChargerAvailability({ hasEvCharger: false }), '');
    assert.equal(formatEvChargerAvailability({}), '');
});

test('formatEvChargerCount only displays positive charger counts', () => {
    assert.equal(formatEvChargerCount(6), '6대');
    assert.equal(formatEvChargerCount(0), '');
    assert.equal(formatEvChargerCount(undefined), '');
});

function createFakeElement(classNames = []) {
    const classes = new Set(classNames);
    return {
        children: [],
        textContent: '',
        classList: {
            add: (className) => classes.add(className),
            remove: (className) => classes.delete(className),
            contains: (className) => classes.has(className),
            toggle: (className, force) => {
                const shouldAdd = force === undefined ? !classes.has(className) : force;
                if (shouldAdd) {
                    classes.add(className);
                    return true;
                }
                classes.delete(className);
                return false;
            }
        },
        appendChild(child) {
            this.children.push(child);
        },
        replaceChildren(...children) {
            this.children = children;
        }
    };
}

function withFakeOilInfoDocument(callback) {
    const previousDocument = globalThis.document;
    const elements = new Map([
        ['restStopOilSection', createFakeElement(['d-none'])],
        ['restStopOilGasolinePrice', createFakeElement()],
        ['restStopOilDieselPrice', createFakeElement()],
        ['restStopOilLpgPrice', createFakeElement()],
        ['restStopOilCompany', createFakeElement()],
        ['restStopOilTelNo', createFakeElement()],
        ['restStopOilRefreshStatus', createFakeElement()],
        ['restStopOilConvenienceTags', createFakeElement()],
        ['restStopOilConvenienceFallback', createFakeElement(['d-none'])],
        ['restStopOilConvenienceDetails', createFakeElement()]
    ]);

    globalThis.document = {
        createElement: () => createFakeElement(),
        getElementById: (id) => elements.get(id) ?? null
    };

    try {
        return callback(elements);
    } finally {
        globalThis.document = previousDocument;
    }
}

function withFakeThemeDocument(callback) {
    const previousDocument = globalThis.document;
    const elements = new Map([['restStopDetailThemes', createFakeElement(['d-none'])]]);

    globalThis.document = {
        createElement: () => createFakeElement(),
        getElementById: (id) => elements.get(id) ?? null
    };

    try {
        return callback(elements);
    } finally {
        globalThis.document = previousDocument;
    }
}

test('renderThemeBadges renders one badge per theme and reveals the list', () => {
    withFakeThemeDocument((elements) => {
        renderThemeBadges([
            { name: '4계절 꽃이 있는 휴게소', detail: '365일 꽃향기가 나는 휴게소입니다' },
            { name: '포토존', detail: '' }
        ]);

        const list = elements.get('restStopDetailThemes');
        assert.equal(list.children.length, 2);
        assert.equal(list.children[0].textContent, '4계절 꽃이 있는 휴게소');
        assert.equal(list.children[0].title, '365일 꽃향기가 나는 휴게소입니다');
        assert.equal(list.children[1].textContent, '포토존');
        assert.equal(list.children[1].title, undefined);
        assert.equal(list.classList.contains('d-none'), false);
    });
});

test('renderThemeBadges hides the list when there are no themes', () => {
    withFakeThemeDocument((elements) => {
        renderThemeBadges([]);

        assert.equal(elements.get('restStopDetailThemes').children.length, 0);
        assert.equal(elements.get('restStopDetailThemes').classList.contains('d-none'), true);
    });
});

test('renderThemeBadges tolerates missing or malformed input without throwing', () => {
    withFakeThemeDocument((elements) => {
        assert.doesNotThrow(() => renderThemeBadges(undefined));
        assert.doesNotThrow(() => renderThemeBadges(null));
        renderThemeBadges([{ name: '   ' }, { name: null }, 'not-an-object']);

        assert.equal(elements.get('restStopDetailThemes').children.length, 0);
        assert.equal(elements.get('restStopDetailThemes').classList.contains('d-none'), true);
    });
});

function withFakeEventDocument(callback) {
    const previousDocument = globalThis.document;
    const elements = new Map([
        ['restStopDetailEventSection', createFakeElement(['d-none'])],
        ['restStopDetailEventList', createFakeElement()]
    ]);

    globalThis.document = {
        createElement: () => createFakeElement(),
        getElementById: (id) => elements.get(id) ?? null
    };

    try {
        return callback(elements);
    } finally {
        globalThis.document = previousDocument;
    }
}

test('renderEventSection renders one item per event and reveals the section', () => {
    withFakeEventDocument((elements) => {
        renderEventSection([
            { name: 'TEN+1 이벤트', detail: '한식당 식사 10번 이용 시 1번 무료', period: '2026.01.01 ~ 2026.12.31' },
            { name: '포토존 이벤트', detail: '', period: '2026.03.01 ~ 2026.03.31' }
        ]);

        const list = elements.get('restStopDetailEventList');
        assert.equal(list.children.length, 2);
        assert.equal(list.children[0].children[0].textContent, 'TEN+1 이벤트');
        assert.equal(list.children[0].children[1].textContent, '2026.01.01 ~ 2026.12.31');
        assert.equal(list.children[0].children[2].textContent, '한식당 식사 10번 이용 시 1번 무료');
        assert.equal(list.children[1].children.length, 2);
        assert.equal(elements.get('restStopDetailEventSection').classList.contains('d-none'), false);
    });
});

test('renderEventSection hides the section when there are no active events', () => {
    withFakeEventDocument((elements) => {
        renderEventSection([]);

        assert.equal(elements.get('restStopDetailEventList').children.length, 0);
        assert.equal(elements.get('restStopDetailEventSection').classList.contains('d-none'), true);
    });
});

test('renderEventSection tolerates missing or malformed input without throwing', () => {
    withFakeEventDocument((elements) => {
        assert.doesNotThrow(() => renderEventSection(undefined));
        assert.doesNotThrow(() => renderEventSection(null));
        renderEventSection([{ name: '   ' }, { name: null }, 'not-an-object']);

        assert.equal(elements.get('restStopDetailEventList').children.length, 0);
        assert.equal(elements.get('restStopDetailEventSection').classList.contains('d-none'), true);
    });
});

test('createPopupContent renders rest stop popup as a small summary card', () => {
    const content = createPopupContent({
        unitName: '서울만남(부산)휴게소',
        routeName: '경부선'
    });

    assert.match(content, /rest-stop-map-popup-card/);
    assert.match(content, /서울만남\(부산\)휴게소/);
    assert.match(content, /경부선/);
    assert.match(content, /정보를 불러오는 중/);
});

test('createPopupContent renders availability tags on success', () => {
    const content = createPopupContent(
        { unitName: '안성(부산)휴게소', routeName: '경부선' },
        { status: 'success', tags: [{ key: 'food', label: '먹거리' }, { key: 'oil', label: '주유' }] }
    );

    assert.match(content, /rest-stop-map-popup-tag/);
    assert.match(content, /먹거리/);
    assert.match(content, /주유/);
    assert.doesNotMatch(content, /오른쪽 패널/);
});

test('createPopupContent shows empty notice when no data tags exist', () => {
    const content = createPopupContent(
        { unitName: '안성(부산)휴게소', routeName: '경부선' },
        { status: 'success', tags: [] }
    );

    assert.match(content, /등록된 정보 없음/);
    assert.doesNotMatch(content, /rest-stop-map-popup-tag"/);
});

test('createPopupContent escapes rest stop text before rendering HTML', () => {
    const content = createPopupContent({
        unitName: '<script>alert(1)</script>',
        routeName: '<b>경부선</b>'
    });

    assert.match(content, /&lt;script&gt;alert\(1\)&lt;\/script&gt;/);
    assert.match(content, /&lt;b&gt;경부선&lt;\/b&gt;/);
    assert.doesNotMatch(content, /<script>/);
    assert.doesNotMatch(content, /<b>경부선<\/b>/);
});

test('routePointLabel shows a selected point without repeating missing text', () => {
    assert.equal(
        routePointLabel({ name: '구로역', address: '서울 구로구 구로동' }, '미설정'),
        '구로역 · 서울 구로구 구로동'
    );
    assert.equal(
        routePointLabel({ name: '지도에서 선택한 위치', address: '' }, '미설정'),
        '지도에서 선택한 위치'
    );
    assert.equal(routePointLabel(undefined, '출발지를 설정하세요'), '출발지를 설정하세요');
});

test('routeMapSelectionMessage distinguishes target and confirmation state', () => {
    assert.equal(routeMapSelectionMessage('origin', false), '지도에서 출발 위치를 선택하세요.');
    assert.equal(routeMapSelectionMessage('origin', true), '선택한 출발 위치를 확정해주세요.');
    assert.equal(routeMapSelectionMessage('destination', false), '지도에서 도착 위치를 선택하세요.');
    assert.equal(routeMapSelectionMessage('destination', true), '선택한 도착 위치를 확정해주세요.');
});

test('canRequestRouteAutomatically requires selected origin and destination coordinates', () => {
    const origin = { latitude: 37.5, longitude: 126.9 };
    const destination = { latitude: 35.1, longitude: 129.0 };

    assert.equal(canRequestRouteAutomatically(origin, destination), true);
    assert.equal(canRequestRouteAutomatically(undefined, destination), false);
    assert.equal(canRequestRouteAutomatically(origin, undefined), false);
    assert.equal(canRequestRouteAutomatically(origin, { latitude: Number.NaN, longitude: 129.0 }), false);
});

test('shouldRequestRouteAutomatically allows automatic route search on mobile and desktop', () => {
    const origin = { latitude: 37.5, longitude: 126.9 };
    const destination = { latitude: 35.1, longitude: 129.0 };

    assert.equal(shouldRequestRouteAutomatically(origin, destination), true);
});

test('shouldShowRouteResultBackButton is visible only on mobile detail opened from route results', () => {
    assert.equal(shouldShowRouteResultBackButton(true, true), true);
    assert.equal(shouldShowRouteResultBackButton(true, false), false);
    assert.equal(shouldShowRouteResultBackButton(false, true), false);
    assert.equal(shouldShowRouteResultBackButton(undefined, true), false);
});

test('isRouteGlobalLoadingState is true only while route search is loading', () => {
    assert.equal(isRouteGlobalLoadingState({ status: 'loading' }), true);
    assert.equal(isRouteGlobalLoadingState({ status: 'success' }), false);
    assert.equal(isRouteGlobalLoadingState({ status: 'not-found' }), false);
    assert.equal(isRouteGlobalLoadingState({ status: 'external-unavailable' }), false);
    assert.equal(isRouteGlobalLoadingState({ status: 'error' }), false);
    assert.equal(isRouteGlobalLoadingState(undefined), false);
});

test('routeRecommendationLabels returns comparison badge labels in response order', () => {
    assert.deepEqual(routeRecommendationLabels({
        recommendationTags: [
            { key: 'lowest-gasoline', label: '휘발유 최저가' },
            { key: 'largest-parking', label: '주차장 큼' }
        ]
    }), ['휘발유 최저가', '주차장 큼']);
    assert.deepEqual(routeRecommendationLabels({ recommendationTags: [] }), []);
    assert.deepEqual(routeRecommendationLabels({}), []);
});

test('routeNearbyTrafficBadge returns the key/label pair when present', () => {
    assert.deepEqual(
        routeNearbyTrafficBadge({ nearbyTraffic: { key: 'jam', label: '정체' } }),
        { key: 'jam', label: '정체' }
    );
});

test('routeNearbyTrafficBadge returns null when there is no traffic info', () => {
    assert.equal(routeNearbyTrafficBadge({ nearbyTraffic: null }), null);
    assert.equal(routeNearbyTrafficBadge({}), null);
    assert.equal(routeNearbyTrafficBadge(undefined), null);
});

test('formatRouteComparisonSummary renders prices, parking, food and facility counts compactly', () => {
    assert.deepEqual(formatRouteComparisonSummary({
        comparisonSummary: {
            gasolinePrice: '1,650원',
            dieselPrice: '1,550원',
            lpgPrice: '1,100원',
            gasolinePriceDiffFromAverage: -43,
            dieselPriceDiffFromAverage: 20,
            lpgPriceDiffFromAverage: 0,
            totalParkingCount: 63,
            foodMenuCount: 2,
            facilityCount: 3
        }
    }), [
        '휘발유 1,650원 (-43) · 경유 1,550원 (+20) · LPG 1,100원 (0)',
        '주차 63대 · 먹거리 2개 · 시설 3개'
    ]);

    assert.deepEqual(formatRouteComparisonSummary({
        comparisonSummary: {
            gasolinePrice: null,
            dieselPrice: '1,550원',
            lpgPrice: null,
            totalParkingCount: null,
            foodMenuCount: 0,
            facilityCount: 1
        }
    }), ['경유 1,550원', '시설 1개']);
});

test('formatOilPriceComparison renders average diff only when it exists', () => {
    assert.equal(formatOilPriceComparison('1,849원', -44), '1,849원 (-44)');
    assert.equal(formatOilPriceComparison('1,920원', 27), '1,920원 (+27)');
    assert.equal(formatOilPriceComparison('1,892원', 0), '1,892원 (0)');
    assert.equal(formatOilPriceComparison('1,849원', null), '1,849원');
    assert.equal(formatOilPriceComparison(null, -44), '');
});

test('formatOilPriceDelta marks cheaper and expensive average differences', () => {
    assert.deepEqual(formatOilPriceDelta(-44), { text: '(-44)', tone: 'cheap' });
    assert.deepEqual(formatOilPriceDelta(27), { text: '(+27)', tone: 'expensive' });
    assert.deepEqual(formatOilPriceDelta(0), { text: '(0)', tone: 'same' });
    assert.equal(formatOilPriceDelta(null), null);
});

test('formatNationalOilPriceSummary renders gasoline diesel and lpg averages', () => {
    assert.deepEqual(formatNationalOilPriceSummary({
        tradeDate: '2026.07.07',
        gasoline: {
            productName: '휘발유',
            price: '1,893원',
            dailyDiff: '-4.19'
        },
        diesel: {
            productName: '경유',
            price: '1,880원',
            dailyDiff: '+3'
        },
        lpg: {
            productName: '자동차용부탄',
            price: '1,135원',
            dailyDiff: '0'
        }
    }), {
        tradeDate: '2026.07.07',
        items: [
            { label: '휘발유', price: '1,893원', dailyDiff: '↓ 4.19원', dailyDiffTone: 'favorable' },
            { label: '경유', price: '1,880원', dailyDiff: '↑ 3원', dailyDiffTone: 'unfavorable' },
            { label: 'LPG', price: '1,135원', dailyDiff: '0원', dailyDiffTone: 'same' }
        ]
    });

    assert.equal(formatNationalOilPriceSummary(null), null);
});

test('renderNationalOilPriceState renders summary success and hides summary failures independently', () => {
    const previousDocument = globalThis.document;
    const container = createFakeElement(['d-none']);
    globalThis.document = {
        createElement: () => createFakeElement(),
        getElementById: (id) => (id === 'routeNationalOilPriceSummary' ? container : null)
    };

    try {
        renderNationalOilPriceState({
            status: 'success',
            data: {
                tradeDate: '2026.07.09',
                gasoline: { price: '1,700원', dailyDiff: '-2' }
            }
        });

        assert.equal(container.classList.contains('d-none'), false);
        assert.equal(container.children.length > 0, true);

        renderNationalOilPriceState({ status: 'error' });

        assert.equal(container.classList.contains('d-none'), true);
        assert.equal(container.children.length, 0);
    } finally {
        globalThis.document = previousDocument;
    }
});

test('renderOilInfo keeps the oil section visible with empty state when oil info is missing', () => {
    withFakeOilInfoDocument((elements) => {
        renderOilInfo(null);

        assert.equal(elements.get('restStopOilSection').classList.contains('d-none'), false);
        assert.equal(elements.get('restStopOilGasolinePrice').textContent, '정보 없음');
        assert.equal(elements.get('restStopOilDieselPrice').textContent, '정보 없음');
        assert.equal(elements.get('restStopOilLpgPrice').textContent, '정보 없음');
        assert.equal(elements.get('restStopOilCompany').textContent, '정보 없음');
        assert.equal(elements.get('restStopOilTelNo').textContent, '정보 없음');
        assert.equal(elements.get('restStopOilRefreshStatus').textContent, '최근 갱신: 갱신 정보 없음');
        assert.equal(elements.get('restStopOilConvenienceFallback').textContent, '주유소 편의시설 정보 없음');
        assert.equal(elements.get('restStopOilConvenienceFallback').classList.contains('d-none'), false);
    });
});

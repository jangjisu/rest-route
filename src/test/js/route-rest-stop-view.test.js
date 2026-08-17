import assert from 'node:assert/strict';
import test from 'node:test';
import * as routeRestStopView from '../../main/resources/static/js/route-rest-stop-view.js';
import {
    filterRouteRestStops,
    formatNationalOilPriceSummary,
    formatOilPriceComparison,
    formatOilPriceDelta,
    formatRouteComparisonSummary,
    formatRouteDistance,
    formatRouteDuration,
    formatRouteOptionSummary,
    formatRouteTollFare,
    renderNationalOilPriceState,
    routeNearbyTrafficBadge,
    routeRecommendationLabels,
    routeRestStopAvailabilityLabels,
    routeRestStopCardBadges,
    routeRestStopFilterCounts
} from '../../main/resources/static/js/route-rest-stop-view.js';

function createFakeElement(classNames = []) {
    const classes = new Set(classNames);
    return {
        attributes: new Map(),
        children: [],
        eventListeners: new Map(),
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
        addEventListener(name, listener) {
            this.eventListeners.set(name, listener);
        },
        setAttribute(name, value) {
            this.attributes.set(name, String(value));
        },
        replaceChildren(...children) {
            this.children = children;
        }
    };
}

test('formatRouteDuration formats seconds as hours and minutes', () => {
    assert.equal(formatRouteDuration(12000), '3시간 20분');
    assert.equal(formatRouteDuration(3600), '1시간');
    assert.equal(formatRouteDuration(1800), '30분');
    assert.equal(formatRouteDuration(0), '');
    assert.equal(formatRouteDuration(undefined), '');
});

test('formatRouteDistance formats meters as km', () => {
    assert.equal(formatRouteDistance(250000), '250km');
    assert.equal(formatRouteDistance(9500), '9.5km');
    assert.equal(formatRouteDistance(0), '');
    assert.equal(formatRouteDistance(undefined), '');
});

test('formatRouteTollFare formats won or 무료', () => {
    assert.equal(formatRouteTollFare(4500), '톨비 4,500원');
    assert.equal(formatRouteTollFare(0), '무료');
    assert.equal(formatRouteTollFare(undefined), '무료');
});

test('formatRouteOptionSummary keeps toll out of the duration and distance line', () => {
    const route = { summary: { durationSeconds: 12000, distanceMeters: 250000, tollFareWon: 4500 } };
    assert.equal(formatRouteOptionSummary(route), '3시간 20분 · 250km');
    assert.equal(formatRouteOptionSummary({}), '');
});

test('route option cards expose selection state without a detail action arrow', () => {
    const previousDocument = globalThis.document;
    const container = createFakeElement(['d-none']);
    globalThis.document = {
        createElement: () => ({
            attributes: new Map(),
            setAttribute(name, value) {
                this.attributes.set(name, String(value));
            },
            addEventListener() {}
        }),
        getElementById: (id) => (id === 'routeOptions' ? container : null)
    };

    try {
        assert.equal(typeof routeRestStopView.renderRouteOptionCards, 'function');
        routeRestStopView.renderRouteOptionCards([
            { summary: { durationSeconds: 7200, distanceMeters: 150000, tollFareWon: 12000 } },
            { summary: { durationSeconds: 7500, distanceMeters: 160000, tollFareWon: 9000 } }
        ], 1, () => {});

        assert.equal(container.children[0].attributes.get('aria-pressed'), 'false');
        assert.equal(container.children[1].attributes.get('aria-pressed'), 'true');
        assert.doesNotMatch(container.children[0].innerHTML, /route-option-arrow/);
    } finally {
        globalThis.document = previousDocument;
    }
});

test('route rest stop cards expose detail navigation to pointer and keyboard users', () => {
    const previousDocument = globalThis.document;
    globalThis.document = { createElement: () => createFakeElement() };
    let selectedCount = 0;

    try {
        assert.equal(typeof routeRestStopView.createRouteResultItem, 'function');
        const item = routeRestStopView.createRouteResultItem({
            unitName: '목감(서울)휴게소',
            routeName: '서해안선'
        }, 0, () => {
            selectedCount += 1;
        });

        assert.equal(item.attributes.get('role'), 'button');
        assert.equal(item.tabIndex, 0);
        assert.equal(item.attributes.get('aria-label'), '목감(서울)휴게소 상세정보 보기');
        assert.equal(item.children.some((child) => child.className === 'route-result-action-arrow'), true);

        item.eventListeners.get('click')({});
        item.eventListeners.get('keydown')({ key: 'Enter', preventDefault() {} });
        item.eventListeners.get('keydown')({ key: ' ', preventDefault() {} });
        item.eventListeners.get('keydown')({ key: 'Escape', preventDefault() {} });
        assert.equal(selectedCount, 3);
    } finally {
        globalThis.document = previousDocument;
    }
});

test('routeRestStopFilterCounts counts each availability from the current route', () => {
    const restStops = [
        {
            serviceAreaCode: 'A',
            comparisonSummary: {
                gasolinePrice: '1,800원',
                dieselPrice: null,
                lpgPrice: null,
                foodMenuCount: 2
            },
            hasTheme: true,
            hasEvent: false,
            hasEvCharger: false
        },
        {
            serviceAreaCode: 'B',
            comparisonSummary: {
                gasolinePrice: null,
                dieselPrice: null,
                lpgPrice: null,
                foodMenuCount: 0
            },
            hasTheme: false,
            hasEvent: true,
            hasEvCharger: true
        },
        {
            serviceAreaCode: 'C',
            comparisonSummary: {
                gasolinePrice: null,
                dieselPrice: null,
                lpgPrice: '1,100원',
                foodMenuCount: 1
            },
            hasTheme: true,
            hasEvent: false,
            hasEvCharger: false
        }
    ];

    assert.deepEqual(routeRestStopFilterCounts(restStops), {
        all: 3,
        fuel: 2,
        food: 2,
        theme: 2,
        event: 1,
        ev: 1
    });
});

test('filterRouteRestStops returns only rest stops matching the selected filter', () => {
    const restStops = [
        {
            serviceAreaCode: 'A',
            comparisonSummary: { gasolinePrice: '1,800원', foodMenuCount: 0 },
            hasTheme: false,
            hasEvent: false,
            hasEvCharger: false
        },
        {
            serviceAreaCode: 'B',
            comparisonSummary: { gasolinePrice: null, foodMenuCount: 1 },
            hasTheme: false,
            hasEvent: false,
            hasEvCharger: false
        }
    ];

    assert.deepEqual(
        filterRouteRestStops(restStops, 'fuel').map((restStop) => restStop.serviceAreaCode),
        ['A']
    );
    assert.deepEqual(
        filterRouteRestStops(restStops, 'food').map((restStop) => restStop.serviceAreaCode),
        ['B']
    );
    assert.deepEqual(filterRouteRestStops(restStops, 'unknown'), restStops);
});

test('routeRestStopAvailabilityLabels returns card chips in display order', () => {
    assert.deepEqual(routeRestStopAvailabilityLabels({
        comparisonSummary: { gasolinePrice: '1,800원', foodMenuCount: 2 },
        hasTheme: true,
        hasEvent: true,
        hasEvCharger: true
    }), ['주유 가능', '먹거리', '테마', '이벤트', 'EV 충전']);

    assert.deepEqual(routeRestStopAvailabilityLabels({
        comparisonSummary: { gasolinePrice: null, dieselPrice: null, lpgPrice: null, foodMenuCount: 0 },
        hasTheme: false,
        hasEvent: false,
        hasEvCharger: false
    }), []);
});

test('routeRestStopCardBadges keeps availability and recommendation chips in one ordered collection', () => {
    assert.deepEqual(routeRestStopCardBadges({
        comparisonSummary: { gasolinePrice: '1,800원', foodMenuCount: 2 },
        hasTheme: false,
        hasEvent: false,
        hasEvCharger: true,
        recommendationTags: [
            { key: 'has-food', label: '먹거리 있음' },
            { key: 'large-parking', label: '주차장 큼' }
        ]
    }), [
        { label: '주유 가능', kind: 'availability' },
        { label: '먹거리', kind: 'availability' },
        { label: 'EV 충전', kind: 'availability' },
        { label: '먹거리 있음', kind: 'recommendation' },
        { label: '주차장 큼', kind: 'recommendation' }
    ]);
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

import assert from 'node:assert/strict';
import test from 'node:test';

import {
    INTEREST_OPTIONS,
    destinationBadgesFor,
    destinationConditionFilters,
    destinationFilterItems,
    destinationMatchesFilter,
    nearbyBadgesFor
} from '../../main/resources/static/js/finder-condition.js';

function nearbyItem(overrides = {}) {
    return {
        sizeTier: null,
        topTrafficTier: false,
        hasTheme: false,
        hasEvent: false,
        evChargerCount: null,
        fuelBelowAverage: null,
        ...overrides
    };
}

test('INTEREST_OPTIONS exposes the four fuel/EV choices in order', () => {
    assert.deepEqual(INTEREST_OPTIONS.map((option) => option.key), ['EV', 'GASOLINE', 'DIESEL', 'LPG']);
    assert.deepEqual(INTEREST_OPTIONS.map((option) => option.label), ['EV', '휘발유', '경유', 'LPG']);
});

test('nearbyBadgesFor shows the always-on four tags regardless of interest', () => {
    const badges = nearbyBadgesFor(
        nearbyItem({ sizeTier: 'LARGE', topTrafficTier: true, hasTheme: true, hasEvent: true }),
        null
    );

    assert.deepEqual(badges.map((badge) => badge.key), ['SIZE_LARGE', 'TOP_TRAFFIC', 'HAS_THEME', 'HAS_EVENT']);
});

test('nearbyBadgesFor adds an EV badge with the charger count only when interest is EV', () => {
    const item = nearbyItem({ evChargerCount: 8 });

    assert.deepEqual(nearbyBadgesFor(item, 'EV'), [{ key: 'EV_COUNT', label: 'EV 충전 8대' }]);
    assert.deepEqual(nearbyBadgesFor(item, null), []);
});

test('nearbyBadgesFor hides the EV badge when the charger count is missing or zero', () => {
    assert.deepEqual(nearbyBadgesFor(nearbyItem({ evChargerCount: null }), 'EV'), []);
    assert.deepEqual(nearbyBadgesFor(nearbyItem({ evChargerCount: 0 }), 'EV'), []);
});

test('nearbyBadgesFor adds a fuel badge naming the selected fuel type only when below average', () => {
    const cheaper = nearbyItem({ fuelBelowAverage: true });

    assert.deepEqual(nearbyBadgesFor(cheaper, 'DIESEL'), [{ key: 'FUEL_BELOW_AVERAGE', label: '경유 평균보다 저렴' }]);
    assert.deepEqual(nearbyBadgesFor(cheaper, 'GASOLINE'), [{ key: 'FUEL_BELOW_AVERAGE', label: '휘발유 평균보다 저렴' }]);
    assert.deepEqual(nearbyBadgesFor(cheaper, 'LPG'), [{ key: 'FUEL_BELOW_AVERAGE', label: 'LPG 평균보다 저렴' }]);
});

test('nearbyBadgesFor omits the fuel badge when not below average or no interest selected', () => {
    assert.deepEqual(nearbyBadgesFor(nearbyItem({ fuelBelowAverage: null }), 'DIESEL'), []);
    assert.deepEqual(nearbyBadgesFor(nearbyItem({ fuelBelowAverage: true }), null), []);
});

function routeItem(overrides = {}) {
    return {
        sizeTier: null,
        topTrafficTier: false,
        evChargerCount: null,
        fuelPriceTier: null,
        ...overrides
    };
}

test('destinationBadgesFor shows size/traffic regardless of interest, and hides the last slot when skipped', () => {
    const badges = destinationBadgesFor(routeItem({ sizeTier: 'LARGE', topTrafficTier: true }), null);

    assert.deepEqual(badges.map((badge) => badge.key), ['SIZE_LARGE', 'TOP_TRAFFIC']);
});

test('destinationBadgesFor adds an EV badge with the charger count only when interest is EV', () => {
    const item = routeItem({ evChargerCount: 5 });

    assert.deepEqual(destinationBadgesFor(item, 'EV'), [{ key: 'EV_CHARGER', label: 'EV 충전 5대' }]);
    assert.deepEqual(destinationBadgesFor(item, 'DIESEL'), []);
});

test('destinationBadgesFor hides the EV badge when the charger count is missing or zero', () => {
    assert.deepEqual(destinationBadgesFor(routeItem({ evChargerCount: null }), 'EV'), []);
    assert.deepEqual(destinationBadgesFor(routeItem({ evChargerCount: 0 }), 'EV'), []);
});

test('destinationBadgesFor distinguishes CHEAPEST from BELOW_AVERAGE only when a fuel interest is selected', () => {
    assert.deepEqual(destinationBadgesFor(routeItem({ fuelPriceTier: 'CHEAPEST' }), 'GASOLINE'), [
        { key: 'FUEL_CHEAPEST', label: '제일 저렴' }
    ]);
    assert.deepEqual(destinationBadgesFor(routeItem({ fuelPriceTier: 'BELOW_AVERAGE' }), 'LPG'), [
        { key: 'FUEL_BELOW_AVERAGE', label: '평균보다 저렴' }
    ]);
    assert.deepEqual(destinationBadgesFor(routeItem({ fuelPriceTier: 'CHEAPEST' }), 'EV'), []);
    assert.deepEqual(destinationBadgesFor(routeItem({ fuelPriceTier: 'CHEAPEST' }), null), []);
});

test('destinationConditionFilters always includes size and traffic, plus EV or cheap-fuel depending on interest', () => {
    assert.deepEqual(destinationConditionFilters(null).map((filter) => filter.key), ['LARGE_SIZE', 'TOP_TRAFFIC']);
    assert.deepEqual(destinationConditionFilters('EV').map((filter) => filter.key), [
        'LARGE_SIZE',
        'TOP_TRAFFIC',
        'EV_CHARGER'
    ]);
    assert.deepEqual(destinationConditionFilters('DIESEL').map((filter) => filter.key), [
        'LARGE_SIZE',
        'TOP_TRAFFIC',
        'CHEAP_FUEL'
    ]);
});

test('destinationMatchesFilter TOP_TRAFFIC matches topTrafficTier', () => {
    assert.equal(destinationMatchesFilter(routeItem({ topTrafficTier: true }), 'TOP_TRAFFIC'), true);
    assert.equal(destinationMatchesFilter(routeItem({ topTrafficTier: false }), 'TOP_TRAFFIC'), false);
});

test('destinationMatchesFilter CHEAP_FUEL matches either CHEAPEST or BELOW_AVERAGE, not a separate cheapest filter', () => {
    assert.equal(destinationMatchesFilter(routeItem({ fuelPriceTier: 'CHEAPEST' }), 'CHEAP_FUEL'), true);
    assert.equal(destinationMatchesFilter(routeItem({ fuelPriceTier: 'BELOW_AVERAGE' }), 'CHEAP_FUEL'), true);
    assert.equal(destinationMatchesFilter(routeItem({ fuelPriceTier: null }), 'CHEAP_FUEL'), false);
    assert.equal(destinationMatchesFilter(routeItem(), 'FUEL_CHEAPEST'), false);
});

test('destinationFilterItems keeps only items matching every selected filter (AND)', () => {
    const items = [
        routeItem({ sizeTier: 'LARGE', evChargerCount: 2 }),
        routeItem({ sizeTier: 'LARGE', evChargerCount: null }),
        routeItem({ sizeTier: null, evChargerCount: 2 })
    ];

    const result = destinationFilterItems(items, ['LARGE_SIZE', 'EV_CHARGER']);

    assert.equal(result.length, 1);
    assert.equal(result[0].sizeTier, 'LARGE');
    assert.equal(result[0].evChargerCount, 2);
});

test('destinationFilterItems returns everything when no filter is selected', () => {
    const items = [routeItem(), routeItem()];

    assert.equal(destinationFilterItems(items, []).length, 2);
    assert.equal(destinationFilterItems(items, undefined).length, 2);
});

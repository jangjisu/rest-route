import assert from 'node:assert/strict';
import test from 'node:test';

import {
    badgesFor,
    CONDITION_FILTERS,
    filterItems,
    INTEREST_OPTIONS,
    matchesFilter,
    nearbyBadgesFor
} from '../../main/resources/static/js/finder-condition.js';

function item(overrides = {}) {
    return {
        sizeTier: null,
        maleToiletCount: null,
        femaleToiletCount: null,
        comparisonSummary: { foodMenuCount: 0 },
        hasEvCharger: false,
        fuelPriceTier: null,
        topTrafficTier: false,
        ...overrides
    };
}

test('CONDITION_FILTERS exposes the four confirmed filter chips', () => {
    assert.deepEqual(
        CONDITION_FILTERS.map((filter) => filter.key),
        ['LARGE_SIZE', 'HAS_FOOD', 'EV_CHARGER', 'CHEAP_FUEL']
    );
});

test('matchesFilter LARGE_SIZE only matches sizeTier LARGE', () => {
    assert.equal(matchesFilter(item({ sizeTier: 'LARGE' }), 'LARGE_SIZE'), true);
    assert.equal(matchesFilter(item({ sizeTier: 'MEDIUM' }), 'LARGE_SIZE'), false);
    assert.equal(matchesFilter(item({ sizeTier: null }), 'LARGE_SIZE'), false);
});

test('matchesFilter CHEAP_FUEL matches either CHEAPEST or BELOW_AVERAGE', () => {
    assert.equal(matchesFilter(item({ fuelPriceTier: 'CHEAPEST' }), 'CHEAP_FUEL'), true);
    assert.equal(matchesFilter(item({ fuelPriceTier: 'BELOW_AVERAGE' }), 'CHEAP_FUEL'), true);
    assert.equal(matchesFilter(item({ fuelPriceTier: null }), 'CHEAP_FUEL'), false);
});

test('filterItems keeps only items matching every selected filter (AND)', () => {
    const items = [
        item({ sizeTier: 'LARGE', hasEvCharger: true }),
        item({ sizeTier: 'LARGE', hasEvCharger: false }),
        item({ sizeTier: 'SMALL', hasEvCharger: true })
    ];

    const result = filterItems(items, ['LARGE_SIZE', 'EV_CHARGER']);

    assert.equal(result.length, 1);
    assert.equal(result[0].sizeTier, 'LARGE');
    assert.equal(result[0].hasEvCharger, true);
});

test('filterItems returns everything when no filter is selected', () => {
    const items = [item(), item()];

    assert.equal(filterItems(items, []).length, 2);
    assert.equal(filterItems(items, undefined).length, 2);
});

test('badgesFor distinguishes CHEAPEST from BELOW_AVERAGE labels', () => {
    assert.deepEqual(badgesFor(item({ fuelPriceTier: 'CHEAPEST' })), [{ key: 'FUEL_CHEAPEST', label: '제일 저렴' }]);
    assert.deepEqual(badgesFor(item({ fuelPriceTier: 'BELOW_AVERAGE' })), [
        { key: 'FUEL_BELOW_AVERAGE', label: '평균보다 저렴' }
    ]);
});

test('badgesFor combines multiple applicable badges in a stable order', () => {
    const badges = badgesFor(
        item({
            sizeTier: 'LARGE',
            topTrafficTier: true,
            fuelPriceTier: 'CHEAPEST',
            hasEvCharger: true,
            comparisonSummary: { foodMenuCount: 3 }
        })
    );

    assert.deepEqual(
        badges.map((badge) => badge.key),
        ['SIZE_LARGE', 'TOP_TRAFFIC', 'FUEL_CHEAPEST', 'EV_CHARGER', 'HAS_FOOD']
    );
});

test('badgesFor returns an empty list when nothing qualifies', () => {
    assert.deepEqual(badgesFor(item()), []);
});

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

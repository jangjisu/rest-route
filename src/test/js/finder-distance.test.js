import assert from 'node:assert/strict';
import test from 'node:test';

import { formatDistance, haversineDistanceMeters, sortByDistance } from '../../main/resources/static/js/finder-distance.js';

test('haversineDistanceMeters returns 0 for identical coordinates', () => {
    const point = { latitude: 37.5, longitude: 127.0 };

    assert.equal(haversineDistanceMeters(point, point), 0);
});

test('haversineDistanceMeters returns a plausible distance for two known points', () => {
    // 서울역 -> 부산역, 실제 직선 거리는 약 325km
    const seoulStation = { latitude: 37.5547, longitude: 126.9707 };
    const busanStation = { latitude: 35.1152, longitude: 129.0403 };

    const distance = haversineDistanceMeters(seoulStation, busanStation);

    assert.ok(distance > 320000 && distance < 330000, `unexpected distance: ${distance}`);
});

test('sortByDistance annotates and sorts rest stops nearest-first', () => {
    const origin = { latitude: 37.5, longitude: 127.0 };
    const restStops = [
        { serviceAreaCode: 'FAR', yValue: '38.5', xValue: '128.0' },
        { serviceAreaCode: 'NEAR', yValue: '37.501', xValue: '127.001' }
    ];

    const sorted = sortByDistance(restStops, origin);

    assert.deepEqual(sorted.map((restStop) => restStop.serviceAreaCode), ['NEAR', 'FAR']);
    assert.ok(sorted[0].distanceMeters < sorted[1].distanceMeters);
});

test('sortByDistance drops rest stops with unparsable coordinates', () => {
    const origin = { latitude: 37.5, longitude: 127.0 };
    const restStops = [
        { serviceAreaCode: 'VALID', yValue: '37.6', xValue: '127.1' },
        { serviceAreaCode: 'MISSING', yValue: '', xValue: '' },
        { serviceAreaCode: 'GARBAGE', yValue: 'abc', xValue: 'def' }
    ];

    const sorted = sortByDistance(restStops, origin);

    assert.deepEqual(sorted.map((restStop) => restStop.serviceAreaCode), ['VALID']);
});

test('formatDistance switches from meters to kilometers at 1000m', () => {
    assert.equal(formatDistance(850), '850m');
    assert.equal(formatDistance(999.6), '1000m');
    assert.equal(formatDistance(1000), '1.0km');
    assert.equal(formatDistance(15230), '15.2km');
});

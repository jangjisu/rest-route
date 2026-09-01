import assert from 'node:assert/strict';
import test from 'node:test';

import { requestCurrentPosition } from '../../main/resources/static/js/finder-geolocation.js';

test('requestCurrentPosition resolves granted with coordinates on success', async () => {
    const geolocation = {
        getCurrentPosition: (onSuccess) => {
            onSuccess({ coords: { latitude: 37.55, longitude: 126.97 } });
        }
    };

    const result = await requestCurrentPosition({ geolocation });

    assert.deepEqual(result, { granted: true, latitude: 37.55, longitude: 126.97 });
});

test('requestCurrentPosition resolves denied when the browser reports an error', async () => {
    const geolocation = {
        getCurrentPosition: (_onSuccess, onError) => {
            onError({ code: 1, message: 'User denied' });
        }
    };

    const result = await requestCurrentPosition({ geolocation });

    assert.deepEqual(result, { granted: false, reason: 'denied' });
});

test('requestCurrentPosition resolves unsupported when geolocation is unavailable', async () => {
    const result = await requestCurrentPosition({ geolocation: undefined });

    assert.deepEqual(result, { granted: false, reason: 'unsupported' });
});

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

test('requestCurrentPosition resolves denied without retrying when permission is denied', async () => {
    let callCount = 0;
    const geolocation = {
        getCurrentPosition: (_onSuccess, onError) => {
            callCount += 1;
            onError({ code: 1, message: 'User denied' });
        }
    };

    const result = await requestCurrentPosition({ geolocation });

    assert.deepEqual(result, { granted: false, reason: 'denied' });
    assert.equal(callCount, 1);
});

test('requestCurrentPosition resolves unsupported when geolocation is unavailable', async () => {
    const result = await requestCurrentPosition({ geolocation: undefined });

    assert.deepEqual(result, { granted: false, reason: 'unsupported' });
});

test('requestCurrentPosition retries with higher accuracy when the first attempt is not a denial, and succeeds', async () => {
    let callCount = 0;
    const geolocation = {
        getCurrentPosition: (onSuccess, onError, options) => {
            callCount += 1;
            if (callCount === 1) {
                onError({ code: 2, message: 'Position unavailable' });
                return;
            }
            assert.equal(options.enableHighAccuracy, true);
            onSuccess({ coords: { latitude: 35.1, longitude: 129.05 } });
        }
    };

    const result = await requestCurrentPosition({ geolocation });

    assert.deepEqual(result, { granted: true, latitude: 35.1, longitude: 129.05 });
    assert.equal(callCount, 2);
});

test('requestCurrentPosition resolves unavailable when both attempts fail for a non-denial reason', async () => {
    let callCount = 0;
    const geolocation = {
        getCurrentPosition: (_onSuccess, onError) => {
            callCount += 1;
            onError({ code: 3, message: 'Timeout' });
        }
    };

    const result = await requestCurrentPosition({ geolocation });

    assert.deepEqual(result, { granted: false, reason: 'unavailable' });
    assert.equal(callCount, 2);
});

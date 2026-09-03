import assert from 'node:assert/strict';
import test from 'node:test';

import { createFinderRestStopNearbyRequest } from '../../main/resources/static/js/finder-rest-stop-nearby-request.js';

function jsonResponse(status, body) {
    return { status, ok: status >= 200 && status < 300, json: async () => body };
}

function collect() {
    const states = [];
    return { states, onState: (state) => states.push(state) };
}

test('파라미터가 없으면 쿼리스트링 없이 호출한다', async () => {
    const { states, onState } = collect();
    await createFinderRestStopNearbyRequest({
        fetchImpl: async (url) => {
            assert.equal(url, '/api/rest-stops/nearby');
            return jsonResponse(200, { code: 'SUCCESS', data: [] });
        },
        onState
    }).load();

    assert.deepEqual(states, [{ status: 'loading' }, { status: 'success', restStops: [] }]);
});

test('위치가 있으면 originLat/originLng를 실어 보낸다', async () => {
    const { onState } = collect();
    await createFinderRestStopNearbyRequest({
        fetchImpl: async (url) => {
            assert.equal(url, '/api/rest-stops/nearby?originLat=37.5&originLng=127');
            return jsonResponse(200, { code: 'SUCCESS', data: [] });
        },
        onState
    }).load({ originLat: 37.5, originLng: 127 });
});

test('이름이 있으면 앞뒤 공백을 지우고 name으로 실어 보낸다', async () => {
    const { onState } = collect();
    await createFinderRestStopNearbyRequest({
        fetchImpl: async (url) => {
            assert.equal(url, '/api/rest-stops/nearby?name=%EC%95%88%EC%84%B1');
            return jsonResponse(200, { code: 'SUCCESS', data: [] });
        },
        onState
    }).load({ name: '  안성  ' });
});

test('관심 항목이 있으면 fuelType으로 실어 보낸다', async () => {
    const { onState } = collect();
    await createFinderRestStopNearbyRequest({
        fetchImpl: async (url) => {
            assert.equal(url, '/api/rest-stops/nearby?fuelType=EV');
            return jsonResponse(200, { code: 'SUCCESS', data: [] });
        },
        onState
    }).load({ interest: 'EV' });
});

test('세 값이 모두 있으면 전부 실어 보낸다', async () => {
    const { onState } = collect();
    await createFinderRestStopNearbyRequest({
        fetchImpl: async (url) => {
            assert.equal(url, '/api/rest-stops/nearby?originLat=37.5&originLng=127&name=%EC%95%88%EC%84%B1&fuelType=DIESEL');
            return jsonResponse(200, { code: 'SUCCESS', data: [] });
        },
        onState
    }).load({ originLat: 37.5, originLng: 127, name: '안성', interest: 'DIESEL' });
});

test('성공 응답이면 loading 후 success와 restStops 배열을 낸다', async () => {
    const restStops = [{ unitName: '서울만남(부산)휴게소', serviceAreaCode: 'A00001' }];
    const { states, onState } = collect();
    await createFinderRestStopNearbyRequest({
        fetchImpl: async () => jsonResponse(200, { code: 'SUCCESS', data: restStops }),
        onState
    }).load();

    assert.deepEqual(states[1], { status: 'success', restStops });
});

test('네트워크 실패는 error 상태를 낸다', async () => {
    const { states, onState } = collect();
    await createFinderRestStopNearbyRequest({
        fetchImpl: async () => {
            throw new Error('down');
        },
        onState
    }).load();

    assert.equal(states.at(-1).status, 'error');
});

test('AbortError는 상태를 내지 않는다', async () => {
    const { states, onState } = collect();
    await createFinderRestStopNearbyRequest({
        fetchImpl: async () => {
            const error = new Error('aborted');
            error.name = 'AbortError';
            throw error;
        },
        onState
    }).load();

    assert.deepEqual(states, [{ status: 'loading' }]);
});

test('이전 요청은 취소되고 마지막 요청 상태만 반영된다', async () => {
    const { states, onState } = collect();
    const request = createFinderRestStopNearbyRequest({
        fetchImpl: async (_url, { signal } = {}) => new Promise((resolve, reject) => {
            signal?.addEventListener('abort', () => {
                const error = new Error('aborted');
                error.name = 'AbortError';
                reject(error);
            });
            resolve(jsonResponse(200, { code: 'SUCCESS', data: [] }));
        }),
        onState
    });

    const first = request.load({ name: '부산' });
    const second = request.load({ name: '서울' });
    await Promise.all([first, second]);

    assert.deepEqual(states, [
        { status: 'loading' },
        { status: 'loading' },
        { status: 'success', restStops: [] }
    ]);
});

import assert from 'node:assert/strict';
import test from 'node:test';

import { createRouteRestStopListRequest } from '../../main/resources/static/js/finder-route-rest-stop-list-request.js';

function jsonResponse(status, body) {
    return { status, ok: status >= 200 && status < 300, json: async () => body };
}

function collect() {
    const states = [];
    return { states, onState: (state) => states.push(state) };
}

function capturingFetch(status, body) {
    const calls = [];
    const fetchImpl = async (url) => {
        calls.push(url);
        return jsonResponse(status, body);
    };
    return { calls, fetchImpl };
}

test('destinationQuery만 있으면 그 파라미터만 실어 보낸다', async () => {
    const { calls, fetchImpl } = capturingFetch(200, { code: 'SUCCESS', data: [] });
    const { onState } = collect();
    await createRouteRestStopListRequest({ fetchImpl, onState }).load({
        originLat: 37.0,
        originLng: 127.0,
        destinationQuery: '부산역'
    });

    const url = new URL(calls[0], 'http://localhost');
    assert.equal(url.pathname, '/api/route-rest-stops/list');
    assert.equal(url.searchParams.get('originLat'), '37');
    assert.equal(url.searchParams.get('originLng'), '127');
    assert.equal(url.searchParams.get('destinationQuery'), '부산역');
    assert.equal(url.searchParams.has('destinationLat'), false);
    assert.equal(url.searchParams.has('fuelType'), false);
});

test('목적지 좌표가 있으면 좌표·이름을 실어 보내고 destinationQuery는 뺀다', async () => {
    const { calls, fetchImpl } = capturingFetch(200, { code: 'SUCCESS', data: [] });
    const { onState } = collect();
    await createRouteRestStopListRequest({ fetchImpl, onState }).load({
        originLat: 37.0,
        originLng: 127.0,
        destinationLat: 35.1148,
        destinationLng: 129.0403,
        destinationName: '부산역'
    });

    const url = new URL(calls[0], 'http://localhost');
    assert.equal(url.searchParams.get('destinationLat'), '35.1148');
    assert.equal(url.searchParams.get('destinationLng'), '129.0403');
    assert.equal(url.searchParams.get('destinationName'), '부산역');
    assert.equal(url.searchParams.has('destinationQuery'), false);
});

test('fuelType이 있으면 그대로 실어 보낸다', async () => {
    const { calls, fetchImpl } = capturingFetch(200, { code: 'SUCCESS', data: [] });
    const { onState } = collect();
    await createRouteRestStopListRequest({ fetchImpl, onState }).load({
        originLat: 37.0,
        originLng: 127.0,
        destinationQuery: '부산역',
        fuelType: 'DIESEL'
    });

    const url = new URL(calls[0], 'http://localhost');
    assert.equal(url.searchParams.get('fuelType'), 'DIESEL');
});

test('출발지 좌표나 목적지가 전부 없으면 요청하지 않고 error 상태를 낸다', async () => {
    const { states, onState } = collect();
    await createRouteRestStopListRequest({ fetchImpl: async () => jsonResponse(200, {}), onState }).load({
        originLat: NaN,
        originLng: 127.0,
        destinationQuery: '부산역'
    });
    assert.deepEqual(states, [{ status: 'error' }]);

    const b = collect();
    await createRouteRestStopListRequest({ fetchImpl: async () => jsonResponse(200, {}), onState: b.onState }).load({
        originLat: 37.0,
        originLng: 127.0,
        destinationQuery: '   '
    });
    assert.deepEqual(b.states, [{ status: 'error' }]);
});

test('성공 응답이면 loading 후 success와 목록을 낸다', async () => {
    const restStops = [{ serviceAreaCode: 'A', unitName: 'A휴게소', distanceMeters: 850.5 }];
    const { states, onState } = collect();
    await createRouteRestStopListRequest({
        fetchImpl: async () => jsonResponse(200, { code: 'SUCCESS', data: restStops }),
        onState
    }).load({ originLat: 37.0, originLng: 127.0, destinationQuery: '부산역' });

    assert.deepEqual(states[0], { status: 'loading' });
    assert.deepEqual(states[1], { status: 'success', restStops });
});

test('404 NOT_FOUND는 not-found 상태와 안내 메시지를 낸다', async () => {
    const { states, onState } = collect();
    await createRouteRestStopListRequest({
        fetchImpl: async () => jsonResponse(404, { code: 'NOT_FOUND', message: '경로를 찾을 수 없어요.' }),
        onState
    }).load({ originLat: 37.0, originLng: 127.0, destinationQuery: '없는곳' });

    assert.equal(states.at(-1).status, 'not-found');
    assert.equal(states.at(-1).message, '경로를 찾을 수 없어요.');
});

test('EXTERNAL_API_UNAVAILABLE는 external-unavailable 상태를 낸다', async () => {
    const { states, onState } = collect();
    await createRouteRestStopListRequest({
        fetchImpl: async () => jsonResponse(200, { code: 'EXTERNAL_API_UNAVAILABLE' }),
        onState
    }).load({ originLat: 37.0, originLng: 127.0, destinationQuery: '부산역' });

    assert.equal(states.at(-1).status, 'external-unavailable');
});

test('네트워크 실패는 error 상태를 낸다', async () => {
    const { states, onState } = collect();
    await createRouteRestStopListRequest({
        fetchImpl: async () => {
            throw new Error('network down');
        },
        onState
    }).load({ originLat: 37.0, originLng: 127.0, destinationQuery: '부산역' });

    assert.equal(states.at(-1).status, 'error');
});

test('AbortError는 상태를 내지 않는다', async () => {
    const { states, onState } = collect();
    await createRouteRestStopListRequest({
        fetchImpl: async () => {
            const error = new Error('aborted');
            error.name = 'AbortError';
            throw error;
        },
        onState
    }).load({ originLat: 37.0, originLng: 127.0, destinationQuery: '부산역' });

    assert.deepEqual(states, [{ status: 'loading' }]);
});

test('뒤이은 요청이 앞선 요청의 응답을 덮어쓰지 않는다', async () => {
    const { states, onState } = collect();
    let resolveFirst;
    const first = new Promise((resolve) => {
        resolveFirst = () => resolve(jsonResponse(200, { code: 'SUCCESS', data: [{ serviceAreaCode: 'OLD' }] }));
    });
    let callCount = 0;
    const fetchImpl = async () => {
        callCount += 1;
        if (callCount === 1) {
            return first;
        }
        return jsonResponse(200, { code: 'SUCCESS', data: [{ serviceAreaCode: 'NEW' }] });
    };
    const request = createRouteRestStopListRequest({ fetchImpl, onState });

    const firstLoad = request.load({ originLat: 37.0, originLng: 127.0, destinationQuery: '옛날목적지' });
    await request.load({ originLat: 37.0, originLng: 127.0, destinationQuery: '새목적지' });
    resolveFirst();
    await firstLoad;

    assert.deepEqual(states.at(-1), { status: 'success', restStops: [{ serviceAreaCode: 'NEW' }] });
});

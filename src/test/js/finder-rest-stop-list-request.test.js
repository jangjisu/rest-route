import assert from 'node:assert/strict';
import test from 'node:test';

import { createFinderRestStopListRequest } from '../../main/resources/static/js/finder-rest-stop-list-request.js';

function jsonResponse(status, body) {
    return { status, ok: status >= 200 && status < 300, json: async () => body };
}

function collect() {
    const states = [];
    return { states, onState: (state) => states.push(state) };
}

test('성공 응답이면 loading 후 success와 restStops 배열을 낸다', async () => {
    const restStops = [{ unitName: '서울만남(부산)휴게소', serviceAreaCode: 'A00001' }];
    const { states, onState } = collect();
    await createFinderRestStopListRequest({
        fetchImpl: async (url) => {
            assert.equal(url, '/api/rest-stops');
            return jsonResponse(200, { code: 'SUCCESS', data: restStops });
        },
        onState
    }).load();

    assert.deepEqual(states[0], { status: 'loading' });
    assert.deepEqual(states[1], { status: 'success', restStops });
});

test('실패 응답은 error 상태를 낸다', async () => {
    const { states, onState } = collect();
    await createFinderRestStopListRequest({
        fetchImpl: async () => jsonResponse(500, { code: 'INTERNAL_ERROR' }),
        onState
    }).load();

    assert.equal(states.at(-1).status, 'error');
});

test('네트워크 실패는 error 상태를 낸다', async () => {
    const { states, onState } = collect();
    await createFinderRestStopListRequest({
        fetchImpl: async () => {
            throw new Error('down');
        },
        onState
    }).load();

    assert.equal(states.at(-1).status, 'error');
});

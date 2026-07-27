import assert from 'node:assert/strict';
import test from 'node:test';

import { createRestStopCompareRequest } from '../../main/resources/static/js/rest-stop-compare-request.js';

function jsonResponse(status, body) {
    return { status, ok: status >= 200 && status < 300, json: async () => body };
}

function collect() {
    const states = [];
    return { states, onState: (state) => states.push(state) };
}

test('성공 응답이면 loading 후 success와 comparison을 낸다', async () => {
    const comparison = { sideA: { unitName: 'A휴게소' }, sideB: { unitName: 'B휴게소' }, result: {} };
    const { states, onState } = collect();
    await createRestStopCompareRequest({
        fetchImpl: async (url) => {
            assert.equal(url, '/api/rest-stops/compare?serviceAreaCodeA=A00001&serviceAreaCodeB=A00002');
            return jsonResponse(200, { code: 'SUCCESS', data: comparison });
        },
        onState
    }).load('A00001', 'A00002');

    assert.deepEqual(states[0], { status: 'loading' });
    assert.deepEqual(states[1], { status: 'success', comparison });
});

test('실패 응답(404/400)이면 서버 메시지와 함께 error 상태를 낸다', async () => {
    const { states, onState } = collect();
    await createRestStopCompareRequest({
        fetchImpl: async () => jsonResponse(404, { code: 'NOT_FOUND', message: '휴게소를 찾을 수 없습니다' }),
        onState
    }).load('A00001', 'UNKNOWN');

    assert.deepEqual(states.at(-1), { status: 'error', message: '휴게소를 찾을 수 없습니다' });
});

test('네트워크 실패는 error 상태를 낸다', async () => {
    const { states, onState } = collect();
    await createRestStopCompareRequest({
        fetchImpl: async () => {
            throw new Error('down');
        },
        onState
    }).load('A00001', 'A00002');

    assert.equal(states.at(-1).status, 'error');
});

test('AbortError는 상태를 내지 않는다', async () => {
    const { states, onState } = collect();
    await createRestStopCompareRequest({
        fetchImpl: async () => {
            const error = new Error('aborted');
            error.name = 'AbortError';
            throw error;
        },
        onState
    }).load('A00001', 'A00002');

    assert.deepEqual(states, [{ status: 'loading' }]);
});

test('이전 요청은 취소되고 마지막 요청 상태만 반영된다', async () => {
    const { states, onState } = collect();
    const request = createRestStopCompareRequest({
        fetchImpl: async (_url, { signal } = {}) => new Promise((resolve, reject) => {
            signal?.addEventListener('abort', () => {
                const error = new Error('aborted');
                error.name = 'AbortError';
                reject(error);
            });
            resolve(jsonResponse(200, { code: 'SUCCESS', data: {} }));
        }),
        onState
    });

    const first = request.load('A00001', 'A00002');
    const second = request.load('A00001', 'A00003');
    await Promise.all([first, second]);

    assert.deepEqual(states, [
        { status: 'loading' },
        { status: 'loading' },
        { status: 'success', comparison: {} }
    ]);
});

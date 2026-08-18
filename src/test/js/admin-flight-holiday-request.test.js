import assert from 'node:assert/strict';
import test from 'node:test';

import {
    addFlightHoliday,
    deleteFlightHoliday,
    fetchFlightHolidays
} from '../../main/resources/static/js/admin-flight-holiday-request.js';

function jsonResponse(status, body) {
    return { status, ok: status >= 200 && status < 300, json: async () => body };
}

const csrf = { headerName: 'X-CSRF-TOKEN', token: 'csrf-token' };

test('fetchFlightHolidays는 성공 시 공휴일 목록을 반환한다', async () => {
    const holidays = [{ id: 1, date: '2026-01-01', name: '신정' }];
    let requestedUrl;
    const result = await fetchFlightHolidays(async (url) => {
        requestedUrl = url;
        return jsonResponse(200, { code: 'SUCCESS', data: holidays });
    });

    assert.equal(requestedUrl, '/api/admin/flights/holidays');
    assert.deepEqual(result, { status: 'success', holidays });
});

test('fetchFlightHolidays는 네트워크 실패 시 error 상태를 낸다', async () => {
    const result = await fetchFlightHolidays(async () => {
        throw new Error('down');
    });

    assert.equal(result.status, 'error');
});

test('addFlightHoliday는 성공 시 등록된 공휴일을 반환한다', async () => {
    const holiday = { id: 5, date: '2026-09-26', name: '대체공휴일' };
    let requestInit;
    const result = await addFlightHoliday('2026-09-26', '대체공휴일', csrf, async (url, init) => {
        requestInit = init;
        return jsonResponse(200, { code: 'SUCCESS', data: holiday });
    });

    assert.equal(requestInit.method, 'POST');
    assert.equal(requestInit.headers['X-CSRF-TOKEN'], 'csrf-token');
    assert.deepEqual(JSON.parse(requestInit.body), { date: '2026-09-26', name: '대체공휴일' });
    assert.deepEqual(result, { status: 'success', holiday });
});

test('addFlightHoliday는 이미 등록된 날짜면 invalid 상태를 낸다', async () => {
    const result = await addFlightHoliday(
        '2026-09-26',
        '대체공휴일',
        csrf,
        async () => jsonResponse(400, { code: 'INVALID_PARAMETER', message: '이미 등록된 날짜입니다' })
    );

    assert.equal(result.status, 'invalid');
});

test('deleteFlightHoliday는 204면 success 상태를 낸다', async () => {
    let requestedUrl;
    let requestInit;
    const result = await deleteFlightHoliday(5, csrf, async (url, init) => {
        requestedUrl = url;
        requestInit = init;
        return jsonResponse(204, null);
    });

    assert.equal(requestedUrl, '/api/admin/flights/holidays/5');
    assert.equal(requestInit.method, 'DELETE');
    assert.deepEqual(result, { status: 'success' });
});

test('deleteFlightHoliday는 404면 not-found 상태를 낸다', async () => {
    const result = await deleteFlightHoliday(999, csrf, async () => jsonResponse(404, { code: 'NOT_FOUND' }));

    assert.equal(result.status, 'not-found');
});

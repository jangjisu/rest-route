import assert from 'node:assert/strict';
import test from 'node:test';

import {
    clearRestStopOverride,
    createRestStop,
    fetchEditableRestStop,
    saveEditableRestStop
} from '../../main/resources/static/js/admin-rest-stop-edit-request.js';

function response({ ok = true, status = 200, body } = {}) {
    return { ok, status, json: async () => body };
}

function sampleData(overrides = {}) {
    return {
        serviceAreaCode: 'A00001',
        unitCode: '001',
        unitName: '서울만남(부산)휴게소',
        routeNo: '0010',
        routeName: '경부선',
        xValue: '127.0',
        yValue: '37.0',
        telNo: '031-000-0000',
        brand: '투썸플레이스',
        routeCode: '0010',
        svarAddr: '주소',
        convenience: '수유실',
        maintenanceYn: 'O',
        truckSaYn: 'X',
        adminOverridden: false,
        ...overrides
    };
}

const CSRF = { headerName: 'X-CSRF-TOKEN', token: 'csrf-token' };

test('fetchEditableRestStop returns success with data', async () => {
    const fetchImpl = () => Promise.resolve(response({ body: { code: 'SUCCESS', data: sampleData() } }));

    const result = await fetchEditableRestStop('A00001', fetchImpl);

    assert.deepEqual(result, { status: 'success', data: sampleData() });
});

test('fetchEditableRestStop returns not-found for missing rest stop', async () => {
    const fetchImpl = () => Promise.resolve(response({ ok: false, status: 404, body: { code: 'NOT_FOUND' } }));

    const result = await fetchEditableRestStop('UNKNOWN', fetchImpl);

    assert.deepEqual(result, { status: 'not-found' });
});

test('fetchEditableRestStop returns error on network failure', async () => {
    const fetchImpl = () => Promise.reject(new Error('network down'));

    const result = await fetchEditableRestStop('A00001', fetchImpl);

    assert.deepEqual(result, { status: 'error' });
});

test('saveEditableRestStop sends PUT with JSON body and CSRF header', async () => {
    let capturedUrl;
    let capturedOptions;
    const fetchImpl = (url, options) => {
        capturedUrl = url;
        capturedOptions = options;
        return Promise.resolve(response({ body: { code: 'SUCCESS', data: sampleData({ adminOverridden: true }) } }));
    };
    const payload = { unitName: '수정된이름' };

    const result = await saveEditableRestStop('A00001', payload, CSRF, fetchImpl);

    assert.equal(capturedUrl, '/api/admin/rest-stops/A00001/editable');
    assert.equal(capturedOptions.method, 'PUT');
    assert.equal(capturedOptions.headers['X-CSRF-TOKEN'], 'csrf-token');
    assert.equal(capturedOptions.headers['Content-Type'], 'application/json');
    assert.deepEqual(JSON.parse(capturedOptions.body), payload);
    assert.deepEqual(result, { status: 'success', data: sampleData({ adminOverridden: true }) });
});

test('saveEditableRestStop returns invalid with server message on 400', async () => {
    const fetchImpl = () => Promise.resolve(response({
        ok: false,
        status: 400,
        body: { code: 'INVALID_PARAMETER', message: 'Invalid coordinate value: 숫자아님' }
    }));

    const result = await saveEditableRestStop('A00001', {}, CSRF, fetchImpl);

    assert.deepEqual(result, { status: 'invalid', message: 'Invalid coordinate value: 숫자아님' });
});

test('saveEditableRestStop returns not-found on 404', async () => {
    const fetchImpl = () => Promise.resolve(response({ ok: false, status: 404, body: { code: 'NOT_FOUND' } }));

    const result = await saveEditableRestStop('UNKNOWN', {}, CSRF, fetchImpl);

    assert.deepEqual(result, { status: 'not-found' });
});

test('createRestStop sends POST with JSON body and CSRF header', async () => {
    let capturedUrl;
    let capturedOptions;
    const fetchImpl = (url, options) => {
        capturedUrl = url;
        capturedOptions = options;
        return Promise.resolve(response({ body: { code: 'SUCCESS', data: sampleData({ adminOverridden: true }) } }));
    };
    const payload = { unitName: '가평휴게소' };

    const result = await createRestStop(payload, CSRF, fetchImpl);

    assert.equal(capturedUrl, '/api/admin/rest-stops');
    assert.equal(capturedOptions.method, 'POST');
    assert.equal(capturedOptions.headers['X-CSRF-TOKEN'], 'csrf-token');
    assert.equal(capturedOptions.headers['Content-Type'], 'application/json');
    assert.deepEqual(JSON.parse(capturedOptions.body), payload);
    assert.deepEqual(result, { status: 'success', data: sampleData({ adminOverridden: true }) });
});

test('createRestStop returns invalid with server message on 400', async () => {
    const fetchImpl = () => Promise.resolve(response({
        ok: false,
        status: 400,
        body: { code: 'INVALID_PARAMETER', message: 'Invalid coordinate value: 숫자아님' }
    }));

    const result = await createRestStop({}, CSRF, fetchImpl);

    assert.deepEqual(result, { status: 'invalid', message: 'Invalid coordinate value: 숫자아님' });
});

test('clearRestStopOverride sends DELETE and returns success', async () => {
    let capturedUrl;
    let capturedMethod;
    const fetchImpl = (url, options) => {
        capturedUrl = url;
        capturedMethod = options.method;
        return Promise.resolve(response({ body: { code: 'SUCCESS', data: sampleData({ adminOverridden: false }) } }));
    };

    const result = await clearRestStopOverride('A00001', CSRF, fetchImpl);

    assert.equal(capturedUrl, '/api/admin/rest-stops/A00001/editable/override');
    assert.equal(capturedMethod, 'DELETE');
    assert.deepEqual(result, { status: 'success', data: sampleData({ adminOverridden: false }) });
});

import assert from 'node:assert/strict';
import test from 'node:test';

import {
    clearOilStationOverride,
    fetchOilLinks,
    linkOilStation,
    searchOilStations,
    unlinkOilStation
} from '../../main/resources/static/js/admin-rest-oil-link-request.js';

function jsonResponse(status, body) {
    return { status, ok: status >= 200 && status < 300, json: async () => body };
}

const csrf = { headerName: 'X-CSRF-TOKEN', token: 'csrf-token' };

test('fetchOilLinks는 성공 시 휴게소별 연결 목록을 반환한다', async () => {
    const restStops = [{ serviceAreaCode: 'A00001', unitName: '서울만남(부산)휴게소', oilStations: [] }];
    let requestedUrl;
    const result = await fetchOilLinks(async (url) => {
        requestedUrl = url;
        return jsonResponse(200, { code: 'SUCCESS', data: restStops });
    });

    assert.equal(requestedUrl, '/api/admin/rest-stops/oil-links');
    assert.deepEqual(result, { status: 'success', restStops });
});

test('fetchOilLinks는 네트워크 실패 시 error 상태를 낸다', async () => {
    const result = await fetchOilLinks(async () => {
        throw new Error('down');
    });

    assert.equal(result.status, 'error');
});

test('searchOilStations는 이름으로 검색해 주유소 배열을 반환한다', async () => {
    const oilStations = [{ id: 1, standardRestName: 'SK에너지 마장주유소', linkedRestStopName: null }];
    let requestedUrl;
    const result = await searchOilStations('마장', null, async (url) => {
        requestedUrl = url;
        return jsonResponse(200, { code: 'SUCCESS', data: oilStations });
    });

    assert.equal(requestedUrl, '/api/admin/oil-stations/search?name=%EB%A7%88%EC%9E%A5');
    assert.deepEqual(result, { status: 'success', oilStations });
});

test('searchOilStations는 노선명을 함께 지정하면 두 조건 모두 쿼리에 포함한다', async () => {
    let requestedUrl;
    const result = await searchOilStations('마장', '경부선', async (url) => {
        requestedUrl = url;
        return jsonResponse(200, { code: 'SUCCESS', data: [] });
    });

    assert.equal(requestedUrl, '/api/admin/oil-stations/search?name=%EB%A7%88%EC%9E%A5&routeName=%EA%B2%BD%EB%B6%80%EC%84%A0');
    assert.equal(result.status, 'success');
});

test('searchOilStations는 이름 없이 노선만으로도 검색할 수 있다', async () => {
    let requestedUrl;
    await searchOilStations(null, '경부선', async (url) => {
        requestedUrl = url;
        return jsonResponse(200, { code: 'SUCCESS', data: [] });
    });

    assert.equal(requestedUrl, '/api/admin/oil-stations/search?routeName=%EA%B2%BD%EB%B6%80%EC%84%A0');
});

test('searchOilStations는 네트워크 실패 시 error 상태를 낸다', async () => {
    const result = await searchOilStations('마장', null, async () => {
        throw new Error('down');
    });

    assert.equal(result.status, 'error');
});

test('linkOilStation은 PUT으로 연결하고 CSRF 헤더를 포함한다', async () => {
    let capturedUrl;
    let capturedOptions;
    const result = await linkOilStation('1', 'A00099', csrf, async (url, options) => {
        capturedUrl = url;
        capturedOptions = options;
        return jsonResponse(200, {
            code: 'SUCCESS',
            data: { id: 1, standardRestName: 'SK에너지 마장주유소', restStopServiceAreaCode: 'A00099' }
        });
    });

    assert.equal(capturedUrl, '/api/admin/oil-stations/1/link');
    assert.equal(capturedOptions.method, 'PUT');
    assert.equal(capturedOptions.headers['X-CSRF-TOKEN'], 'csrf-token');
    assert.deepEqual(JSON.parse(capturedOptions.body), { serviceAreaCode: 'A00099' });
    assert.equal(result.status, 'success');
    assert.equal(result.oilStation.restStopServiceAreaCode, 'A00099');
});

test('linkOilStation은 404면 not-found 상태를 낸다', async () => {
    const result = await linkOilStation('99', 'A00001', csrf, async () => jsonResponse(404, { code: 'NOT_FOUND' }));

    assert.equal(result.status, 'not-found');
});

test('unlinkOilStation은 DELETE .../link를 호출한다', async () => {
    let capturedUrl;
    let capturedOptions;
    const result = await unlinkOilStation('1', csrf, async (url, options) => {
        capturedUrl = url;
        capturedOptions = options;
        return jsonResponse(200, { code: 'SUCCESS', data: { id: 1, restStopServiceAreaCode: null } });
    });

    assert.equal(capturedUrl, '/api/admin/oil-stations/1/link');
    assert.equal(capturedOptions.method, 'DELETE');
    assert.equal(result.status, 'success');
});

test('clearOilStationOverride는 DELETE .../override를 호출한다', async () => {
    let capturedUrl;
    const result = await clearOilStationOverride('1', csrf, async (url, options) => {
        capturedUrl = url;
        assert.equal(options.method, 'DELETE');
        return jsonResponse(200, { code: 'SUCCESS', data: { id: 1, adminOverridden: false } });
    });

    assert.equal(capturedUrl, '/api/admin/oil-stations/1/override');
    assert.equal(result.status, 'success');
});

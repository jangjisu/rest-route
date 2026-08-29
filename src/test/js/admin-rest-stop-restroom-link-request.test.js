import assert from 'node:assert/strict';
import test from 'node:test';

import {
    fetchRestroomLinks,
    linkRestroom,
    searchRestrooms,
    unlinkRestroom
} from '../../main/resources/static/js/admin-rest-stop-restroom-link-request.js';

function jsonResponse(status, body) {
    return { status, ok: status >= 200 && status < 300, json: async () => body };
}

const csrf = { headerName: 'X-CSRF-TOKEN', token: 'csrf-token' };

test('fetchRestroomLinks는 성공 시 휴게소별 연결 목록을 반환한다', async () => {
    const restStops = [{ serviceAreaCode: 'A00001', unitName: '죽전(서울)휴게소', linkedRestroom: null }];
    let requestedUrl;
    const result = await fetchRestroomLinks(async (url) => {
        requestedUrl = url;
        return jsonResponse(200, { code: 'SUCCESS', data: restStops });
    });

    assert.equal(requestedUrl, '/api/admin/rest-stops/restroom-links');
    assert.deepEqual(result, { status: 'success', restStops });
});

test('fetchRestroomLinks는 네트워크 실패 시 error 상태를 낸다', async () => {
    const result = await fetchRestroomLinks(async () => {
        throw new Error('down');
    });

    assert.equal(result.status, 'error');
});

test('searchRestrooms는 이름으로 검색해 화장실 현황 배열을 반환한다', async () => {
    const restrooms = [{ id: 1, sourceRestStopName: '죽전(서울)', linkedRestStopName: null }];
    let requestedUrl;
    const result = await searchRestrooms('죽전', null, async (url) => {
        requestedUrl = url;
        return jsonResponse(200, { code: 'SUCCESS', data: restrooms });
    });

    assert.equal(requestedUrl, '/api/admin/rest-stop-restrooms/search?name=%EC%A3%BD%EC%A0%84');
    assert.deepEqual(result, { status: 'success', restrooms });
});

test('searchRestrooms는 노선명을 함께 지정하면 두 조건 모두 쿼리에 포함한다', async () => {
    let requestedUrl;
    const result = await searchRestrooms('죽전', '경부선', async (url) => {
        requestedUrl = url;
        return jsonResponse(200, { code: 'SUCCESS', data: [] });
    });

    assert.equal(
        requestedUrl,
        '/api/admin/rest-stop-restrooms/search?name=%EC%A3%BD%EC%A0%84&routeName=%EA%B2%BD%EB%B6%80%EC%84%A0'
    );
    assert.equal(result.status, 'success');
});

test('searchRestrooms는 이름 없이 노선만으로도 검색할 수 있다', async () => {
    let requestedUrl;
    await searchRestrooms(null, '경부선', async (url) => {
        requestedUrl = url;
        return jsonResponse(200, { code: 'SUCCESS', data: [] });
    });

    assert.equal(requestedUrl, '/api/admin/rest-stop-restrooms/search?routeName=%EA%B2%BD%EB%B6%80%EC%84%A0');
});

test('searchRestrooms는 네트워크 실패 시 error 상태를 낸다', async () => {
    const result = await searchRestrooms('죽전', null, async () => {
        throw new Error('down');
    });

    assert.equal(result.status, 'error');
});

test('linkRestroom은 PUT으로 연결하고 CSRF 헤더를 포함한다', async () => {
    let capturedUrl;
    let capturedOptions;
    const result = await linkRestroom('1', 'A00001', csrf, async (url, options) => {
        capturedUrl = url;
        capturedOptions = options;
        return jsonResponse(200, {
            code: 'SUCCESS',
            data: { id: 1, sourceRestStopName: '죽전(서울)', restStopServiceAreaCode: 'A00001' }
        });
    });

    assert.equal(capturedUrl, '/api/admin/rest-stop-restrooms/1/link');
    assert.equal(capturedOptions.method, 'PUT');
    assert.equal(capturedOptions.headers['X-CSRF-TOKEN'], 'csrf-token');
    assert.deepEqual(JSON.parse(capturedOptions.body), { serviceAreaCode: 'A00001' });
    assert.equal(result.status, 'success');
    assert.equal(result.restroom.restStopServiceAreaCode, 'A00001');
});

test('linkRestroom은 404면 not-found 상태를 낸다', async () => {
    const result = await linkRestroom('99', 'A00001', csrf, async () => jsonResponse(404, { code: 'NOT_FOUND' }));

    assert.equal(result.status, 'not-found');
});

test('unlinkRestroom은 DELETE .../link를 호출한다', async () => {
    let capturedUrl;
    let capturedOptions;
    const result = await unlinkRestroom('1', csrf, async (url, options) => {
        capturedUrl = url;
        capturedOptions = options;
        return jsonResponse(200, { code: 'SUCCESS', data: { id: 1, restStopServiceAreaCode: '' } });
    });

    assert.equal(capturedUrl, '/api/admin/rest-stop-restrooms/1/link');
    assert.equal(capturedOptions.method, 'DELETE');
    assert.equal(result.status, 'success');
});

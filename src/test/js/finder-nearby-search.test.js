import assert from 'node:assert/strict';
import test from 'node:test';

import { describeNearbyError } from '../../main/resources/static/js/finder-nearby-search.js';

test('describeNearbyError shows the HTTP status code for http failures', () => {
    assert.equal(
        describeNearbyError({ status: 'error', reason: 'http', httpStatus: 500 }),
        '서버에 문제가 생겼어요. (상태 코드 500)'
    );
});

test('describeNearbyError shows the API code for api failures', () => {
    assert.equal(
        describeNearbyError({ status: 'error', reason: 'api', apiCode: 'BAD_REQUEST' }),
        '요청을 처리하지 못했어요. (코드: BAD_REQUEST)'
    );
});

test('describeNearbyError falls back to a generic code label when the api reason has none', () => {
    assert.equal(
        describeNearbyError({ status: 'error', reason: 'api' }),
        '요청을 처리하지 못했어요. (코드: 알 수 없음)'
    );
});

test('describeNearbyError distinguishes network/invalid-response/unexpected-shape reasons', () => {
    assert.equal(describeNearbyError({ status: 'error', reason: 'network' }), '네트워크 연결을 확인해주세요.');
    assert.equal(
        describeNearbyError({ status: 'error', reason: 'invalid-response' }),
        '서버 응답을 처리하지 못했어요.'
    );
    assert.equal(
        describeNearbyError({ status: 'error', reason: 'unexpected-shape' }),
        '예상과 다른 응답을 받았어요.'
    );
});

test('describeNearbyError falls back to a generic message for an unknown or missing reason', () => {
    assert.equal(describeNearbyError({ status: 'error' }), '휴게소 목록을 불러오지 못했어요.');
    assert.equal(describeNearbyError({ status: 'error', reason: 'mystery' }), '휴게소 목록을 불러오지 못했어요.');
});

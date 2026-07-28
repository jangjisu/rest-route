import assert from 'node:assert/strict';
import test from 'node:test';

import { createRestStopDetailRequest } from '../../main/resources/static/js/rest-stop-detail-request.js';

function deferred() {
    let resolve;
    let reject;
    const promise = new Promise((promiseResolve, promiseReject) => {
        resolve = promiseResolve;
        reject = promiseReject;
    });
    return { promise, reject, resolve };
}

function response({ ok = true, status = 200, body, jsonError } = {}) {
    return {
        ok,
        status,
        json: jsonError
            ? async () => {
                throw jsonError;
            }
            : async () => body
    };
}

function successSectionResponse(data = {}) {
    return response({ body: { code: 'SUCCESS', data } });
}

function csrfDocument({ token = 'csrf-token', headerName = 'X-CSRF-TOKEN' } = {}) {
    const contentBySelector = {
        'meta[name="_csrf"]': token,
        'meta[name="_csrf_header"]': headerName
    };

    return {
        querySelector: (selector) => {
            const content = contentBySelector[selector];
            return content ? { content } : null;
        }
    };
}

function sectionResponseFor(url, sections = {}) {
    if (url.endsWith('/basic-info')) {
        return sections.basicInfo ?? successSectionResponse({ unitName: '휴게소' });
    }

    if (url.endsWith('/facilities')) {
        return sections.facilities ?? successSectionResponse({});
    }

    if (url.endsWith('/oil-info')) {
        return sections.oilInfo ?? successSectionResponse({});
    }

    if (url.endsWith('/foods')) {
        return sections.foodMenu ?? successSectionResponse({ menus: [], sections: [] });
    }

    if (url.endsWith('/sales-rankings')) {
        return sections.salesRanking ?? response({
            ok: false,
            status: 404,
            body: { code: 'NOT_FOUND', data: null }
        });
    }

    if (url.endsWith('/events')) {
        return sections.events ?? successSectionResponse({ events: [] });
    }

    return response({
        ok: false,
        status: 404,
        body: { code: 'NOT_FOUND', data: null }
    });
}

test('a later selection ignores an earlier successful response', async () => {
    const first = deferred();
    const second = deferred();
    const states = [];
    const signals = [];
    const fetchImpl = (url, { signal }) => {
        signals.push(signal);
        if (url.includes('/FIRST/basic-info')) {
            return first.promise;
        }

        if (url.includes('/SECOND/basic-info')) {
            return second.promise;
        }

        return Promise.resolve(sectionResponseFor(url));
    };
    const request = createRestStopDetailRequest({ fetchImpl, onState: (state) => states.push(state) });

    const firstLoad = request.load('FIRST');
    const secondLoad = request.load('SECOND');
    second.resolve(successSectionResponse({ unitName: '두 번째 상세', evChargerCount: 4 }));
    await secondLoad;
    first.resolve(successSectionResponse({ unitName: '첫 번째 상세' }));
    await firstLoad;

    assert.deepEqual(states, [
        { status: 'loading' },
        { status: 'loading' },
        {
            status: 'success',
            data: {
                unitName: '두 번째 상세',
                evChargerCount: 4,
                oilInfo: {},
                foodMenu: { menus: [], sections: [] },
                events: []
            }
        }
    ]);
    assert.equal(signals.slice(0, 6).every((signal) => signal.aborted), true);
    assert.equal(signals.slice(6).every((signal) => !signal.aborted), true);
});

test('loads sales rankings as an optional detail feature', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: (url) => Promise.resolve(sectionResponseFor(url, {
            basicInfo: successSectionResponse({ unitName: '휴게소' }),
            salesRanking: successSectionResponse({
                baseYearMonth: '2026-06',
                storeRankings: [{ rank: 1, storeName: 'CU편의점' }],
                products: [{ rank: 1, productName: '대표 메뉴' }]
            })
        })),
        onState: (state) => states.push(state)
    });

    await request.load('A00001');

    assert.deepEqual(states.at(-1), {
        status: 'success',
        data: {
            unitName: '휴게소',
            oilInfo: {},
            foodMenu: { menus: [], sections: [] },
            events: [],
            salesRanking: {
                baseYearMonth: '2026-06',
                storeRankings: [{ rank: 1, storeName: 'CU편의점' }],
                products: [{ rank: 1, productName: '대표 메뉴' }]
            }
        }
    });
});

test('a later selection ignores an earlier request error', async () => {
    const first = deferred();
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: (url) => {
            if (url.includes('/FIRST/basic-info')) {
                return first.promise;
            }

            return Promise.resolve(sectionResponseFor(url, {
                basicInfo: successSectionResponse({ unitName: '두 번째 상세' })
            }));
        },
        onState: (state) => states.push(state)
    });

    const firstLoad = request.load('FIRST');
    await request.load('SECOND');
    first.reject(new Error('network failed'));
    await firstLoad;

    assert.deepEqual(states.at(-1), {
        status: 'success',
        data: {
            unitName: '두 번째 상세',
            oilInfo: {},
            foodMenu: { menus: [], sections: [] },
            events: []
        }
    });
    assert.equal(states.some((state) => state.status === 'error'), false);
});

test('invalidate ignores a response that completes after the panel closes', async () => {
    const pending = deferred();
    const states = [];
    let requestSignal;
    const request = createRestStopDetailRequest({
        fetchImpl: (url, { signal }) => {
            if (url.endsWith('/basic-info')) {
                requestSignal = signal;
                return pending.promise;
            }

            return Promise.resolve(sectionResponseFor(url));
        },
        onState: (state) => states.push(state)
    });

    const load = request.load('A00001');
    request.invalidate();
    pending.resolve(successSectionResponse({ unitName: '상세' }));
    await load;

    assert.deepEqual(states, [
        { status: 'loading' }
    ]);
    assert.equal(requestSignal.aborted, true);
});

test('404 with NOT_FOUND emits the not-found state', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async () => response({
            ok: false,
            status: 404,
            body: { code: 'NOT_FOUND', data: null }
        }),
        onState: (state) => states.push(state)
    });

    await request.load('UNKNOWN');

    assert.deepEqual(states.at(-1), { status: 'not-found' });
});

test('200 with NOT_FOUND emits the error state', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async () => response({
            body: { code: 'NOT_FOUND', data: null }
        }),
        onState: (state) => states.push(state)
    });

    await request.load('UNKNOWN');

    assert.deepEqual(states.at(-1), { status: 'error' });
});

test('404 with SUCCESS emits the error state', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async () => response({
            ok: false,
            status: 404,
            body: { code: 'SUCCESS', data: { unitName: '휴게소' } }
        }),
        onState: (state) => states.push(state)
    });

    await request.load('A00001');

    assert.deepEqual(states.at(-1), { status: 'error' });
});

test('404 with an unexpected code emits the error state', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async () => response({
            ok: false,
            status: 404,
            body: { code: 'INVALID_REQUEST', data: null }
        }),
        onState: (state) => states.push(state)
    });

    await request.load('A00001');

    assert.deepEqual(states.at(-1), { status: 'error' });
});

test('a 500 response emits the error state', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async () => response({
            ok: false,
            status: 500,
            body: { code: 'INTERNAL_SERVER_ERROR', data: null }
        }),
        onState: (state) => states.push(state)
    });

    await request.load('A00001');

    assert.deepEqual(states.at(-1), { status: 'error' });
});

test('EXTERNAL_API_UNAVAILABLE emits a dedicated state', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async () => response({
            ok: false,
            status: 503,
            body: { code: 'EXTERNAL_API_UNAVAILABLE', data: null }
        }),
        onState: (state) => states.push(state)
    });

    await request.load('A00001');

    assert.deepEqual(states.at(-1), { status: 'external-unavailable' });
});

test('a fetch failure emits the error state', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async () => {
            throw new Error('network failed');
        },
        onState: (state) => states.push(state)
    });

    await request.load('A00001');

    assert.deepEqual(states.at(-1), { status: 'error' });
});

test('invalid JSON emits the error state', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async () => response({ jsonError: new SyntaxError('invalid JSON') }),
        onState: (state) => states.push(state)
    });

    await request.load('A00001');

    assert.deepEqual(states.at(-1), { status: 'error' });
});

test('SUCCESS with null data emits the error state', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async () => response({ body: { code: 'SUCCESS', data: null } }),
        onState: (state) => states.push(state)
    });

    await request.load('A00001');

    assert.deepEqual(states.at(-1), { status: 'error' });
});

test('an empty serviceAreaCode does not fetch and emits the error state', async () => {
    let fetchCount = 0;
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async () => {
            fetchCount += 1;
            return response();
        },
        onState: (state) => states.push(state)
    });

    await request.load('   ');

    assert.equal(fetchCount, 0);
    assert.deepEqual(states, [{ status: 'error' }]);
});

test('load fetches feature detail APIs with a trimmed serviceAreaCode', async () => {
    const requestedUrls = [];
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async (url) => {
            requestedUrls.push(url);
            return sectionResponseFor(url, {
                basicInfo: successSectionResponse({
                    unitName: '휴게소',
                    routeName: '경부선',
                    address: '주소'
                }),
                facilities: successSectionResponse({
                    direction: '부산',
                    compactCarParkingCount: 3
                }),
                oilInfo: successSectionResponse({
                    gasolinePrice: '1,699원'
                }),
                foodMenu: successSectionResponse({
                    menus: [{ foodName: '돈가스' }],
                    sections: []
                }),
                events: successSectionResponse({
                    events: [{ name: 'TEN+1 이벤트', detail: '한식당 식사 10번 이용 시 1번 무료', period: '2026.01.01 ~ 2026.12.31' }]
                })
            });
        },
        onState: (state) => states.push(state)
    });

    await request.load('  A00001  ');

    assert.deepEqual(requestedUrls, [
        '/api/rest-stops/A00001/basic-info',
        '/api/rest-stops/A00001/facilities',
        '/api/rest-stops/A00001/oil-info',
        '/api/rest-stops/A00001/foods',
        '/api/rest-stops/A00001/sales-rankings',
        '/api/rest-stops/A00001/events'
    ]);
    assert.deepEqual(states.at(-1), {
        status: 'success',
        data: {
            unitName: '휴게소',
            routeName: '경부선',
            address: '주소',
            direction: '부산',
            compactCarParkingCount: 3,
            oilInfo: {
                gasolinePrice: '1,699원'
            },
            foodMenu: {
                menus: [{ foodName: '돈가스' }],
                sections: []
            },
            events: [{ name: 'TEN+1 이벤트', detail: '한식당 식사 10번 이용 시 1번 무료', period: '2026.01.01 ~ 2026.12.31' }]
        }
    });
});

test('optional feature API failures keep the basic detail response visible', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async (url) => sectionResponseFor(url, {
            basicInfo: successSectionResponse({ unitName: '휴게소' }),
            facilities: response({
                ok: false,
                status: 500,
                body: { code: 'INTERNAL_SERVER_ERROR', data: null }
            }),
            oilInfo: response({
                ok: false,
                status: 404,
                body: { code: 'NOT_FOUND', data: null }
            }),
            foodMenu: response({ jsonError: new SyntaxError('invalid JSON') })
        }),
        onState: (state) => states.push(state)
    });

    await request.load('A00001');

    assert.deepEqual(states.at(-1), {
        status: 'success',
        data: {
            unitName: '휴게소',
            oilInfo: null,
            foodMenu: { menus: [], sections: [] },
            events: []
        }
    });
});

test('optional EXTERNAL_API_UNAVAILABLE is reported with the successful detail response', async () => {
    const states = [];
    const request = createRestStopDetailRequest({
        fetchImpl: async (url) => sectionResponseFor(url, {
            basicInfo: successSectionResponse({ unitName: '휴게소' }),
            oilInfo: response({
                ok: false,
                status: 503,
                body: { code: 'EXTERNAL_API_UNAVAILABLE', data: null }
            })
        }),
        onState: (state) => states.push(state)
    });

    await request.load('A00001');

    assert.deepEqual(states.at(-1), {
        status: 'success',
        externalUnavailable: true,
        data: {
            unitName: '휴게소',
            oilInfo: null,
            foodMenu: { menus: [], sections: [] },
            events: []
        }
    });
});

test('refreshOilPrice posts to the refresh endpoint and returns oil info', async () => {
    let requestedUrl;
    let requestedOptions;
    const request = createRestStopDetailRequest({
        document: csrfDocument({ token: 'request-token', headerName: 'X-REST-ROUTE-CSRF' }),
        fetchImpl: async (url, options) => {
            requestedUrl = url;
            requestedOptions = options;
            return response({
                body: {
                    code: 'SUCCESS',
                    data: {
                        gasolinePrice: '1,699원',
                        dieselPrice: '1,529원'
                    }
                }
            });
        }
    });

    const result = await request.refreshOilPrice(' A00001 ');

    assert.equal(requestedUrl, '/api/rest-stops/A00001/oil-price/refresh');
    assert.equal(requestedOptions.method, 'POST');
    assert.deepEqual(requestedOptions.headers, { 'X-REST-ROUTE-CSRF': 'request-token' });
    assert.deepEqual(result, {
        status: 'success',
        data: {
            gasolinePrice: '1,699원',
            dieselPrice: '1,529원'
        }
    });
});

test('refreshOilPrice encodes serviceAreaCode before building the request URL', async () => {
    let requestedUrl;
    const request = createRestStopDetailRequest({
        document: csrfDocument(),
        fetchImpl: async (url) => {
            requestedUrl = url;
            return response({ body: { code: 'SUCCESS', data: {} } });
        }
    });

    await request.refreshOilPrice('A 0001');

    assert.equal(requestedUrl, '/api/rest-stops/A%200001/oil-price/refresh');
});

test('refreshOilPrice returns not-found for 404 NOT_FOUND', async () => {
    const request = createRestStopDetailRequest({
        document: csrfDocument(),
        fetchImpl: async () => response({
            ok: false,
            status: 404,
            body: { code: 'NOT_FOUND', data: null }
        })
    });

    const result = await request.refreshOilPrice('UNKNOWN');

    assert.deepEqual(result, { status: 'not-found' });
});

test('refreshOilPrice returns external-unavailable for EXTERNAL_API_UNAVAILABLE', async () => {
    const request = createRestStopDetailRequest({
        document: csrfDocument(),
        fetchImpl: async () => response({
            ok: false,
            status: 503,
            body: { code: 'EXTERNAL_API_UNAVAILABLE', data: null }
        })
    });

    const result = await request.refreshOilPrice('A00001');

    assert.deepEqual(result, { status: 'external-unavailable' });
});

test('refreshOilPrice returns error for empty serviceAreaCode without fetching', async () => {
    let fetchCount = 0;
    const request = createRestStopDetailRequest({
        fetchImpl: async () => {
            fetchCount += 1;
            return response();
        }
    });

    const result = await request.refreshOilPrice('   ');

    assert.equal(fetchCount, 0);
    assert.deepEqual(result, { status: 'error' });
});

test('refreshOilPrice returns error without fetching when CSRF metadata is missing', async () => {
    let fetchCount = 0;
    const request = createRestStopDetailRequest({
        document: csrfDocument({ token: '' }),
        fetchImpl: async () => {
            fetchCount += 1;
            return response();
        }
    });

    const result = await request.refreshOilPrice('A00001');

    assert.equal(fetchCount, 0);
    assert.deepEqual(result, { status: 'error' });
});

test('load does not read CSRF metadata for GET requests', async () => {
    const request = createRestStopDetailRequest({
        document: {
            querySelector: () => {
                throw new Error('GET requests must not read CSRF metadata');
            }
        },
        fetchImpl: async (url) => sectionResponseFor(url)
    });

    await request.load('A00001');
});

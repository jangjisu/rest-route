import assert from 'node:assert/strict';
import test from 'node:test';

import { initializeAdminRestOilLink } from '../../main/resources/static/js/admin-rest-oil-link.js';

function interactiveElement(initial = {}) {
    return {
        disabled: false,
        hidden: false,
        open: false,
        textContent: '',
        value: '',
        className: '',
        dataset: {},
        children: [],
        handlers: {},
        addEventListener(event, handler) {
            this.handlers[event] = handler;
        },
        appendChild(child) {
            this.children.push(child);
            return child;
        },
        replaceChildren(...children) {
            this.children = children;
        },
        showModal() {
            this.open = true;
        },
        close() {
            this.open = false;
        },
        setAttribute(name, value) {
            this[name] = value;
        },
        ...initial
    };
}

function oilLinkDocument() {
    const csrfInput = { value: 'csrf-token' };
    const csrfSource = interactiveElement({
        dataset: { csrfHeader: 'X-CSRF-TOKEN' },
        querySelector: () => csrfInput
    });
    const elements = new Map([
        ['oilLinkCsrfSource', csrfSource],
        ['oilLinkStatus', interactiveElement()],
        ['oilLinkTotalCount', interactiveElement()],
        ['oilLinkMissingCount', interactiveElement()],
        ['oilLinkSearchInput', interactiveElement()],
        ['oilLinkMissingOnlyButton', interactiveElement()],
        ['oilLinkTableBody', interactiveElement()],
        ['oilLinkModal', interactiveElement()],
        ['oilLinkModalClose', interactiveElement()],
        ['oilLinkModalTitle', interactiveElement()],
        ['oilLinkCurrentList', interactiveElement()],
        ['oilLinkSearchQuery', interactiveElement()],
        ['oilLinkSearchRoute', interactiveElement()],
        ['oilLinkSearchResults', interactiveElement()]
    ]);
    return {
        createElement: () => interactiveElement(),
        getElementById: (id) => elements.get(id),
        elements
    };
}

async function flushPromises() {
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
}

function oilStation(overrides = {}) {
    return {
        id: 1,
        standardRestName: 'SK에너지 마장주유소',
        routeName: '중부내륙선',
        serviceAreaAddress: '충북 음성군',
        direction: '서울',
        adminOverridden: false,
        ...overrides
    };
}

function restStop(overrides = {}) {
    return {
        serviceAreaCode: 'A00099',
        unitName: '마장휴게소',
        routeName: '중부내륙선',
        linkedOilStation: null,
        ...overrides
    };
}

function linksResponse(restStops) {
    return { ok: true, json: async () => ({ code: 'SUCCESS', data: restStops }) };
}

async function initAndLoad(document, fetchImpl, onNotice = () => {}, confirmImpl = () => true) {
    initializeAdminRestOilLink(document, { fetchImpl, onNotice, confirmImpl, debounceMs: 0 });
    await flushPromises();
}

test('로드하면 요약 카드와 표를 채운다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async () =>
        linksResponse([restStop(), restStop({ serviceAreaCode: 'A00001', unitName: '서울만남(부산)휴게소', linkedOilStation: oilStation({ standardRestName: 'SK에너지 서울만남주유소' }) })]);

    await initAndLoad(document, fetchImpl);

    assert.equal(document.elements.get('oilLinkTotalCount').textContent, '2');
    assert.equal(document.elements.get('oilLinkMissingCount').textContent, '1');
    assert.equal(document.elements.get('oilLinkTableBody').children.length, 2);
});

test('연결 없는 휴게소 행은 연결없음 배지를 보여준다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async () => linksResponse([restStop()]);

    await initAndLoad(document, fetchImpl);

    const row = document.elements.get('oilLinkTableBody').children[0];
    const badgeCell = row.children[3];
    const badge = badgeCell.children[0];
    assert.equal(badge.dataset.state, 'missing');
});

test('관리자가 잠근 연결은 배지에 잠금 표시가 붙는다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async () =>
        linksResponse([restStop({ linkedOilStation: oilStation({ standardRestName: 'SK에너지', adminOverridden: true }) })]);

    await initAndLoad(document, fetchImpl);

    const row = document.elements.get('oilLinkTableBody').children[0];
    const badge = row.children[3].children[0];
    assert.equal(badge.dataset.state, 'linked');
    assert.match(badge.textContent, /🔒/);
});

test('연결없음만 필터를 켜면 연결된 행이 숨는다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async () =>
        linksResponse([restStop(), restStop({ serviceAreaCode: 'A00001', linkedOilStation: oilStation({ standardRestName: 'SK' }) })]);

    await initAndLoad(document, fetchImpl);
    await document.elements.get('oilLinkMissingOnlyButton').handlers.click();

    assert.equal(document.elements.get('oilLinkTableBody').children.length, 1);
});

test('이름으로 표를 필터링한다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async () =>
        linksResponse([restStop(), restStop({ serviceAreaCode: 'A00001', unitName: '서울만남(부산)휴게소' })]);

    await initAndLoad(document, fetchImpl);
    const searchInput = document.elements.get('oilLinkSearchInput');
    searchInput.value = '서울만남';
    await searchInput.handlers.input();

    assert.equal(document.elements.get('oilLinkTableBody').children.length, 1);
});

test('관리 버튼을 누르면 모달이 열리고 현재 연결 목록을 보여준다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async () =>
        linksResponse([restStop({ linkedOilStation: oilStation({ standardRestName: 'SK에너지 마장주유소' }) })]);

    await initAndLoad(document, fetchImpl);
    const row = document.elements.get('oilLinkTableBody').children[0];
    const manageButton = row.children[4].children[0];
    await manageButton.handlers.click();

    assert.equal(document.elements.get('oilLinkModal').open, true);
    assert.equal(document.elements.get('oilLinkModalTitle').textContent, '마장휴게소');
    const currentList = document.elements.get('oilLinkCurrentList');
    assert.equal(currentList.children.length, 1);
    assert.equal(currentList.children[0].children[0].textContent, 'SK에너지 마장주유소');
    assert.equal(currentList.children[0].children[1].textContent, '중부내륙선 · 충북 음성군 · 서울 방향');
});

test('연결이 없으면 모달에 빈 상태 문구를 보여준다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async () => linksResponse([restStop()]);

    await initAndLoad(document, fetchImpl);
    const row = document.elements.get('oilLinkTableBody').children[0];
    await row.children[4].children[0].handlers.click();

    const currentList = document.elements.get('oilLinkCurrentList');
    assert.equal(currentList.children.length, 1);
    assert.equal(currentList.children[0].className, 'oil-link-current-empty');
});

test('관리자가 잠근 연결에만 자동 매칭으로 되돌리기 버튼이 보인다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async () =>
        linksResponse([restStop({ linkedOilStation: oilStation({ standardRestName: 'SK', adminOverridden: true }) })]);

    await initAndLoad(document, fetchImpl);
    const row = document.elements.get('oilLinkTableBody').children[0];
    await row.children[4].children[0].handlers.click();

    const item = document.elements.get('oilLinkCurrentList').children[0];
    assert.equal(item.children.length, 4);
});

test('모달 닫기 버튼은 그냥 닫는다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async () => linksResponse([restStop()]);

    await initAndLoad(document, fetchImpl);
    const row = document.elements.get('oilLinkTableBody').children[0];
    await row.children[4].children[0].handlers.click();
    await document.elements.get('oilLinkModalClose').handlers.click();

    assert.equal(document.elements.get('oilLinkModal').open, false);
});

test('연결 해제 버튼을 누르면 해제하고 목록과 모달을 새로고침한다', async () => {
    const document = oilLinkDocument();
    let unlinkedUrl;
    let foods = [restStop({ linkedOilStation: oilStation({ standardRestName: 'SK' }) })];
    const fetchImpl = async (url, options = {}) => {
        if (url === '/api/admin/rest-stops/oil-links') {
            return linksResponse(foods);
        }
        if (url === '/api/admin/oil-stations/1/link' && options.method === 'DELETE') {
            unlinkedUrl = url;
            foods = [restStop({ linkedOilStation: null })];
            return { ok: true, json: async () => ({ code: 'SUCCESS', data: { id: 1, restStopServiceAreaCode: null } }) };
        }
        throw new Error(`Unexpected request: ${url} ${options.method}`);
    };
    const notices = [];

    await initAndLoad(document, fetchImpl, (message, type) => notices.push({ message, type }));
    const row = document.elements.get('oilLinkTableBody').children[0];
    await row.children[4].children[0].handlers.click();
    const unlinkButton = document.elements.get('oilLinkCurrentList').children[0].children[2];
    await unlinkButton.handlers.click();

    assert.equal(unlinkedUrl, '/api/admin/oil-stations/1/link');
    assert.deepEqual(notices.at(-1), { message: '연결을 해제했습니다.', type: undefined });
    assert.equal(document.elements.get('oilLinkCurrentList').children[0].className, 'oil-link-current-empty');
    assert.equal(document.elements.get('oilLinkMissingCount').textContent, '1');
});

test('검색어를 입력하면 검색 후 결과에 연결 버튼을 보여준다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async (url) => {
        if (url === '/api/admin/rest-stops/oil-links') {
            return linksResponse([restStop()]);
        }
        if (url.startsWith('/api/admin/oil-stations/search')) {
            return {
                ok: true,
                json: async () => ({
                    code: 'SUCCESS',
                    data: [{
                        id: 5,
                        standardRestName: 'SK에너지 마장주유소',
                        routeName: '중부내륙선',
                        serviceAreaAddress: '충북 음성군',
                        direction: '서울',
                        linkedRestStopName: null
                    }]
                })
            };
        }
        throw new Error(`Unexpected request: ${url}`);
    };

    await initAndLoad(document, fetchImpl);
    const row = document.elements.get('oilLinkTableBody').children[0];
    await row.children[4].children[0].handlers.click();
    const queryInput = document.elements.get('oilLinkSearchQuery');
    queryInput.value = '마장';
    await queryInput.handlers.input();
    await flushPromises();

    const results = document.elements.get('oilLinkSearchResults');
    assert.equal(results.children.length, 1);
    assert.equal(results.children[0].children[0].textContent, 'SK에너지 마장주유소');
    assert.equal(results.children[0].children[1].textContent, '중부내륙선 · 충북 음성군 · 서울 방향');
});

test('검색 결과에 연결 버튼을 누르면 연결하고 모달을 새로고침한다', async () => {
    const document = oilLinkDocument();
    let linkedBody;
    let foods = [restStop()];
    const fetchImpl = async (url, options = {}) => {
        if (url === '/api/admin/rest-stops/oil-links') {
            return linksResponse(foods);
        }
        if (url.startsWith('/api/admin/oil-stations/search')) {
            return {
                ok: true,
                json: async () => ({
                    code: 'SUCCESS',
                    data: [{ id: 5, standardRestName: 'SK에너지 마장주유소', linkedRestStopName: null }]
                })
            };
        }
        if (url === '/api/admin/oil-stations/5/link' && options.method === 'PUT') {
            linkedBody = JSON.parse(options.body);
            foods = [restStop({ linkedOilStation: oilStation({ id: 5, standardRestName: 'SK에너지 마장주유소', adminOverridden: true }) })];
            return {
                ok: true,
                json: async () => ({
                    code: 'SUCCESS',
                    data: { id: 5, standardRestName: 'SK에너지 마장주유소', restStopServiceAreaCode: 'A00099' }
                })
            };
        }
        throw new Error(`Unexpected request: ${url} ${options.method}`);
    };
    const notices = [];

    await initAndLoad(document, fetchImpl, (message, type) => notices.push({ message, type }));
    const row = document.elements.get('oilLinkTableBody').children[0];
    await row.children[4].children[0].handlers.click();
    const queryInput = document.elements.get('oilLinkSearchQuery');
    queryInput.value = '마장';
    await queryInput.handlers.input();
    await flushPromises();
    const linkButton = document.elements.get('oilLinkSearchResults').children[0].children[2];
    await linkButton.handlers.click();

    assert.deepEqual(linkedBody, { serviceAreaCode: 'A00099' });
    assert.deepEqual(notices.at(-1), { message: '연결했습니다.', type: undefined });
    assert.equal(document.elements.get('oilLinkCurrentList').children.length, 1);
});

test('노선 select에 전체 휴게소의 노선명이 중복 없이 채워진다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async () =>
        linksResponse([
            restStop({ routeName: '경부선' }),
            restStop({ serviceAreaCode: 'A00001', routeName: '중부내륙선' }),
            restStop({ serviceAreaCode: 'A00002', routeName: '경부선' })
        ]);

    await initAndLoad(document, fetchImpl);

    const routeSelect = document.elements.get('oilLinkSearchRoute');
    assert.deepEqual(
        routeSelect.children.map((option) => option.value),
        ['', '경부선', '중부내륙선']
    );
});

test('관리 버튼을 누르면 해당 휴게소의 노선이 검색 select에 미리 선택되고 자동으로 검색한다', async () => {
    const document = oilLinkDocument();
    let requestedUrl;
    const fetchImpl = async (url) => {
        if (url === '/api/admin/rest-stops/oil-links') {
            return linksResponse([restStop({ routeName: '경부선' })]);
        }
        if (url.startsWith('/api/admin/oil-stations/search')) {
            requestedUrl = url;
            return { ok: true, json: async () => ({ code: 'SUCCESS', data: [] }) };
        }
        throw new Error(`Unexpected request: ${url}`);
    };

    await initAndLoad(document, fetchImpl);
    const row = document.elements.get('oilLinkTableBody').children[0];
    await row.children[4].children[0].handlers.click();
    await flushPromises();

    assert.equal(document.elements.get('oilLinkSearchRoute').value, '경부선');
    assert.equal(requestedUrl, '/api/admin/oil-stations/search?routeName=%EA%B2%BD%EB%B6%80%EC%84%A0');
});

test('노선을 바꾸면 이름 없이도 그 노선 기준으로 다시 검색한다', async () => {
    const document = oilLinkDocument();
    let requestedUrl;
    const fetchImpl = async (url) => {
        if (url === '/api/admin/rest-stops/oil-links') {
            return linksResponse([restStop({ routeName: '경부선' })]);
        }
        if (url.startsWith('/api/admin/oil-stations/search')) {
            requestedUrl = url;
            return { ok: true, json: async () => ({ code: 'SUCCESS', data: [] }) };
        }
        throw new Error(`Unexpected request: ${url}`);
    };

    await initAndLoad(document, fetchImpl);
    const row = document.elements.get('oilLinkTableBody').children[0];
    await row.children[4].children[0].handlers.click();
    await flushPromises();
    const routeSelect = document.elements.get('oilLinkSearchRoute');
    routeSelect.value = '중부내륙선';
    await routeSelect.handlers.change();

    assert.equal(requestedUrl, '/api/admin/oil-stations/search?routeName=%EC%A4%91%EB%B6%80%EB%82%B4%EB%A5%99%EC%84%A0');
});

test('이름과 노선을 함께 지정하면 검색 요청에 둘 다 포함된다', async () => {
    const document = oilLinkDocument();
    let requestedUrl;
    const fetchImpl = async (url) => {
        if (url === '/api/admin/rest-stops/oil-links') {
            return linksResponse([restStop({ routeName: '경부선' })]);
        }
        if (url.startsWith('/api/admin/oil-stations/search')) {
            requestedUrl = url;
            return { ok: true, json: async () => ({ code: 'SUCCESS', data: [] }) };
        }
        throw new Error(`Unexpected request: ${url}`);
    };

    await initAndLoad(document, fetchImpl);
    const row = document.elements.get('oilLinkTableBody').children[0];
    await row.children[4].children[0].handlers.click();
    await flushPromises();
    const queryInput = document.elements.get('oilLinkSearchQuery');
    queryInput.value = '마장';
    await queryInput.handlers.input();

    assert.equal(
        requestedUrl,
        '/api/admin/oil-stations/search?name=%EB%A7%88%EC%9E%A5&routeName=%EA%B2%BD%EB%B6%80%EC%84%A0'
    );
});

test('이미 다른 휴게소에 연결된 검색 결과는 안내 문구를 함께 보여준다', async () => {
    const document = oilLinkDocument();
    const fetchImpl = async (url) => {
        if (url === '/api/admin/rest-stops/oil-links') {
            return linksResponse([restStop()]);
        }
        if (url.startsWith('/api/admin/oil-stations/search')) {
            return {
                ok: true,
                json: async () => ({
                    code: 'SUCCESS',
                    data: [{ id: 5, standardRestName: 'SK에너지 마장주유소', linkedRestStopName: '현풍(대구)휴게소' }]
                })
            };
        }
        throw new Error(`Unexpected request: ${url}`);
    };

    await initAndLoad(document, fetchImpl);
    const row = document.elements.get('oilLinkTableBody').children[0];
    await row.children[4].children[0].handlers.click();
    const queryInput = document.elements.get('oilLinkSearchQuery');
    queryInput.value = '마장';
    await queryInput.handlers.input();
    await flushPromises();

    const result = document.elements.get('oilLinkSearchResults').children[0];
    assert.match(result.children[2].textContent, /현풍\(대구\)휴게소/);
});

import assert from 'node:assert/strict';
import test from 'node:test';

import { initializeAdminRestStopEdit } from '../../main/resources/static/js/admin-rest-stop-edit.js';

function interactiveElement(initial = {}) {
    return {
        disabled: false,
        hidden: false,
        textContent: '',
        value: '',
        checked: false,
        dataset: {},
        handlers: {},
        addEventListener(event, handler) {
            this.handlers[event] = handler;
        },
        setAttribute(name, value) {
            this[name] = value;
        },
        ...initial
    };
}

function editDocument() {
    const csrfInput = { value: 'csrf-token' };
    const form = interactiveElement({
        dataset: { csrfHeader: 'X-CSRF-TOKEN' },
        querySelector: () => csrfInput
    });
    const select = interactiveElement({
        appended: [],
        append(...options) {
            this.appended.push(...options);
        }
    });
    const elements = new Map([
        ['restStopEditForm', form],
        ['restStopEditSelect', select],
        ['restStopEditStatus', interactiveElement()],
        ['restStopEditLockBanner', interactiveElement({ hidden: true })],
        ['restStopEditLockIcon', interactiveElement()],
        ['restStopEditLockTitle', interactiveElement()],
        ['restStopEditLockDesc', interactiveElement()],
        ['restStopEditLockToggle', interactiveElement({ hidden: true })],
        ['restStopEditSubmit', interactiveElement()],
        ['restStopEditServiceAreaCode', interactiveElement()],
        ['restStopEditUnitCode', interactiveElement()],
        ['restStopEditUnitName', interactiveElement()],
        ['restStopEditRouteNo', interactiveElement()],
        ['restStopEditRouteName', interactiveElement()],
        ['restStopEditXValue', interactiveElement()],
        ['restStopEditYValue', interactiveElement()],
        ['restStopEditTelNo', interactiveElement()],
        ['restStopEditBrand', interactiveElement()],
        ['restStopEditRouteCode', interactiveElement()],
        ['restStopEditSvarAddr', interactiveElement()],
        ['restStopEditConvenience', interactiveElement()],
        ['restStopEditMaintenanceYn', interactiveElement()],
        ['restStopEditTruckSaYn', interactiveElement()],
        ['restStopEditCreateButton', interactiveElement()]
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
        convenience: '수유실 | 샤워실',
        maintenanceYn: 'O',
        truckSaYn: 'X',
        adminOverridden: false,
        ...overrides
    };
}

function restStopListResponse() {
    return {
        ok: true,
        json: async () => ({
            code: 'SUCCESS',
            data: [{ serviceAreaCode: 'A00001', unitName: '서울만남(부산)휴게소' }]
        })
    };
}

test('loads the rest stop picker on init', async () => {
    const document = editDocument();
    const fetchImpl = async () => restStopListResponse();

    initializeAdminRestStopEdit(document, { fetchImpl, onNotice: () => {} });
    await flushPromises();

    const select = document.elements.get('restStopEditSelect');
    assert.equal(select.appended.length, 1);
    assert.equal(select.disabled, false);
});

test('selecting a rest stop loads its editable fields and shows a synced lock banner', async () => {
    const document = editDocument();
    const fetchImpl = async (url) => {
        if (url === '/api/rest-stops') {
            return restStopListResponse();
        }
        if (url === '/api/admin/rest-stops/A00001/editable') {
            return { ok: true, status: 200, json: async () => ({ code: 'SUCCESS', data: sampleData() }) };
        }
        throw new Error(`Unexpected request: ${url}`);
    };

    initializeAdminRestStopEdit(document, { fetchImpl, onNotice: () => {} });
    await flushPromises();

    const select = document.elements.get('restStopEditSelect');
    select.value = 'A00001';
    await select.handlers.change();

    assert.equal(document.elements.get('restStopEditUnitName').value, '서울만남(부산)휴게소');
    assert.equal(document.elements.get('restStopEditServiceAreaCode').textContent, 'A00001');
    assert.equal(document.elements.get('restStopEditMaintenanceYn').checked, true);
    assert.equal(document.elements.get('restStopEditTruckSaYn').checked, false);
    assert.equal(document.elements.get('restStopEditForm').hidden, false);
    const banner = document.elements.get('restStopEditLockBanner');
    assert.equal(banner.hidden, false);
    assert.equal(banner.dataset.state, 'synced');
    assert.equal(document.elements.get('restStopEditLockToggle').hidden, true);
});

test('selecting a locked rest stop shows the locked banner with an unlock button', async () => {
    const document = editDocument();
    const fetchImpl = async (url) => {
        if (url === '/api/rest-stops') {
            return restStopListResponse();
        }
        if (url === '/api/admin/rest-stops/A00001/editable') {
            return {
                ok: true,
                status: 200,
                json: async () => ({ code: 'SUCCESS', data: sampleData({ adminOverridden: true }) })
            };
        }
        throw new Error(`Unexpected request: ${url}`);
    };

    initializeAdminRestStopEdit(document, { fetchImpl, onNotice: () => {} });
    await flushPromises();
    const select = document.elements.get('restStopEditSelect');
    select.value = 'A00001';
    await select.handlers.change();

    const banner = document.elements.get('restStopEditLockBanner');
    assert.equal(banner.dataset.state, 'locked');
    assert.equal(document.elements.get('restStopEditLockToggle').hidden, false);
});

test('saving the form sends the payload and updates the banner to locked', async () => {
    const document = editDocument();
    let savedRequest;
    const fetchImpl = async (url, options = {}) => {
        if (url === '/api/rest-stops') {
            return restStopListResponse();
        }
        if (url === '/api/admin/rest-stops/A00001/editable' && !options.method) {
            return { ok: true, status: 200, json: async () => ({ code: 'SUCCESS', data: sampleData() }) };
        }
        if (url === '/api/admin/rest-stops/A00001/editable' && options.method === 'PUT') {
            savedRequest = { url, options };
            return {
                ok: true,
                status: 200,
                json: async () => ({ code: 'SUCCESS', data: sampleData({ unitName: '수정된이름', adminOverridden: true }) })
            };
        }
        throw new Error(`Unexpected request: ${url} ${options.method}`);
    };
    const notices = [];

    initializeAdminRestStopEdit(document, { fetchImpl, onNotice: (message, type) => notices.push({ message, type }) });
    await flushPromises();
    const select = document.elements.get('restStopEditSelect');
    select.value = 'A00001';
    await select.handlers.change();
    document.elements.get('restStopEditUnitName').value = '수정된이름';

    await document.elements.get('restStopEditForm').handlers.submit({ preventDefault() {} });

    assert.equal(savedRequest.options.headers['X-CSRF-TOKEN'], 'csrf-token');
    assert.equal(JSON.parse(savedRequest.options.body).unitName, '수정된이름');
    assert.equal(document.elements.get('restStopEditLockBanner').dataset.state, 'locked');
    assert.deepEqual(notices.at(-1), { message: '휴게소 정보를 저장했습니다.', type: undefined });
});

test('save failure with invalid coordinate shows the server message as an error notice', async () => {
    const document = editDocument();
    const fetchImpl = async (url, options = {}) => {
        if (url === '/api/rest-stops') {
            return restStopListResponse();
        }
        if (url === '/api/admin/rest-stops/A00001/editable' && !options.method) {
            return { ok: true, status: 200, json: async () => ({ code: 'SUCCESS', data: sampleData() }) };
        }
        if (options.method === 'PUT') {
            return {
                ok: false,
                status: 400,
                json: async () => ({ code: 'INVALID_PARAMETER', message: 'Invalid coordinate value: 숫자아님' })
            };
        }
        throw new Error(`Unexpected request: ${url}`);
    };
    const notices = [];

    initializeAdminRestStopEdit(document, { fetchImpl, onNotice: (message, type) => notices.push({ message, type }) });
    await flushPromises();
    const select = document.elements.get('restStopEditSelect');
    select.value = 'A00001';
    await select.handlers.change();

    await document.elements.get('restStopEditForm').handlers.submit({ preventDefault() {} });

    assert.deepEqual(notices.at(-1), { message: 'Invalid coordinate value: 숫자아님', type: 'error' });
});

test('clicking unlock clears the override and shows the synced banner', async () => {
    const document = editDocument();
    let overrideCleared = false;
    const fetchImpl = async (url, options = {}) => {
        if (url === '/api/rest-stops') {
            return restStopListResponse();
        }
        if (url === '/api/admin/rest-stops/A00001/editable' && !options.method) {
            return {
                ok: true,
                status: 200,
                json: async () => ({ code: 'SUCCESS', data: sampleData({ adminOverridden: !overrideCleared }) })
            };
        }
        if (url === '/api/admin/rest-stops/A00001/editable/override' && options.method === 'DELETE') {
            overrideCleared = true;
            return {
                ok: true,
                status: 200,
                json: async () => ({ code: 'SUCCESS', data: sampleData({ adminOverridden: false }) })
            };
        }
        throw new Error(`Unexpected request: ${url}`);
    };
    const notices = [];

    initializeAdminRestStopEdit(document, { fetchImpl, onNotice: (message, type) => notices.push({ message, type }) });
    await flushPromises();
    const select = document.elements.get('restStopEditSelect');
    select.value = 'A00001';
    await select.handlers.change();
    assert.equal(document.elements.get('restStopEditLockBanner').dataset.state, 'locked');

    await document.elements.get('restStopEditLockToggle').handlers.click();

    assert.equal(document.elements.get('restStopEditLockBanner').dataset.state, 'synced');
    assert.deepEqual(notices.at(-1), { message: '동기화 잠금을 해제했습니다.', type: undefined });
});

test('selecting an unknown rest stop hides the form and shows a status message', async () => {
    const document = editDocument();
    const fetchImpl = async (url) => {
        if (url === '/api/rest-stops') {
            return restStopListResponse();
        }
        if (url === '/api/admin/rest-stops/A00001/editable') {
            return { ok: false, status: 404, json: async () => ({ code: 'NOT_FOUND' }) };
        }
        throw new Error(`Unexpected request: ${url}`);
    };

    initializeAdminRestStopEdit(document, { fetchImpl, onNotice: () => {} });
    await flushPromises();
    const select = document.elements.get('restStopEditSelect');
    select.value = 'A00001';
    await select.handlers.change();

    assert.equal(document.elements.get('restStopEditForm').hidden, true);
    assert.equal(document.elements.get('restStopEditStatus').textContent, '존재하지 않는 휴게소 코드입니다.');
});

test('clicking the create button opens an empty form in create mode', async () => {
    const document = editDocument();
    const fetchImpl = async () => restStopListResponse();

    initializeAdminRestStopEdit(document, { fetchImpl, onNotice: () => {} });
    await flushPromises();
    document.elements.get('restStopEditUnitName').value = '남은값';

    await document.elements.get('restStopEditCreateButton').handlers.click();

    assert.equal(document.elements.get('restStopEditForm').hidden, false);
    assert.equal(document.elements.get('restStopEditUnitName').value, '');
    assert.equal(document.elements.get('restStopEditLockBanner').hidden, true);
    assert.equal(document.elements.get('restStopEditSelect').value, '');
});

test('submitting the form in create mode posts a new rest stop and adds it to the picker', async () => {
    const document = editDocument();
    let createdRequest;
    const fetchImpl = async (url, options = {}) => {
        if (url === '/api/rest-stops') {
            return restStopListResponse();
        }
        if (url === '/api/admin/rest-stops' && options.method === 'POST') {
            createdRequest = { url, options };
            return {
                ok: true,
                status: 200,
                json: async () => ({
                    code: 'SUCCESS',
                    data: sampleData({ serviceAreaCode: 'ADMIN-1', unitName: '가평휴게소', adminOverridden: true })
                })
            };
        }
        throw new Error(`Unexpected request: ${url} ${options.method}`);
    };
    const notices = [];

    initializeAdminRestStopEdit(document, { fetchImpl, onNotice: (message, type) => notices.push({ message, type }) });
    await flushPromises();
    await document.elements.get('restStopEditCreateButton').handlers.click();
    document.elements.get('restStopEditUnitName').value = '가평휴게소';

    await document.elements.get('restStopEditForm').handlers.submit({ preventDefault() {} });

    assert.equal(createdRequest.options.headers['X-CSRF-TOKEN'], 'csrf-token');
    assert.equal(JSON.parse(createdRequest.options.body).unitName, '가평휴게소');
    const select = document.elements.get('restStopEditSelect');
    assert.equal(select.appended.length, 2);
    assert.equal(select.value, 'ADMIN-1');
    assert.equal(document.elements.get('restStopEditLockBanner').dataset.state, 'locked');
    assert.deepEqual(notices.at(-1), { message: '휴게소를 새로 등록했습니다.', type: undefined });
});

test('create mode with invalid coordinate shows the server message as an error notice', async () => {
    const document = editDocument();
    const fetchImpl = async (url, options = {}) => {
        if (url === '/api/rest-stops') {
            return restStopListResponse();
        }
        if (url === '/api/admin/rest-stops' && options.method === 'POST') {
            return {
                ok: false,
                status: 400,
                json: async () => ({ code: 'INVALID_PARAMETER', message: 'Invalid coordinate value: 숫자아님' })
            };
        }
        throw new Error(`Unexpected request: ${url}`);
    };
    const notices = [];

    initializeAdminRestStopEdit(document, { fetchImpl, onNotice: (message, type) => notices.push({ message, type }) });
    await flushPromises();
    await document.elements.get('restStopEditCreateButton').handlers.click();

    await document.elements.get('restStopEditForm').handlers.submit({ preventDefault() {} });

    assert.deepEqual(notices.at(-1), { message: 'Invalid coordinate value: 숫자아님', type: 'error' });
});

test('selecting an existing rest stop while in create mode cancels create mode', async () => {
    const document = editDocument();
    const fetchImpl = async (url) => {
        if (url === '/api/rest-stops') {
            return restStopListResponse();
        }
        if (url === '/api/admin/rest-stops/A00001/editable') {
            return { ok: true, status: 200, json: async () => ({ code: 'SUCCESS', data: sampleData() }) };
        }
        throw new Error(`Unexpected request: ${url}`);
    };

    initializeAdminRestStopEdit(document, { fetchImpl, onNotice: () => {} });
    await flushPromises();
    await document.elements.get('restStopEditCreateButton').handlers.click();
    const select = document.elements.get('restStopEditSelect');
    select.value = 'A00001';
    await select.handlers.change();

    assert.equal(document.elements.get('restStopEditUnitName').value, '서울만남(부산)휴게소');
    assert.equal(document.elements.get('restStopEditLockBanner').hidden, false);
});

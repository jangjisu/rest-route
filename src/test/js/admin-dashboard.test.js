import assert from 'node:assert/strict';
import test from 'node:test';

import {
    attachAdminForms,
    bindActivityModal,
    fetchAdminDashboard,
    renderDashboard,
    renderDashboardError
} from '../../main/resources/static/js/admin-dashboard.js';

function fakeElement(initial = {}) {
    return {
        textContent: '',
        children: [],
        handlers: {},
        open: false,
        appendChild(child) {
            this.children.push(child);
        },
        replaceChildren(...children) {
            this.children = children;
        },
        addEventListener(event, handler) {
            this.handlers[event] = handler;
        },
        showModal() {
            this.open = true;
        },
        close() {
            this.open = false;
        },
        ...initial
    };
}

function documentWithDashboardElements() {
    const elements = new Map([
        ['restStopCount', fakeElement()],
        ['latestSalesRankingMonth', fakeElement()],
        ['lastSyncStatus', fakeElement()],
        ['salesRankingMonthTag', fakeElement()],
        ['adminActivityList', fakeElement()],
        ['adminActivityModalList', fakeElement()],
        ['adminActivityModal', fakeElement()],
        ['showActivityNotice', fakeElement()],
        ['adminActivityModalClose', fakeElement()]
    ]);
    return {
        getElementById: (id) => elements.get(id),
        createElement: () => fakeElement(),
        querySelectorAll: () => [],
        elements
    };
}

test('fetches the admin dashboard API and returns its data', async () => {
    let requestedUrl;
    const summary = { restStopCount: 203, latestSalesRankingMonth: '2026-06', lastSyncStatus: '준비중' };

    const result = await fetchAdminDashboard(async (url) => {
        requestedUrl = url;
        return { ok: true, json: async () => ({ code: 'SUCCESS', data: summary }) };
    });

    assert.equal(requestedUrl, '/api/admin/dashboard');
    assert.deepEqual(result, summary);
});

test('renders dashboard summary and uses 준비중 when month is missing', () => {
    const document = documentWithDashboardElements();

    renderDashboard(document, { restStopCount: 203, latestSalesRankingMonth: null, lastSyncStatus: null });

    assert.equal(document.elements.get('restStopCount').textContent, 203);
    assert.equal(document.elements.get('latestSalesRankingMonth').textContent, '준비중');
    assert.equal(document.elements.get('lastSyncStatus').textContent, '준비중');
    assert.equal(document.elements.get('salesRankingMonthTag').textContent, '기준월 없음');
});

test('renders a stable error state when dashboard loading fails', () => {
    const document = documentWithDashboardElements();

    renderDashboardError(document);

    assert.equal(document.elements.get('restStopCount').textContent, '확인 불가');
    assert.equal(document.elements.get('latestSalesRankingMonth').textContent, '확인 불가');
    assert.equal(document.elements.get('lastSyncStatus').textContent, '준비중');
    assert.equal(document.elements.get('salesRankingMonthTag').textContent, '조회 실패');
});

test('renders up to 5 recent activity logs inline and clears the empty state', () => {
    const document = documentWithDashboardElements();
    const logs = Array.from({ length: 7 }, (_, index) => ({
        actor: 'admin',
        message: `작업 ${index + 1}`,
        occurredAt: '2026-07-21 15:32'
    }));

    renderDashboard(document, { restStopCount: 1, latestSalesRankingMonth: '2026-06', recentActivityLogs: logs });

    const list = document.elements.get('adminActivityList');
    assert.equal(list.children.length, 5);
});

test('restores the empty-state message when there are no activity logs', () => {
    const document = documentWithDashboardElements();

    renderDashboard(document, { restStopCount: 0, latestSalesRankingMonth: null, recentActivityLogs: [] });

    const list = document.elements.get('adminActivityList');
    assert.equal(list.children.length, 1);
    assert.equal(list.children[0].className, 'activity-empty');
});

test('전체 보기 클릭 시 모달에 전체 활동 로그를 렌더링하고 연다', async () => {
    const document = documentWithDashboardElements();
    const logs = Array.from({ length: 7 }, (_, index) => ({
        actor: 'admin',
        message: `작업 ${index + 1}`,
        occurredAt: '2026-07-21 15:32'
    }));

    renderDashboard(document, { restStopCount: 1, recentActivityLogs: logs });
    bindActivityModal(document);
    document.elements.get('showActivityNotice').handlers.click();

    const modal = document.elements.get('adminActivityModal');
    const modalList = document.elements.get('adminActivityModalList');
    assert.equal(modal.open, true);
    assert.equal(modalList.children.length, 7);
});

function fakeAdminForm(action, actionKind) {
    const button = { disabled: false, textContent: '' };
    const form = {
        action,
        dataset: { actionKind },
        querySelector: () => button,
        reset: () => { form.wasReset = true; },
        addEventListener: (_event, handler) => { form.submitHandler = handler; },
        button
    };
    return form;
}

function fakeAdminDocument(form) {
    const overlay = { classList: { toggle: (_name, value) => { overlay.visible = value; } }, setAttribute: () => {} };
    const message = { textContent: '' };
    const toast = { textContent: '', className: '' };
    const dashboardElements = documentWithDashboardElements();
    return {
        getElementById: (id) =>
            ({ adminLoadingOverlay: overlay, adminLoadingMessage: message, adminToast: toast }[id]
                ?? dashboardElements.getElementById(id)),
        querySelectorAll: () => [form],
        overlay,
        message,
        toast
    };
}

function jsonResponse(ok, body) {
    return { ok, json: async () => body };
}

test('shows the backfill loading overlay and locks the page while submitting', () => {
    const form = fakeAdminForm('/api/admin/sales-rankings/backfill', 'backfill');
    const document = fakeAdminDocument(form);

    attachAdminForms(document, () => new Promise(() => {}), () => 'fake-form-data');
    form.submitHandler({ preventDefault: () => {} });

    assert.equal(form.dataset.submitting, 'true');
    assert.equal(form.button.disabled, true);
    assert.equal(form.button.textContent, '매핑 실행 중...');
    assert.equal(document.overlay.visible, true);
    assert.equal(document.message.textContent, '휴게소명 매핑을 실행하고 있습니다.');
});

test('shows the uploaded row count and refreshes the dashboard after a successful product upload', async () => {
    const form = fakeAdminForm('/api/admin/sales-rankings/products', 'product');
    const document = fakeAdminDocument(form);
    const calledUrls = [];
    const fetchImpl = async (url) => {
        calledUrls.push(url);
        if (url === '/api/admin/sales-rankings/products') {
            return jsonResponse(true, { data: 128 });
        }
        return jsonResponse(true, { data: { restStopCount: 203, latestSalesRankingMonth: '2026-06' } });
    };

    attachAdminForms(document, fetchImpl, () => 'fake-form-data');
    await form.submitHandler({ preventDefault: () => {} });

    assert.deepEqual(calledUrls, ['/api/admin/sales-rankings/products', '/api/admin/dashboard']);
    assert.equal(document.toast.textContent, '상품 판매순위 128건을 업로드했습니다.');
    assert.match(document.toast.className, /is-success/);
    assert.equal(form.wasReset, true);
    assert.equal(form.button.disabled, false);
    assert.equal(form.button.textContent, '업로드');
    assert.equal(form.dataset.submitting, 'false');
});

test('shows the uploaded row count and refreshes the dashboard after a successful restroom upload', async () => {
    const form = fakeAdminForm('/api/admin/rest-stops/restrooms', 'restroom');
    const document = fakeAdminDocument(form);
    const calledUrls = [];
    const fetchImpl = async (url) => {
        calledUrls.push(url);
        if (url === '/api/admin/rest-stops/restrooms') {
            return jsonResponse(true, { data: 211 });
        }
        return jsonResponse(true, { data: { restStopCount: 203, latestSalesRankingMonth: '2026-06' } });
    };

    attachAdminForms(document, fetchImpl, () => 'fake-form-data');
    await form.submitHandler({ preventDefault: () => {} });

    assert.deepEqual(calledUrls, ['/api/admin/rest-stops/restrooms', '/api/admin/dashboard']);
    assert.equal(document.toast.textContent, '화장실 현황 211건을 업로드했습니다.');
    assert.match(document.toast.className, /is-success/);
});

test('shows a fallback error message when a restroom upload request itself fails', async () => {
    const form = fakeAdminForm('/api/admin/rest-stops/restrooms', 'restroom');
    const document = fakeAdminDocument(form);
    const fetchImpl = async () => {
        throw new Error('network down');
    };

    attachAdminForms(document, fetchImpl, () => 'fake-form-data');
    await form.submitHandler({ preventDefault: () => {} });

    assert.equal(document.toast.textContent, '화장실 현황 업로드에 실패했습니다.');
    assert.match(document.toast.className, /is-error/);
});

test('shows the uploaded row count and refreshes the dashboard after a successful usage snapshot upload', async () => {
    const form = fakeAdminForm('/api/admin/rest-stops/usage-snapshots', 'usage');
    const document = fakeAdminDocument(form);
    const calledUrls = [];
    const fetchImpl = async (url) => {
        calledUrls.push(url);
        if (url === '/api/admin/rest-stops/usage-snapshots') {
            return jsonResponse(true, { data: 206 });
        }
        return jsonResponse(true, { data: { restStopCount: 203, latestSalesRankingMonth: '2026-06' } });
    };

    attachAdminForms(document, fetchImpl, () => 'fake-form-data');
    await form.submitHandler({ preventDefault: () => {} });

    assert.deepEqual(calledUrls, ['/api/admin/rest-stops/usage-snapshots', '/api/admin/dashboard']);
    assert.equal(document.toast.textContent, '이용객 및 교통량 현황 206건을 업로드했습니다.');
    assert.match(document.toast.className, /is-success/);
});

test('shows a fallback error message when a usage snapshot upload request itself fails', async () => {
    const form = fakeAdminForm('/api/admin/rest-stops/usage-snapshots', 'usage');
    const document = fakeAdminDocument(form);
    const fetchImpl = async () => {
        throw new Error('network down');
    };

    attachAdminForms(document, fetchImpl, () => 'fake-form-data');
    await form.submitHandler({ preventDefault: () => {} });

    assert.equal(document.toast.textContent, '이용객 및 교통량 현황 업로드에 실패했습니다.');
    assert.match(document.toast.className, /is-error/);
});

test('shows the server error message when a product upload fails', async () => {
    const form = fakeAdminForm('/api/admin/sales-rankings/products', 'product');
    const document = fakeAdminDocument(form);
    const fetchImpl = async () => jsonResponse(false, { message: 'CSV 형식이 올바르지 않습니다.' });

    attachAdminForms(document, fetchImpl, () => 'fake-form-data');
    await form.submitHandler({ preventDefault: () => {} });

    assert.equal(document.toast.textContent, 'CSV 형식이 올바르지 않습니다.');
    assert.match(document.toast.className, /is-error/);
    assert.equal(form.wasReset, undefined);
});

test('falls back to a generic error message when the upload request itself fails', async () => {
    const form = fakeAdminForm('/api/admin/sales-rankings/stores', 'store');
    const document = fakeAdminDocument(form);
    const fetchImpl = async () => {
        throw new Error('network down');
    };

    attachAdminForms(document, fetchImpl, () => 'fake-form-data');
    await form.submitHandler({ preventDefault: () => {} });

    assert.equal(document.toast.textContent, '판매순위 업로드에 실패했습니다.');
    assert.match(document.toast.className, /is-error/);
});

test('shows a completion toast for a successful backfill run', async () => {
    const form = fakeAdminForm('/api/admin/sales-rankings/backfill', 'backfill');
    const document = fakeAdminDocument(form);
    const fetchImpl = async (url) => {
        if (url === '/api/admin/sales-rankings/backfill') {
            return jsonResponse(true, { data: { restFoodMappedCount: 3 } });
        }
        return jsonResponse(true, { data: {} });
    };

    attachAdminForms(document, fetchImpl, () => 'fake-form-data');
    await form.submitHandler({ preventDefault: () => {} });

    assert.equal(document.toast.textContent, '전체 휴게소명 매핑이 완료되었습니다.');
    assert.match(document.toast.className, /is-success/);
});

test('logs and shows a stale-data notice when the post-submit dashboard refresh fails', async () => {
    const form = fakeAdminForm('/api/admin/sales-rankings/products', 'product');
    const document = fakeAdminDocument(form);
    const fetchImpl = async (url) => {
        if (url === '/api/admin/sales-rankings/products') {
            return jsonResponse(true, { data: 128 });
        }
        throw new Error('refresh failed');
    };
    const originalConsoleError = console.error;
    const loggedErrors = [];
    console.error = (...args) => loggedErrors.push(args);

    try {
        attachAdminForms(document, fetchImpl, () => 'fake-form-data');
        await form.submitHandler({ preventDefault: () => {} });
        await new Promise((resolve) => setTimeout(resolve, 0));
    } finally {
        console.error = originalConsoleError;
    }

    assert.equal(loggedErrors.length, 1);
    assert.match(document.toast.textContent, /최신|갱신|새로고침/);
    assert.match(document.toast.className, /is-error/);
});

test('classifies the submit action from the data-action-kind attribute rather than the URL', () => {
    const form = fakeAdminForm('/api/admin/sales-rankings/some-future-path', 'backfill');
    const document = fakeAdminDocument(form);

    attachAdminForms(document, () => new Promise(() => {}), () => 'fake-form-data');
    form.submitHandler({ preventDefault: () => {} });

    assert.equal(form.button.textContent, '매핑 실행 중...');
    assert.equal(document.message.textContent, '휴게소명 매핑을 실행하고 있습니다.');
});

test('ignores a duplicate submit while a request is already in flight', () => {
    const form = fakeAdminForm('/api/admin/sales-rankings/backfill', 'backfill');
    form.dataset.submitting = 'true';
    const document = fakeAdminDocument(form);
    let fetchCalls = 0;
    const fetchImpl = async () => {
        fetchCalls += 1;
        return jsonResponse(true, { data: {} });
    };

    attachAdminForms(document, fetchImpl, () => 'fake-form-data');
    form.submitHandler({ preventDefault: () => {} });

    assert.equal(fetchCalls, 0);
});

import { closeDialogById, openDialogById, setGlobalLoading, showToast } from './admin-common.js';

const ADMIN_DASHBOARD_API = '/api/admin/dashboard';
const ACTIVITY_LIST_INLINE_LIMIT = 5;
const ACTIVITY_EMPTY_MESSAGE = '최근 실행한 작업이 없습니다.';

let latestActivityLogs = [];

function setDashboardValue(document, id, value) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = value;
    }
}

function createActivityItem(document, log) {
    const item = document.createElement('div');
    item.className = 'activity-item';

    const main = document.createElement('div');
    main.className = 'activity-item-main';
    const actor = document.createElement('span');
    actor.className = 'activity-item-actor';
    actor.textContent = log.actor;
    const message = document.createElement('span');
    message.className = 'activity-item-message';
    message.textContent = log.message;
    main.appendChild(actor);
    main.appendChild(message);

    const time = document.createElement('span');
    time.className = 'activity-item-time';
    time.textContent = log.occurredAt;

    item.appendChild(main);
    item.appendChild(time);
    return item;
}

function renderActivityList(document, elementId, logs) {
    const container = document.getElementById(elementId);
    if (!container) {
        return;
    }

    if (logs.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'activity-empty';
        empty.textContent = ACTIVITY_EMPTY_MESSAGE;
        container.replaceChildren(empty);
        return;
    }

    container.replaceChildren(...logs.map((log) => createActivityItem(document, log)));
}

export function openActivityModal(document) {
    openDialogById(document, 'adminActivityModal', {
        onOpened: () => renderActivityList(document, 'adminActivityModalList', latestActivityLogs)
    });
}

export function bindActivityModal(document) {
    document.getElementById('showActivityNotice')?.addEventListener('click', () => openActivityModal(document));
    document.getElementById('adminActivityModalClose')?.addEventListener('click', () => {
        closeDialogById(document, 'adminActivityModal');
    });
}

export function renderDashboard(document, summary) {
    const latestMonth = summary.latestSalesRankingMonth || '준비중';
    setDashboardValue(document, 'restStopCount', summary.restStopCount ?? '확인 불가');
    setDashboardValue(document, 'latestSalesRankingMonth', latestMonth);
    setDashboardValue(document, 'lastSyncStatus', summary.lastSyncStatus || '준비중');
    setDashboardValue(document, 'salesRankingMonthTag', latestMonth === '준비중' ? '기준월 없음' : `${latestMonth} 기준`);

    latestActivityLogs = summary.recentActivityLogs || [];
    renderActivityList(document, 'adminActivityList', latestActivityLogs.slice(0, ACTIVITY_LIST_INLINE_LIMIT));
}

export function renderDashboardError(document) {
    setDashboardValue(document, 'restStopCount', '확인 불가');
    setDashboardValue(document, 'latestSalesRankingMonth', '확인 불가');
    setDashboardValue(document, 'lastSyncStatus', '준비중');
    setDashboardValue(document, 'salesRankingMonthTag', '조회 실패');
}

function submitButton(form) {
    return form.querySelector('button[type="submit"]');
}

function actionKind(form) {
    return form.dataset.actionKind || 'product';
}

function loadingMessage(kind) {
    return kind === 'backfill' ? '휴게소명 매핑을 실행하고 있습니다.' : '파일을 업로드하고 있습니다.';
}

function submittingLabel(kind) {
    return kind === 'backfill' ? '매핑 실행 중...' : '업로드 중...';
}

function idleLabel(kind) {
    return kind === 'backfill' ? '전체 휴게소명 매핑' : '업로드';
}

function successMessage(kind, uploadedCount) {
    if (kind === 'backfill') {
        return '전체 휴게소명 매핑이 완료되었습니다.';
    }
    if (kind === 'restroom') {
        return `화장실 현황 ${uploadedCount ?? 0}건을 업로드했습니다.`;
    }
    const label = kind === 'store' ? '매장' : '상품';
    return `${label} 판매순위 ${uploadedCount ?? 0}건을 업로드했습니다.`;
}

function failureMessage(kind) {
    if (kind === 'backfill') {
        return '전체 휴게소명 매핑에 실패했습니다.';
    }
    return kind === 'restroom' ? '화장실 현황 업로드에 실패했습니다.' : '판매순위 업로드에 실패했습니다.';
}

function buildFormData(form) {
    return new FormData(form);
}

async function submitAdminForm(document, form, fetchImpl, buildFormDataImpl) {
    const kind = actionKind(form);
    const button = submitButton(form);

    try {
        const response = await fetchImpl(form.action, {
            method: 'POST',
            body: buildFormDataImpl(form),
            headers: { Accept: 'application/json' }
        });
        const payload = await response.json().catch(() => null);
        if (response.ok) {
            showToast(document, successMessage(kind, payload?.data), 'success');
            form.reset?.();
            fetchAdminDashboard(fetchImpl)
                .then((summary) => renderDashboard(document, summary))
                .catch((error) => {
                    console.error('관리자 대시보드 갱신에 실패했습니다.', error);
                    showToast(document, '대시보드 정보가 최신이 아닐 수 있습니다. 새로고침해주세요.', 'error');
                });
        } else {
            showToast(document, payload?.message || failureMessage(kind), 'error');
        }
    } catch {
        showToast(document, failureMessage(kind), 'error');
    } finally {
        setGlobalLoading(document, false);
        form.dataset.submitting = 'false';
        if (button) {
            button.disabled = false;
            button.textContent = idleLabel(kind);
        }
    }
}

export function attachAdminForms(document, fetchImpl = fetch, buildFormDataImpl = buildFormData) {
    const forms = document.querySelectorAll('form[data-action-kind]');
    forms.forEach((form) => {
        form.addEventListener('submit', (event) => {
            event.preventDefault();
            if (form.dataset.submitting === 'true') {
                return undefined;
            }

            form.dataset.submitting = 'true';
            const kind = actionKind(form);
            const button = submitButton(form);
            if (button) {
                button.disabled = true;
                button.textContent = submittingLabel(kind);
            }
            setGlobalLoading(document, true, loadingMessage(kind));
            return submitAdminForm(document, form, fetchImpl, buildFormDataImpl);
        });
    });
}

export async function fetchAdminDashboard(fetchImpl = fetch) {
    const response = await fetchImpl(ADMIN_DASHBOARD_API, { headers: { Accept: 'application/json' } });
    if (!response.ok) {
        throw new Error(`Dashboard request failed: ${response.status}`);
    }
    const payload = await response.json();
    return payload.data || {};
}

export function initializeAdminDashboard(document, fetchImpl = fetch) {
    attachAdminForms(document, fetchImpl);
    bindActivityModal(document);
    fetchAdminDashboard(fetchImpl)
        .then((summary) => renderDashboard(document, summary))
        .catch((error) => {
            console.error('관리자 대시보드 조회에 실패했습니다.', error);
            renderDashboardError(document);
        });
}

if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => initializeAdminDashboard(document));
}

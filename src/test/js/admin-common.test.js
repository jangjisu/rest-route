import assert from 'node:assert/strict';
import test from 'node:test';

import { csrfFrom, setGlobalLoading, showToast } from '../../main/resources/static/js/admin-common.js';

test('shows and removes a toast through the shared toast helper', async () => {
    const toast = { textContent: '', className: '' };
    const document = { getElementById: () => toast };

    showToast(document, '완료되었습니다.', 'success', 1);
    assert.equal(toast.textContent, '완료되었습니다.');
    assert.match(toast.className, /is-visible/);
    await new Promise((resolve) => setTimeout(resolve, 5));
    assert.equal(toast.textContent, '');
    assert.equal(toast.className, 'admin-toast');
});

test('toggles the shared loading overlay on and off', () => {
    const overlay = { classList: { toggle: (_name, value) => { overlay.visible = value; } }, setAttribute: () => {} };
    const message = { textContent: '' };
    const document = {
        getElementById: (id) => ({ adminLoadingOverlay: overlay, adminLoadingMessage: message }[id])
    };

    setGlobalLoading(document, true, '처리 중입니다.');

    assert.equal(overlay.visible, true);
    assert.equal(message.textContent, '처리 중입니다.');
});

test('csrfFrom reads header name and token from the source element', () => {
    const source = {
        dataset: { csrfHeader: 'X-Custom-Header' },
        querySelector: () => ({ value: 'token-value' })
    };

    assert.deepEqual(csrfFrom(source), { headerName: 'X-Custom-Header', token: 'token-value' });
});

test('csrfFrom falls back to defaults when header/token are missing', () => {
    const source = { dataset: {}, querySelector: () => null };

    assert.deepEqual(csrfFrom(source), { headerName: 'X-CSRF-TOKEN', token: '' });
});

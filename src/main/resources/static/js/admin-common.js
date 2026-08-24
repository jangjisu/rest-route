const TOAST_DURATION_MS = 3200;

let toastTimer;

export function showToast(document, message, type = 'success', duration = TOAST_DURATION_MS) {
    const toast = document.getElementById('adminToast');
    if (!toast) {
        return;
    }

    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.className = `admin-toast is-visible is-${type}`;
    toastTimer = setTimeout(() => {
        toast.className = 'admin-toast';
        toast.textContent = '';
    }, duration);
}

export function openDialogById(document, id, { guard, onOpened } = {}) {
    const dialog = document.getElementById(id);
    if (!dialog || dialog.open) {
        return;
    }
    if (guard && !guard()) {
        return;
    }
    onOpened?.();
    dialog.showModal();
}

export function closeDialogById(document, id, onClosed) {
    const dialog = document.getElementById(id);
    if (!dialog?.open) {
        return;
    }
    dialog.close();
    onClosed?.();
}

export function csrfFrom(source) {
    return {
        headerName: source.dataset.csrfHeader || 'X-CSRF-TOKEN',
        token: source.querySelector('input[name="_csrf"]')?.value || ''
    };
}

export function setGlobalLoading(document, isLoading, message = '처리 중입니다.') {
    const overlay = document.getElementById('adminLoadingOverlay');
    if (!overlay) {
        return;
    }

    overlay.setAttribute('aria-hidden', String(!isLoading));
    overlay.classList.toggle('is-visible', isLoading);
    const messageElement = document.getElementById('adminLoadingMessage');
    if (messageElement) {
        messageElement.textContent = message;
    }
}

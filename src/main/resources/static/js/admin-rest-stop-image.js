import { csrfFrom } from './admin-common.js';

const REST_STOPS_ENDPOINT = '/api/rest-stops';
const MAX_IMAGE_SIZE_BYTES = 20 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png']);

function imageEndpoint(serviceAreaCode) {
    return `/api/rest-stops/${encodeURIComponent(serviceAreaCode)}/images/detail`;
}

function adminImageEndpoint(serviceAreaCode) {
    return `/api/admin/rest-stops/${encodeURIComponent(serviceAreaCode)}/image`;
}

function responseError(operation, response) {
    return new Error(`${operation} request failed: ${response.status}`);
}

export function validateRestStopImageFile(file) {
    if (!file || !ALLOWED_IMAGE_TYPES.has(file.type)) {
        return 'JPEG 또는 PNG 파일만 업로드할 수 있습니다.';
    }
    if (file.size > MAX_IMAGE_SIZE_BYTES) {
        return '파일 크기는 20MB 이하여야 합니다.';
    }
    return null;
}

export async function fetchAdminRestStops(fetchImpl = fetch) {
    const response = await fetchImpl(REST_STOPS_ENDPOINT, { headers: { Accept: 'application/json' } });
    if (!response.ok) {
        throw responseError('Rest stop list', response);
    }

    const payload = await response.json();
    if (payload?.code !== 'SUCCESS' || !Array.isArray(payload.data)) {
        throw new Error('Rest stop list response is invalid');
    }

    return payload.data
        .filter((restStop) => typeof restStop?.serviceAreaCode === 'string' && restStop.serviceAreaCode.trim() !== '')
        .map((restStop) => ({
            serviceAreaCode: restStop.serviceAreaCode,
            unitName: restStop.unitName
        }))
        .sort((left, right) => String(left.unitName).localeCompare(String(right.unitName), 'ko'));
}

export async function fetchRestStopImage(serviceAreaCode, fetchImpl = fetch) {
    const response = await fetchImpl(imageEndpoint(serviceAreaCode), { cache: 'no-cache' });
    if (response.status === 204) {
        return { status: 'empty' };
    }
    if (response.status === 404) {
        return { status: 'not-found' };
    }
    if (!response.ok) {
        throw responseError('Image preview', response);
    }
    return { status: 'success', blob: await response.blob() };
}

function mutationOptions(method, csrf, body) {
    const options = {
        method,
        headers: { [csrf.headerName]: csrf.token }
    };
    if (body) {
        options.body = body;
    }
    return options;
}

export async function uploadRestStopImage(serviceAreaCode, file, csrf, fetchImpl = fetch) {
    const body = new globalThis.FormData();
    body.append('file', file);
    const response = await fetchImpl(
        adminImageEndpoint(serviceAreaCode),
        mutationOptions('PUT', csrf, body)
    );
    if (!response.ok) {
        throw responseError('Image upload', response);
    }
}

export async function deleteRestStopImage(serviceAreaCode, csrf, fetchImpl = fetch) {
    const response = await fetchImpl(
        adminImageEndpoint(serviceAreaCode),
        mutationOptions('DELETE', csrf)
    );
    if (!response.ok) {
        throw responseError('Image delete', response);
    }
}

function createOption(document, restStop) {
    const option = document.createElement('option');
    option.value = restStop.serviceAreaCode;
    option.textContent = `${restStop.unitName || '이름 정보 없음'} · ${restStop.serviceAreaCode}`;
    return option;
}

export function initializeAdminRestStopImage(document, {
    fetchImpl = fetch,
    urlApi = globalThis.URL,
    confirmImpl = globalThis.confirm,
    onNotice = () => {}
} = {}) {
    const form = document.getElementById('restStopImageForm');
    const select = document.getElementById('restStopImageSelect');
    const fileInput = document.getElementById('restStopImageFile');
    const previewFrame = document.getElementById('restStopImagePreviewFrame');
    const preview = document.getElementById('restStopImagePreview');
    const empty = document.getElementById('restStopImageEmpty');
    const status = document.getElementById('restStopImageStatus');
    const submitButton = document.getElementById('restStopImageSubmit');
    const deleteButton = document.getElementById('restStopImageDelete');
    if (!form || !select || !fileInput || !previewFrame || !preview || !empty || !status
        || !submitButton || !deleteButton) {
        return;
    }

    let objectUrl;
    let storedImage = false;
    let canEdit = false;
    let previewRequestId = 0;

    function revokePreviewUrl() {
        if (objectUrl) {
            urlApi.revokeObjectURL(objectUrl);
            objectUrl = undefined;
        }
    }

    function showPreview(blob, message) {
        revokePreviewUrl();
        objectUrl = urlApi.createObjectURL(blob);
        preview.src = objectUrl;
        preview.alt = `${select.selectedOptions[0]?.textContent || '선택한 휴게소'} 전경 미리보기`;
        previewFrame.hidden = false;
        empty.hidden = true;
        status.textContent = message;
    }

    function showEmpty(message) {
        revokePreviewUrl();
        preview.removeAttribute('src');
        preview.alt = '';
        previewFrame.hidden = true;
        empty.hidden = false;
        empty.textContent = message;
        status.textContent = '';
    }

    function updateActions(enabled) {
        canEdit = enabled;
        fileInput.disabled = !enabled;
        submitButton.disabled = !enabled;
        deleteButton.disabled = !enabled || !storedImage;
        submitButton.textContent = storedImage ? '이미지 교체' : '이미지 등록';
    }

    function setBusy(busy, message = '') {
        select.disabled = busy;
        if (busy) {
            fileInput.disabled = true;
            submitButton.disabled = true;
            deleteButton.disabled = true;
        } else {
            updateActions(canEdit);
        }
        form.setAttribute('aria-busy', String(busy));
        if (message) {
            status.textContent = message;
        }
    }

    async function loadSelectedImage() {
        const requestId = ++previewRequestId;
        const serviceAreaCode = select.value;
        fileInput.value = '';
        storedImage = false;
        if (serviceAreaCode === '') {
            showEmpty('휴게소를 선택하면 등록된 이미지를 확인할 수 있습니다.');
            updateActions(false);
            return;
        }

        setBusy(true, '이미지를 확인하고 있습니다.');
        try {
            const result = await fetchRestStopImage(serviceAreaCode, fetchImpl);
            if (requestId !== previewRequestId) {
                return;
            }
            if (result.status === 'not-found') {
                showEmpty('존재하지 않는 휴게소 코드입니다.');
                updateActions(false);
                return;
            }
            storedImage = result.status === 'success';
            if (storedImage) {
                showPreview(result.blob, '현재 등록된 이미지입니다.');
            } else {
                showEmpty('등록된 이미지가 없습니다.');
            }
            updateActions(true);
        } catch (error) {
            if (requestId !== previewRequestId) {
                return;
            }
            console.error('휴게소 이미지 조회에 실패했습니다.', error);
            showEmpty('이미지를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
            updateActions(false);
        } finally {
            if (requestId === previewRequestId) {
                setBusy(false);
            }
        }
    }

    fileInput.addEventListener('change', () => {
        const file = fileInput.files?.[0];
        if (!file) {
            loadSelectedImage();
            return;
        }
        const validationMessage = validateRestStopImageFile(file);
        if (validationMessage) {
            fileInput.value = '';
            onNotice(validationMessage, 'error');
            loadSelectedImage();
            return;
        }
        showPreview(file, '업로드 전 미리보기입니다.');
    });

    select.addEventListener('change', loadSelectedImage);
    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const file = fileInput.files?.[0];
        const validationMessage = validateRestStopImageFile(file);
        if (validationMessage) {
            onNotice(validationMessage, 'error');
            return;
        }

        setBusy(true, storedImage ? '이미지를 교체하고 있습니다.' : '이미지를 등록하고 있습니다.');
        try {
            await uploadRestStopImage(select.value, file, csrfFrom(form), fetchImpl);
            onNotice(storedImage ? '휴게소 이미지를 교체했습니다.' : '휴게소 이미지를 등록했습니다.');
            await loadSelectedImage();
        } catch (error) {
            console.error('휴게소 이미지 저장에 실패했습니다.', error);
            onNotice('휴게소 이미지 저장에 실패했습니다.', 'error');
            setBusy(false);
        }
    });

    deleteButton.addEventListener('click', async () => {
        if (!storedImage || !confirmImpl('등록된 휴게소 이미지를 삭제할까요?')) {
            return;
        }

        setBusy(true, '이미지를 삭제하고 있습니다.');
        try {
            await deleteRestStopImage(select.value, csrfFrom(form), fetchImpl);
            storedImage = false;
            fileInput.value = '';
            showEmpty('등록된 이미지가 없습니다.');
            updateActions(true);
            onNotice('휴게소 이미지를 삭제했습니다.');
        } catch (error) {
            console.error('휴게소 이미지 삭제에 실패했습니다.', error);
            onNotice('휴게소 이미지 삭제에 실패했습니다.', 'error');
        } finally {
            setBusy(false);
        }
    });

    showEmpty('휴게소 목록을 불러오고 있습니다.');
    updateActions(false);
    fetchAdminRestStops(fetchImpl)
        .then((restStops) => {
            select.append(...restStops.map((restStop) => createOption(document, restStop)));
            select.disabled = false;
            showEmpty('휴게소를 선택하면 등록된 이미지를 확인할 수 있습니다.');
        })
        .catch((error) => {
            console.error('휴게소 목록 조회에 실패했습니다.', error);
            showEmpty('휴게소 목록을 불러오지 못했습니다.');
        });
}

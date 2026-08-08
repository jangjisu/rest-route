const ADMIN_REST_STOPS_ENDPOINT = '/api/admin/rest-stops';

function editableEndpoint(serviceAreaCode) {
    return `${ADMIN_REST_STOPS_ENDPOINT}/${encodeURIComponent(serviceAreaCode)}/editable`;
}

function overrideEndpoint(serviceAreaCode) {
    return `${editableEndpoint(serviceAreaCode)}/override`;
}

function hasValidData(body) {
    return body?.data !== null && typeof body?.data === 'object' && !Array.isArray(body.data);
}

export async function fetchEditableRestStop(serviceAreaCode, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(editableEndpoint(serviceAreaCode), { headers: { Accept: 'application/json' } });
        const body = await response.json();

        if (response.status === 404 && body?.code === 'NOT_FOUND') {
            return { status: 'not-found' };
        }

        if (response.ok && body?.code === 'SUCCESS' && hasValidData(body)) {
            return { status: 'success', data: body.data };
        }

        return { status: 'error' };
    } catch {
        return { status: 'error' };
    }
}

function mutationOptions(method, csrf, payload) {
    const options = {
        method,
        headers: { [csrf.headerName]: csrf.token }
    };
    if (payload) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(payload);
    }
    return options;
}

async function parseMutationResponse(response) {
    const body = await response.json();

    if (response.status === 404 && body?.code === 'NOT_FOUND') {
        return { status: 'not-found' };
    }

    if (response.status === 400 && body?.code === 'INVALID_PARAMETER') {
        return { status: 'invalid', message: body?.message || '입력값을 확인해주세요.' };
    }

    if (response.ok && body?.code === 'SUCCESS' && hasValidData(body)) {
        return { status: 'success', data: body.data };
    }

    return { status: 'error' };
}

export async function saveEditableRestStop(serviceAreaCode, payload, csrf, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(editableEndpoint(serviceAreaCode), mutationOptions('PUT', csrf, payload));
        return await parseMutationResponse(response);
    } catch {
        return { status: 'error' };
    }
}

export async function createRestStop(payload, csrf, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(ADMIN_REST_STOPS_ENDPOINT, mutationOptions('POST', csrf, payload));
        return await parseMutationResponse(response);
    } catch {
        return { status: 'error' };
    }
}

export async function clearRestStopOverride(serviceAreaCode, csrf, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(overrideEndpoint(serviceAreaCode), mutationOptions('DELETE', csrf));
        return await parseMutationResponse(response);
    } catch {
        return { status: 'error' };
    }
}

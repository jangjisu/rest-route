const RESTROOM_LINKS_ENDPOINT = '/api/admin/rest-stops/restroom-links';
const RESTROOMS_SEARCH_ENDPOINT = '/api/admin/rest-stop-restrooms/search';

function restroomEndpoint(restroomId) {
    return `/api/admin/rest-stop-restrooms/${encodeURIComponent(restroomId)}`;
}

export async function fetchRestroomLinks(fetchImpl = fetch) {
    try {
        const response = await fetchImpl(RESTROOM_LINKS_ENDPOINT, { headers: { Accept: 'application/json' } });
        const body = await response.json();

        if (response.ok && body?.code === 'SUCCESS' && Array.isArray(body.data)) {
            return { status: 'success', restStops: body.data };
        }

        return { status: 'error' };
    } catch {
        return { status: 'error' };
    }
}

export async function searchRestrooms(name, routeName, fetchImpl = fetch) {
    try {
        const params = new globalThis.URLSearchParams();
        if (name) {
            params.set('name', name);
        }
        if (routeName) {
            params.set('routeName', routeName);
        }
        const response = await fetchImpl(`${RESTROOMS_SEARCH_ENDPOINT}?${params.toString()}`, {
            headers: { Accept: 'application/json' }
        });
        const body = await response.json();

        if (response.ok && body?.code === 'SUCCESS' && Array.isArray(body.data)) {
            return { status: 'success', restrooms: body.data };
        }

        return { status: 'error' };
    } catch {
        return { status: 'error' };
    }
}

function jsonMutationOptions(method, csrf, payload) {
    return {
        method,
        headers: { [csrf.headerName]: csrf.token, 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    };
}

async function parseLinkResponse(response) {
    const body = await response.json();

    if (response.status === 404 && body?.code === 'NOT_FOUND') {
        return { status: 'not-found' };
    }

    if (response.ok && body?.code === 'SUCCESS' && body?.data && typeof body.data === 'object') {
        return { status: 'success', restroom: body.data };
    }

    return { status: 'error' };
}

export async function linkRestroom(restroomId, serviceAreaCode, csrf, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(
            `${restroomEndpoint(restroomId)}/link`,
            jsonMutationOptions('PUT', csrf, { serviceAreaCode })
        );
        return await parseLinkResponse(response);
    } catch {
        return { status: 'error' };
    }
}

export async function unlinkRestroom(restroomId, csrf, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(`${restroomEndpoint(restroomId)}/link`, {
            method: 'DELETE',
            headers: { [csrf.headerName]: csrf.token }
        });
        return await parseLinkResponse(response);
    } catch {
        return { status: 'error' };
    }
}

const OIL_LINKS_ENDPOINT = '/api/admin/rest-stops/oil-links';
const OIL_STATIONS_SEARCH_ENDPOINT = '/api/admin/oil-stations/search';

function oilStationEndpoint(oilId) {
    return `/api/admin/oil-stations/${encodeURIComponent(oilId)}`;
}

export async function fetchOilLinks(fetchImpl = fetch) {
    try {
        const response = await fetchImpl(OIL_LINKS_ENDPOINT, { headers: { Accept: 'application/json' } });
        const body = await response.json();

        if (response.ok && body?.code === 'SUCCESS' && Array.isArray(body.data)) {
            return { status: 'success', restStops: body.data };
        }

        return { status: 'error' };
    } catch {
        return { status: 'error' };
    }
}

export async function searchOilStations(name, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(`${OIL_STATIONS_SEARCH_ENDPOINT}?name=${encodeURIComponent(name)}`, {
            headers: { Accept: 'application/json' }
        });
        const body = await response.json();

        if (response.ok && body?.code === 'SUCCESS' && Array.isArray(body.data)) {
            return { status: 'success', oilStations: body.data };
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
        return { status: 'success', oilStation: body.data };
    }

    return { status: 'error' };
}

export async function linkOilStation(oilId, serviceAreaCode, csrf, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(
            `${oilStationEndpoint(oilId)}/link`,
            jsonMutationOptions('PUT', csrf, { serviceAreaCode })
        );
        return await parseLinkResponse(response);
    } catch {
        return { status: 'error' };
    }
}

export async function unlinkOilStation(oilId, csrf, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(`${oilStationEndpoint(oilId)}/link`, {
            method: 'DELETE',
            headers: { [csrf.headerName]: csrf.token }
        });
        return await parseLinkResponse(response);
    } catch {
        return { status: 'error' };
    }
}

export async function clearOilStationOverride(oilId, csrf, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(`${oilStationEndpoint(oilId)}/override`, {
            method: 'DELETE',
            headers: { [csrf.headerName]: csrf.token }
        });
        return await parseLinkResponse(response);
    } catch {
        return { status: 'error' };
    }
}

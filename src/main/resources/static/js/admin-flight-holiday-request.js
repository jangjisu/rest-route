const HOLIDAYS_ENDPOINT = '/api/admin/flights/holidays';

function holidayEndpoint(holidayId) {
    return `${HOLIDAYS_ENDPOINT}/${encodeURIComponent(holidayId)}`;
}

export async function fetchFlightHolidays(fetchImpl = fetch) {
    try {
        const response = await fetchImpl(HOLIDAYS_ENDPOINT, { headers: { Accept: 'application/json' } });
        const body = await response.json();

        if (response.ok && body?.code === 'SUCCESS' && Array.isArray(body.data)) {
            return { status: 'success', holidays: body.data };
        }

        return { status: 'error' };
    } catch {
        return { status: 'error' };
    }
}

export async function addFlightHoliday(date, name, csrf, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(HOLIDAYS_ENDPOINT, {
            method: 'POST',
            headers: { [csrf.headerName]: csrf.token, 'Content-Type': 'application/json' },
            body: JSON.stringify({ date, name })
        });
        const body = await response.json();

        if (response.ok && body?.code === 'SUCCESS' && body?.data && typeof body.data === 'object') {
            return { status: 'success', holiday: body.data };
        }
        if (response.status === 400) {
            return { status: 'invalid', message: body?.message };
        }

        return { status: 'error' };
    } catch {
        return { status: 'error' };
    }
}

export async function deleteFlightHoliday(holidayId, csrf, fetchImpl = fetch) {
    try {
        const response = await fetchImpl(holidayEndpoint(holidayId), {
            method: 'DELETE',
            headers: { [csrf.headerName]: csrf.token }
        });

        if (response.status === 204) {
            return { status: 'success' };
        }
        if (response.status === 404) {
            return { status: 'not-found' };
        }

        return { status: 'error' };
    } catch {
        return { status: 'error' };
    }
}

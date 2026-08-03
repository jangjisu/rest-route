const REST_STOP_COMPARE_ENDPOINT = '/api/rest-stops/compare';

export function createRestStopCompareRequest({ fetchImpl = fetch, onState = () => {} } = {}) {
    let currentRequestId = 0;
    let activeRequestController;

    function emitIfCurrent(requestId, state) {
        if (requestId === currentRequestId) {
            onState(state);
        }
    }

    async function load(serviceAreaCodeA, serviceAreaCodeB) {
        activeRequestController?.abort();
        activeRequestController = new globalThis.AbortController();

        const requestId = ++currentRequestId;
        emitIfCurrent(requestId, { status: 'loading' });

        try {
            const params = new globalThis.URLSearchParams({ serviceAreaCodeA, serviceAreaCodeB });
            const response = await fetchImpl(`${REST_STOP_COMPARE_ENDPOINT}?${params.toString()}`, {
                signal: activeRequestController.signal
            });
            const body = await response.json();

            if (response.ok && body?.code === 'SUCCESS' && body.data) {
                emitIfCurrent(requestId, { status: 'success', comparison: body.data });
                return;
            }

            emitIfCurrent(requestId, { status: 'error', message: body?.message });
        } catch (error) {
            if (error?.name === 'AbortError') {
                return;
            }

            emitIfCurrent(requestId, { status: 'error' });
        }
    }

    function invalidate() {
        currentRequestId += 1;
        activeRequestController?.abort();
        activeRequestController = undefined;
    }

    return { invalidate, load };
}

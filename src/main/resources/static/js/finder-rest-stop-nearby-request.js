const NEARBY_ENDPOINT = '/api/rest-stops/nearby';

/**
 * "이름·거리로 찾기" 목록이 쓰는 유일한 API. 위치(originLat/originLng)·이름(name)·관심
 * 연료(interest) 전부 선택값이라, 있는 것만 쿼리스트링에 실어 보낸다 — 서버가 없는 값에 대응하는
 * 응답 필드를 null로 채워주므로 프런트는 그 필드가 없을 때 표시만 안 하면 된다.
 */
export function createFinderRestStopNearbyRequest({ fetchImpl = fetch, onState = () => {} } = {}) {
    let currentRequestId = 0;
    let activeRequestController;

    function emitIfCurrent(requestId, state) {
        if (requestId === currentRequestId) {
            onState(state);
        }
    }

    async function load({ originLat, originLng, name, interest } = {}) {
        activeRequestController?.abort();
        activeRequestController = new globalThis.AbortController();

        const requestId = ++currentRequestId;
        emitIfCurrent(requestId, { status: 'loading' });

        const params = new globalThis.URLSearchParams();
        if (Number.isFinite(originLat) && Number.isFinite(originLng)) {
            params.set('originLat', originLat);
            params.set('originLng', originLng);
        }
        const trimmedName = typeof name === 'string' ? name.trim() : '';
        if (trimmedName !== '') {
            params.set('name', trimmedName);
        }
        if (interest) {
            params.set('interest', interest);
        }

        try {
            const query = params.toString();
            const response = await fetchImpl(
                query === '' ? NEARBY_ENDPOINT : `${NEARBY_ENDPOINT}?${query}`,
                { signal: activeRequestController.signal }
            );
            const body = await response.json();

            if (response.ok && body?.code === 'SUCCESS' && Array.isArray(body.data)) {
                emitIfCurrent(requestId, { status: 'success', restStops: body.data });
                return;
            }

            emitIfCurrent(requestId, { status: 'error' });
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

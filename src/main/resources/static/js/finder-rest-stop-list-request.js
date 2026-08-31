const REST_STOPS_ENDPOINT = '/api/rest-stops';

/**
 * "이름·거리로 찾기" 모드에서 위치가 허용됐을 때 쓰는 전체 휴게소 목록 조회. 이름 검색은
 * rest-stop-name-search-request.js를 그대로 재사용하고, 이 모듈은 이름 없이 거리순 정렬에
 * 쓸 전체 목록만 담당한다.
 */
export function createFinderRestStopListRequest({ fetchImpl = fetch, onState = () => {} } = {}) {
    let currentRequestId = 0;

    function emitIfCurrent(requestId, state) {
        if (requestId === currentRequestId) {
            onState(state);
        }
    }

    async function load() {
        const requestId = ++currentRequestId;
        emitIfCurrent(requestId, { status: 'loading' });

        try {
            const response = await fetchImpl(REST_STOPS_ENDPOINT);
            const body = await response.json();

            if (response.ok && body?.code === 'SUCCESS' && Array.isArray(body.data)) {
                emitIfCurrent(requestId, { status: 'success', restStops: body.data });
                return;
            }

            emitIfCurrent(requestId, { status: 'error' });
        } catch {
            emitIfCurrent(requestId, { status: 'error' });
        }
    }

    return { load };
}

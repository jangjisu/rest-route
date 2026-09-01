const ROUTE_REST_STOP_LIST_ENDPOINT = '/api/route-rest-stops/list';

/**
 * "목적지로 추천받기"가 쓰는 신규 엔드포인트(/api/route-rest-stops/list) 전용 요청 모듈. 지도 화면이
 * 쓰는 기존 route-rest-stop-request.js와는 완전히 별개다 — 대안 경로/이미지 없이 거리·유가(fuelType
 * 스코프)가 붙은 평평한 목록만 받는다. 목적지는 검색어(destinationQuery) 또는 좌표
 * (destinationLat/Lng, place-search 후보 선택 시)로 넘길 수 있다.
 */
export function createRouteRestStopListRequest({ fetchImpl = fetch, onState = () => {} } = {}) {
    let currentRequestId = 0;
    let activeRequestController;

    function emitIfCurrent(requestId, state) {
        if (requestId === currentRequestId) {
            onState(state);
        }
    }

    async function load({
        originLat,
        originLng,
        destinationQuery,
        destinationLat,
        destinationLng,
        destinationName,
        fuelType
    } = {}) {
        activeRequestController?.abort();
        activeRequestController = new globalThis.AbortController();

        const requestId = ++currentRequestId;
        const trimmedQuery = typeof destinationQuery === 'string' ? destinationQuery.trim() : '';
        const hasOrigin = Number.isFinite(originLat) && Number.isFinite(originLng);
        const hasDestinationCoordinates = Number.isFinite(destinationLat) && Number.isFinite(destinationLng);

        if (!hasOrigin || (trimmedQuery === '' && !hasDestinationCoordinates)) {
            emitIfCurrent(requestId, { status: 'error' });
            return;
        }

        emitIfCurrent(requestId, { status: 'loading' });

        const params = new globalThis.URLSearchParams();
        params.set('originLat', originLat);
        params.set('originLng', originLng);
        if (hasDestinationCoordinates) {
            params.set('destinationLat', destinationLat);
            params.set('destinationLng', destinationLng);
            if (destinationName) {
                params.set('destinationName', destinationName);
            }
        } else {
            params.set('destinationQuery', trimmedQuery);
        }
        if (fuelType) {
            params.set('fuelType', fuelType);
        }

        try {
            const response = await fetchImpl(`${ROUTE_REST_STOP_LIST_ENDPOINT}?${params.toString()}`, {
                signal: activeRequestController.signal
            });
            const body = await response.json();

            if (response.status === 404 && body?.code === 'NOT_FOUND') {
                emitIfCurrent(requestId, { status: 'not-found', message: body?.message });
                return;
            }

            if (body?.code === 'EXTERNAL_API_UNAVAILABLE') {
                emitIfCurrent(requestId, { status: 'external-unavailable' });
                return;
            }

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

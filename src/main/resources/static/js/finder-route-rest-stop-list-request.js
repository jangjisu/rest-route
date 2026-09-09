import { createRequestTracker } from './request-tracker.js';

const ROUTE_REST_STOP_LIST_ENDPOINT = '/api/route-rest-stops/list';

/**
 * "목적지로 추천받기"가 쓰는 신규 엔드포인트(/api/route-rest-stops/list) 전용 요청 모듈. 지도 화면이
 * 쓰는 기존 route-rest-stop-request.js와는 완전히 별개다 — 대안 경로/이미지 없이 거리·유가(fuelType
 * 스코프)가 붙은 평평한 목록만 받는다.
 *
 * 이 엔드포인트는 지오코딩을 하지 않으므로 목적지를 자유 검색어로 넘길 수 없다. 칩은 서버가 좌표를
 * 아는 이름(destinationName)을 넘기고, 직접 입력한 목적지는 place-search 후보 선택으로 얻은 좌표
 * (destinationLat/Lng)를 넘긴다.
 */
export function createRouteRestStopListRequest({ fetchImpl = fetch, onState = () => {} } = {}) {
    const tracker = createRequestTracker({ onState });

    async function load({ originLat, originLng, destinationLat, destinationLng, destinationName, fuelType } = {}) {
        const request = tracker.begin();
        const trimmedName = typeof destinationName === 'string' ? destinationName.trim() : '';
        const hasOrigin = Number.isFinite(originLat) && Number.isFinite(originLng);
        const hasDestinationCoordinates = Number.isFinite(destinationLat) && Number.isFinite(destinationLng);

        if (!hasOrigin || (trimmedName === '' && !hasDestinationCoordinates)) {
            request.emit({ status: 'error' });
            return;
        }

        request.emit({ status: 'loading' });

        const params = new globalThis.URLSearchParams();
        params.set('originLat', originLat);
        params.set('originLng', originLng);
        if (hasDestinationCoordinates) {
            params.set('destinationLat', destinationLat);
            params.set('destinationLng', destinationLng);
        }
        if (trimmedName !== '') {
            params.set('destinationName', trimmedName);
        }
        if (fuelType) {
            params.set('fuelType', fuelType);
        }

        try {
            const response = await fetchImpl(`${ROUTE_REST_STOP_LIST_ENDPOINT}?${params.toString()}`, {
                signal: request.signal
            });
            const body = await response.json();

            if (response.status === 404 && body?.code === 'NOT_FOUND') {
                request.emit({ status: 'not-found', message: body?.message });
                return;
            }

            if (body?.code === 'EXTERNAL_API_UNAVAILABLE') {
                request.emit({ status: 'external-unavailable' });
                return;
            }

            if (response.ok && body?.code === 'SUCCESS' && Array.isArray(body.data)) {
                request.emit({ status: 'success', restStops: body.data });
                return;
            }

            request.emit({ status: 'error' });
        } catch (error) {
            if (request.isAborted(error)) {
                return;
            }

            request.emit({ status: 'error' });
        }
    }

    return { invalidate: tracker.invalidate, load };
}

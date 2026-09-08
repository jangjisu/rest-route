import { createRequestTracker } from './request-tracker.js';

const NEARBY_ENDPOINT = '/api/rest-stops/nearby';

/**
 * "이름·거리로 찾기" 목록이 쓰는 유일한 API. 위치(originLat/originLng)·이름(name)·관심
 * 연료(fuelType) 전부 선택값이라, 있는 것만 쿼리스트링에 실어 보낸다 — 서버가 없는 값에 대응하는
 * 응답 필드를 null로 채워주므로 프런트는 그 필드가 없을 때 표시만 안 하면 된다.
 */
export function createFinderRestStopNearbyRequest({ fetchImpl = fetch, onState = () => {} } = {}) {
    const tracker = createRequestTracker({ onState });

    async function load({ originLat, originLng, name, interest } = {}) {
        const request = tracker.begin();
        request.emit({ status: 'loading' });

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
            params.set('fuelType', interest);
        }

        try {
            const query = params.toString();
            const response = await fetchImpl(
                query === '' ? NEARBY_ENDPOINT : `${NEARBY_ENDPOINT}?${query}`,
                { signal: request.signal }
            );

            // response.ok부터 확인한다 — HTTP 에러 응답의 본문이 JSON이 아닐 수 있어서(빈 응답,
            // 게이트웨이 에러 페이지 등), json() 파싱을 먼저 시도하면 실제로는 HTTP 상태 코드
            // 문제인데 invalid-response로 잘못 분류돼 상태 코드가 화면에 안 뜨는 문제가 있었다.
            if (!response.ok) {
                request.emit({ status: 'error', reason: 'http', httpStatus: response.status });
                return;
            }

            let body;
            try {
                body = await response.json();
            } catch {
                request.emit({ status: 'error', reason: 'invalid-response' });
                return;
            }

            if (body?.code !== 'SUCCESS') {
                request.emit({ status: 'error', reason: 'api', apiCode: body?.code });
                return;
            }
            if (!Array.isArray(body.data)) {
                request.emit({ status: 'error', reason: 'unexpected-shape' });
                return;
            }

            request.emit({ status: 'success', restStops: body.data });
        } catch (error) {
            if (request.isAborted(error)) {
                return;
            }

            request.emit({ status: 'error', reason: 'network' });
        }
    }

    return { invalidate: tracker.invalidate, load };
}

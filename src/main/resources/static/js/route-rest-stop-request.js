import { createRequestTracker } from './request-tracker.js';

const ROUTE_REST_STOPS_ENDPOINT = '/api/route-rest-stops';

export function createRouteRestStopRequest({ fetchImpl = fetch, onState = () => {} } = {}) {
    const tracker = createRequestTracker({ onState });

    async function load(originLatitude, originLongitude, destinationQuery, destinationLat, destinationLng, destinationName) {
        const request = tracker.begin();
        const query = typeof destinationQuery === 'string' ? destinationQuery.trim() : '';
        const hasOrigin = Number.isFinite(originLatitude) && Number.isFinite(originLongitude);
        const hasCoordinates = Number.isFinite(destinationLat) && Number.isFinite(destinationLng);

        if (!hasOrigin || (query === '' && !hasCoordinates)) {
            request.emit({ status: 'error' });
            return;
        }

        request.emit({ status: 'loading' });

        try {
            let url = `${ROUTE_REST_STOPS_ENDPOINT}?originLat=${originLatitude}&originLng=${originLongitude}`;
            if (hasCoordinates) {
                url += `&destinationLat=${destinationLat}&destinationLng=${destinationLng}`;
                if (destinationName) {
                    url += `&destinationName=${encodeURIComponent(destinationName)}`;
                }
            }
            if (!hasCoordinates) {
                url += `&destinationQuery=${encodeURIComponent(query)}`;
            }
            const response = await fetchImpl(url, { signal: request.signal });
            const body = await response.json();

            if (response.status === 404 && body?.code === 'NOT_FOUND') {
                request.emit({ status: 'not-found', message: body?.message });
                return;
            }

            if (body?.code === 'EXTERNAL_API_UNAVAILABLE') {
                request.emit({ status: 'external-unavailable' });
                return;
            }

            const hasValidData = body?.data !== null
                && typeof body?.data === 'object'
                && !Array.isArray(body.data);
            if (response.ok && body?.code === 'SUCCESS' && hasValidData) {
                request.emit({ status: 'success', data: body.data });
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

    function invalidate() {
        tracker.invalidate();
        onState({ status: 'idle' });
    }

    return { invalidate, load };
}

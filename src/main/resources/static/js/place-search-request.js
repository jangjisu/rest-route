import { createRequestTracker } from './request-tracker.js';

const PLACE_SEARCH_ENDPOINT = '/api/place-search';

export function createPlaceSearchRequest({ fetchImpl = fetch, onState = () => {} } = {}) {
    const tracker = createRequestTracker({ onState });

    async function load(query) {
        const request = tracker.begin();
        const normalizedQuery = typeof query === 'string' ? query.trim() : '';
        if (normalizedQuery === '') {
            request.emit({ status: 'error' });
            return;
        }

        request.emit({ status: 'loading' });

        try {
            const response = await fetchImpl(
                `${PLACE_SEARCH_ENDPOINT}?query=${encodeURIComponent(normalizedQuery)}`,
                { signal: request.signal }
            );
            const body = await response.json();

            if (body?.code === 'EXTERNAL_API_UNAVAILABLE') {
                request.emit({ status: 'external-unavailable' });
                return;
            }

            if (response.ok && body?.code === 'SUCCESS' && Array.isArray(body.data)) {
                request.emit({ status: 'success', candidates: body.data });
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

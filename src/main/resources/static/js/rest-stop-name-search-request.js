import { createRequestTracker } from './request-tracker.js';

const REST_STOP_SEARCH_ENDPOINT = '/api/rest-stops/search';

export function createRestStopNameSearchRequest({ fetchImpl = fetch, onState = () => {} } = {}) {
    const tracker = createRequestTracker({ onState });

    async function load(name) {
        const request = tracker.begin();
        const normalizedName = typeof name === 'string' ? name.trim() : '';
        if (normalizedName === '') {
            request.emit({ status: 'error' });
            return;
        }

        request.emit({ status: 'loading' });

        try {
            const response = await fetchImpl(
                `${REST_STOP_SEARCH_ENDPOINT}?name=${encodeURIComponent(normalizedName)}`,
                { signal: request.signal }
            );
            const body = await response.json();

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

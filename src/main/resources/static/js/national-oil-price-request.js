import { createRequestTracker } from './request-tracker.js';

const NATIONAL_OIL_PRICE_SUMMARY_ENDPOINT = '/api/national-oil-prices/summary';

export function createNationalOilPriceRequest({ fetchImpl = fetch, onState = () => {} } = {}) {
    const tracker = createRequestTracker({ onState });

    async function load() {
        const request = tracker.begin();
        request.emit({ status: 'loading' });

        try {
            const response = await fetchImpl(
                NATIONAL_OIL_PRICE_SUMMARY_ENDPOINT,
                { signal: request.signal }
            );
            const body = await response.json();

            if (body?.code === 'EXTERNAL_API_UNAVAILABLE') {
                request.emit({ status: 'external-unavailable' });
                return;
            }

            if (response.ok && body?.code === 'SUCCESS' && hasValidData(body)) {
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

function hasValidData(body) {
    return body?.data !== null
        && typeof body?.data === 'object'
        && !Array.isArray(body.data);
}

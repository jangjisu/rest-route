import { createRequestTracker } from './request-tracker.js';

const REST_STOPS_ENDPOINT = '/api/rest-stops';
const DETAIL_SECTION_REQUESTS = [
    { key: 'basicInfo', path: 'basic-info', required: true },
    { key: 'facilities', path: 'facilities', required: false },
    { key: 'oilInfo', path: 'oil-info', required: false },
    { key: 'foodMenu', path: 'foods', required: false },
    { key: 'salesRanking', path: 'sales-rankings', required: false },
    { key: 'events', path: 'events', required: false }
];

export function createRestStopDetailRequest({
    document: documentRef = globalThis.document,
    fetchImpl = fetch,
    onState = () => {}
} = {}) {
    const tracker = createRequestTracker({ onState });

    async function load(serviceAreaCode) {
        const request = tracker.begin();
        const normalizedServiceAreaCode = typeof serviceAreaCode === 'string'
            ? serviceAreaCode.trim()
            : '';

        if (normalizedServiceAreaCode === '') {
            request.emit({ status: 'error' });
            return;
        }

        request.emit({ status: 'loading' });

        try {
            const sectionResults = await Promise.all(DETAIL_SECTION_REQUESTS.map((section) => fetchDetailSection(
                fetchImpl,
                normalizedServiceAreaCode,
                section,
                request.signal
            )));
            const basicInfoResult = findSectionResult(sectionResults, 'basicInfo');

            if (basicInfoResult?.status === 'not-found') {
                request.emit({ status: 'not-found' });
                return;
            }

            if (basicInfoResult?.status === 'external-unavailable') {
                request.emit({ status: 'external-unavailable' });
                return;
            }

            if (basicInfoResult?.status === 'success') {
                const state = {
                    status: 'success',
                    data: buildDetailData(sectionResults)
                };

                if (hasExternalUnavailableSection(sectionResults)) {
                    state.externalUnavailable = true;
                }

                request.emit(state);
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

    async function refreshOilPrice(serviceAreaCode) {
        const normalizedServiceAreaCode = typeof serviceAreaCode === 'string'
            ? serviceAreaCode.trim()
            : '';

        if (normalizedServiceAreaCode === '') {
            return { status: 'error' };
        }

        try {
            const response = await fetchImpl(
                `${REST_STOPS_ENDPOINT}/${encodeURIComponent(normalizedServiceAreaCode)}/oil-price/refresh`,
                {
                    method: 'POST',
                    headers: csrfHeadersFrom(documentRef)
                }
            );
            const body = await response.json();

            if (response.status === 404 && body?.code === 'NOT_FOUND') {
                return { status: 'not-found' };
            }

            if (body?.code === 'EXTERNAL_API_UNAVAILABLE') {
                return { status: 'external-unavailable' };
            }

            const hasValidData = body?.data !== null
                && typeof body?.data === 'object'
                && !Array.isArray(body.data);
            if (response.ok && body?.code === 'SUCCESS' && hasValidData) {
                return { status: 'success', data: body.data };
            }

            return { status: 'error' };
        } catch {
            return { status: 'error' };
        }
    }

    return { invalidate: tracker.invalidate, load, refreshOilPrice };
}

function csrfHeadersFrom(documentRef) {
    const token = documentRef?.querySelector('meta[name="_csrf"]')?.content?.trim();
    const headerName = documentRef?.querySelector('meta[name="_csrf_header"]')?.content?.trim();
    if (!token || !headerName) {
        throw new Error('CSRF metadata is missing');
    }

    return { [headerName]: token };
}

async function fetchDetailSection(fetchImpl, serviceAreaCode, section, signal) {
    try {
        const response = await fetchImpl(
            `${REST_STOPS_ENDPOINT}/${encodeURIComponent(serviceAreaCode)}/${section.path}`,
            { signal }
        );
        const body = await response.json();

        if (response.status === 404 && body?.code === 'NOT_FOUND') {
            return sectionResult(section, 'not-found');
        }

        if (body?.code === 'EXTERNAL_API_UNAVAILABLE') {
            return sectionResult(section, 'external-unavailable');
        }

        if (response.ok && body?.code === 'SUCCESS' && hasValidApiData(body)) {
            return sectionResult(section, 'success', body.data);
        }

        return sectionResult(section, 'error');
    } catch (error) {
        if (error?.name === 'AbortError') {
            throw error;
        }

        return sectionResult(section, 'error');
    }
}

function sectionResult(section, status, data) {
    return {
        key: section.key,
        required: section.required,
        status,
        data
    };
}

function findSectionResult(sectionResults, key) {
    return sectionResults.find((result) => result.key === key);
}

function buildDetailData(sectionResults) {
    const basicInfo = findSectionResult(sectionResults, 'basicInfo')?.data ?? {};
    const facilities = optionalSectionData(sectionResults, 'facilities', {});
    const oilInfo = optionalSectionData(sectionResults, 'oilInfo', null);
    const foodMenu = optionalSectionData(sectionResults, 'foodMenu', emptyFoodMenu());
    const salesRanking = optionalSectionData(sectionResults, 'salesRanking', null);
    const events = optionalSectionData(sectionResults, 'events', emptyEvents()).events ?? [];

    const detail = {
        ...basicInfo,
        ...facilities,
        oilInfo,
        foodMenu,
        events
    };

    if (salesRanking !== null) {
        detail.salesRanking = salesRanking;
    }

    return detail;
}

function optionalSectionData(sectionResults, key, fallback) {
    const result = findSectionResult(sectionResults, key);
    return result?.status === 'success' ? result.data : fallback;
}

function emptyFoodMenu() {
    return { menus: [], sections: [] };
}

function emptyEvents() {
    return { events: [] };
}

function hasExternalUnavailableSection(sectionResults) {
    return sectionResults.some((result) => !result.required && result.status === 'external-unavailable');
}

function hasValidApiData(body) {
    return body?.data !== null
        && typeof body?.data === 'object'
        && !Array.isArray(body.data);
}

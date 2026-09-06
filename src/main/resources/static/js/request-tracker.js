/**
 * 요청 모듈들이 각자 반복해서 구현하던 "최신 요청만 반영" 배관. 같은 요청 모듈에 새 호출이
 * 들어오면 이전에 진행 중이던 fetch는 abort하고, 그 응답이 뒤늦게 와도 무시한다 —
 * requestId를 비교해서 판단한다. 각 요청 모듈은 begin()으로 이번 호출의 signal/emit만
 * 받아서, URL 구성과 응답 매핑에만 집중하면 된다.
 */
export function createRequestTracker({ onState = () => {} } = {}) {
    let currentRequestId = 0;
    let activeRequestController;

    function begin() {
        activeRequestController?.abort();
        activeRequestController = new globalThis.AbortController();
        const requestId = ++currentRequestId;

        return {
            signal: activeRequestController.signal,
            emit(state) {
                if (requestId === currentRequestId) {
                    onState(state);
                }
            },
            isAborted(error) {
                return error?.name === 'AbortError';
            }
        };
    }

    function invalidate() {
        currentRequestId += 1;
        activeRequestController?.abort();
        activeRequestController = undefined;
    }

    return { begin, invalidate };
}

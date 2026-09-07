// GeolocationPositionError 코드(스펙상 1/2/3 고정) — 거부(1)만 재시도해도 의미 없는 실패고,
// 나머지(2: 위치 계산 실패, 3: 시간초과)는 GPS 콜드스타트 등으로 흔히 겪는 일시적 실패다.
const PERMISSION_DENIED = 1;

/**
 * 위치 동의 팝업에서 쓰는 1회성 위치 요청. rest-stops-map.js의 resolveInitialCenter()와 달리
 * 조용히 서울로 폴백하지 않는다 — 팝업 UI가 허용/거부 결과를 그대로 구분해서 보여줘야 하기 때문.
 *
 * 첫 시도가 거부가 아닌 사유(위치 계산 실패·시간초과)로 실패하면 정확도를 높이고 시간을 더 줘서
 * 한 번 더 시도한다 — 권한은 이미 있는데 GPS가 콜드스타트라 8초 안에 못 잡는 경우가 흔해서,
 * 재시도 없이 바로 실패 처리하면 사용자에게는 이미 허용한 위치가 매번 안 되는 것처럼 보인다.
 */
export function requestCurrentPosition({ geolocation = globalThis.navigator?.geolocation } = {}) {
    if (!geolocation) {
        return Promise.resolve({ granted: false, reason: 'unsupported' });
    }

    return getPosition(geolocation, { enableHighAccuracy: false, maximumAge: 300000, timeout: 8000 }).catch(
        (reason) =>
            reason === 'denied'
                ? { granted: false, reason }
                : getPosition(geolocation, { enableHighAccuracy: true, maximumAge: 0, timeout: 15000 }).catch(
                      (retryReason) => ({ granted: false, reason: retryReason })
                  )
    );
}

function getPosition(geolocation, options) {
    return new Promise((resolve, reject) => {
        geolocation.getCurrentPosition(
            (position) =>
                resolve({
                    granted: true,
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude
                }),
            (error) => reject(error.code === PERMISSION_DENIED ? 'denied' : 'unavailable'),
            options
        );
    });
}

/**
 * 위치 동의 팝업에서 쓰는 1회성 위치 요청. rest-stops-map.js의 resolveInitialCenter()와 달리
 * 조용히 서울로 폴백하지 않는다 — 팝업 UI가 허용/거부 결과를 그대로 구분해서 보여줘야 하기 때문.
 */
export function requestCurrentPosition({ geolocation = globalThis.navigator?.geolocation } = {}) {
    return new Promise((resolve) => {
        if (!geolocation) {
            resolve({ granted: false, reason: 'unsupported' });
            return;
        }

        geolocation.getCurrentPosition(
            (position) => {
                resolve({
                    granted: true,
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude
                });
            },
            () => resolve({ granted: false, reason: 'denied' }),
            {
                enableHighAccuracy: false,
                maximumAge: 300000,
                timeout: 8000
            }
        );
    });
}

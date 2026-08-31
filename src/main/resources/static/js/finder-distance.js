const EARTH_RADIUS_METERS = 6371000;

/**
 * 두 좌표 사이의 대권 거리(하버사인)를 미터 단위로 계산한다.
 */
export function haversineDistanceMeters(origin, target) {
    const originLatRad = (origin.latitude * Math.PI) / 180;
    const targetLatRad = (target.latitude * Math.PI) / 180;
    const deltaLatRad = ((target.latitude - origin.latitude) * Math.PI) / 180;
    const deltaLngRad = ((target.longitude - origin.longitude) * Math.PI) / 180;

    const a = Math.sin(deltaLatRad / 2) ** 2
        + Math.cos(originLatRad) * Math.cos(targetLatRad) * Math.sin(deltaLngRad / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS_METERS * c;
}

/**
 * "이름·거리로 찾기" 목록(/api/rest-stops 응답)의 좌표 형식 — 문자열 xValue/yValue.
 */
export function xyValueCoordinateOf(restStop) {
    const latitude = Number.parseFloat(restStop.yValue);
    const longitude = Number.parseFloat(restStop.xValue);
    return Number.isFinite(latitude) && Number.isFinite(longitude) ? { latitude, longitude } : null;
}

/**
 * "목적지로 추천받기" 결과(RouteRestStopItem)의 좌표 형식 — 이미 숫자인 latitude/longitude.
 */
export function latLngCoordinateOf(item) {
    return Number.isFinite(item.latitude) && Number.isFinite(item.longitude)
        ? { latitude: item.latitude, longitude: item.longitude }
        : null;
}

/**
 * 목록에 origin 기준 거리(distanceMeters)를 채워 가까운 순으로 정렬한다. 좌표가 유효하지
 * 않은 항목은 결과에서 제외한다. coordinateOf로 응답 형식이 다른 목록(문자열 xValue/yValue vs
 * 숫자 latitude/longitude)을 모두 지원한다.
 */
export function sortByDistance(restStops, origin, coordinateOf = xyValueCoordinateOf) {
    return restStops
        .map((restStop) => {
            const coordinate = coordinateOf(restStop);
            if (!coordinate) {
                return null;
            }
            return {
                ...restStop,
                distanceMeters: haversineDistanceMeters(origin, coordinate)
            };
        })
        .filter((restStop) => restStop !== null)
        .sort((a, b) => a.distanceMeters - b.distanceMeters);
}

/**
 * 거리를 사람이 읽기 쉬운 문자열로 변환한다. 1km 미만은 m, 이상은 소수 첫째 자리 km.
 */
export function formatDistance(distanceMeters) {
    if (!Number.isFinite(distanceMeters)) {
        return '';
    }
    if (distanceMeters < 1000) {
        return `${Math.round(distanceMeters)}m`;
    }
    return `${(distanceMeters / 1000).toFixed(1)}km`;
}

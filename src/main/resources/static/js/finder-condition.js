/**
 * "목적지로 추천받기" 결과에 붙는 조건 필터 · 배지 판정. RouteRestStopItem 응답 필드를 그대로
 * 사용하며, "제일 저렴"/"평균보다 저렴" 판정은 fuelPriceTier(백엔드에서 이번에 조회된 휴게소들
 * 기준으로 계산)를 그대로 읽는다.
 */

export const CONDITION_FILTERS = [
    { key: 'LARGE_SIZE', label: '규모 큰 곳' },
    { key: 'HAS_FOOD', label: '먹거리 있는 곳' },
    { key: 'EV_CHARGER', label: 'EV 충전' },
    { key: 'CHEAP_FUEL', label: '유가 저렴한 곳' }
];

export function matchesFilter(item, filterKey) {
    switch (filterKey) {
        case 'LARGE_SIZE':
            return item.sizeTier === 'LARGE';
        case 'HAS_FOOD':
            return (item.comparisonSummary?.foodMenuCount ?? 0) > 0;
        case 'EV_CHARGER':
            return item.hasEvCharger === true;
        case 'CHEAP_FUEL':
            return item.fuelPriceTier === 'CHEAPEST' || item.fuelPriceTier === 'BELOW_AVERAGE';
        default:
            return false;
    }
}

/**
 * 선택된 필터 키 목록을 모두 만족하는 아이템만 남긴다(AND). 선택된 필터가 없으면 전체 통과.
 */
export function filterItems(items, selectedFilterKeys) {
    if (!selectedFilterKeys || selectedFilterKeys.length === 0) {
        return items;
    }
    return items.filter((item) => selectedFilterKeys.every((filterKey) => matchesFilter(item, filterKey)));
}

/**
 * 카드에 붙일 배지 목록. 조건에 안 맞으면 해당 배지를 만들지 않는다. key는 finder.css의
 * .finder-badge-<key 소문자-하이픈> 색상 클래스와 1:1로 대응한다.
 */
export function badgesFor(item) {
    const badges = [];

    if (item.sizeTier === 'LARGE') {
        badges.push({ key: 'SIZE_LARGE', label: '규모 큰 곳' });
    }
    if (item.topTrafficTier === true) {
        badges.push({ key: 'TOP_TRAFFIC', label: '이용량 상위 10%' });
    }
    if (item.fuelPriceTier === 'CHEAPEST') {
        badges.push({ key: 'FUEL_CHEAPEST', label: '제일 저렴' });
    } else if (item.fuelPriceTier === 'BELOW_AVERAGE') {
        badges.push({ key: 'FUEL_BELOW_AVERAGE', label: '평균보다 저렴' });
    }
    if (item.hasEvCharger === true) {
        badges.push({ key: 'EV_CHARGER', label: 'EV 충전' });
    }
    if ((item.comparisonSummary?.foodMenuCount ?? 0) > 0) {
        badges.push({ key: 'HAS_FOOD', label: '먹거리' });
    }

    return badges;
}

/**
 * "이름·거리로 찾기" 연료 선택 팝업에서 고르는 관심 항목 4개.
 */
export const INTEREST_OPTIONS = [
    { key: 'EV', label: 'EV' },
    { key: 'GASOLINE', label: '휘발유' },
    { key: 'DIESEL', label: '경유' },
    { key: 'LPG', label: 'LPG' }
];

const FUEL_INTEREST_LABEL_BY_KEY = {
    GASOLINE: '휘발유',
    DIESEL: '경유',
    LPG: 'LPG'
};

/**
 * "이름·거리로 찾기" 목록(/api/rest-stops/nearby 응답) 카드에 붙일 배지 목록. 규모·이용량·볼거리·
 * 이벤트는 항상 계산되고, 마지막 하나(EV 충전 개수 또는 유가)는 고른 관심 항목에 한해서만 붙는다.
 */
export function nearbyBadgesFor(item, interest) {
    const badges = [];

    if (item.sizeTier === 'LARGE') {
        badges.push({ key: 'SIZE_LARGE', label: '규모 큰 곳' });
    }
    if (item.topTrafficTier === true) {
        badges.push({ key: 'TOP_TRAFFIC', label: '이용량 상위 10%' });
    }
    if (item.hasTheme === true) {
        badges.push({ key: 'HAS_THEME', label: '볼거리 있음' });
    }
    if (item.hasEvent === true) {
        badges.push({ key: 'HAS_EVENT', label: '이벤트 진행중' });
    }
    if (interest === 'EV' && Number.isFinite(item.evChargerCount) && item.evChargerCount > 0) {
        badges.push({ key: 'EV_COUNT', label: `EV 충전 ${item.evChargerCount}대` });
    } else if (FUEL_INTEREST_LABEL_BY_KEY[interest] && item.fuelBelowAverage === true) {
        badges.push({ key: 'FUEL_BELOW_AVERAGE', label: `${FUEL_INTEREST_LABEL_BY_KEY[interest]} 평균보다 저렴` });
    }

    return badges;
}

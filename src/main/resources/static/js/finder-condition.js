/**
 * "이름·거리로 찾기"(mode1)와 "목적지로 추천받기"(mode2)의 배지·필터 판정. 두 화면은 서로 다른
 * 응답 필드를 쓰고(mode1: nearby API, mode2: route-rest-stops/list API) 색상도 완전히 분리돼 있어
 * 판정 함수도 섞지 않는다.
 */

/**
 * 위치 동의 팝업 다음에 뜨는 연료/EV 선택 팝업의 선택지 4개. mode1·mode2가 같은 팝업 컴포넌트를
 * 공유하므로 여기 하나만 둔다.
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

/**
 * "목적지로 추천받기" 목록(/api/route-rest-stops/list 응답) 카드에 붙일 배지 목록. 규모·이용량은
 * 항상 계산되고, 마지막 하나(EV 충전 대수 또는 유가 등급)는 고른 관심 항목에 한해서만 붙는다.
 * 유가 등급은 이미 백엔드가 선택한 유종 하나로 스코프해서 내려주므로 여기서는 CHEAPEST/BELOW_AVERAGE를
 * 그대로 문구로 옮기기만 한다.
 */
export function mode2BadgesFor(item, interest) {
    const badges = [];

    if (item.sizeTier === 'LARGE') {
        badges.push({ key: 'SIZE_LARGE', label: '규모 큰 곳' });
    }
    if (item.topTrafficTier === true) {
        badges.push({ key: 'TOP_TRAFFIC', label: '이용량 상위 10%' });
    }
    if (interest === 'EV') {
        if (Number.isFinite(item.evChargerCount) && item.evChargerCount > 0) {
            badges.push({ key: 'EV_CHARGER', label: `EV 충전 ${item.evChargerCount}대` });
        }
    } else if (FUEL_INTEREST_LABEL_BY_KEY[interest]) {
        if (item.fuelPriceTier === 'CHEAPEST') {
            badges.push({ key: 'FUEL_CHEAPEST', label: '제일 저렴' });
        } else if (item.fuelPriceTier === 'BELOW_AVERAGE') {
            badges.push({ key: 'FUEL_BELOW_AVERAGE', label: '평균보다 저렴' });
        }
    }

    return badges;
}

/**
 * "목적지로 추천받기" 조건 필터 칩 구성. 규모는 항상 뜨고, 나머지 한 자리는 배지와 똑같이 관심
 * 항목에 따라 EV 충전 또는 유가 저렴한 곳만 뜬다(둘 다 뜨는 일은 없다). "유가 저렴한 곳" 하나가
 * 제일 저렴/평균보다 저렴을 모두 매칭한다 — 별도로 "제일 저렴" 필터는 없다.
 */
export function mode2ConditionFilters(interest) {
    const filters = [{ key: 'LARGE_SIZE', label: '규모 큰 곳' }];
    if (interest === 'EV') {
        filters.push({ key: 'EV_CHARGER', label: 'EV 충전' });
    } else if (FUEL_INTEREST_LABEL_BY_KEY[interest]) {
        filters.push({ key: 'CHEAP_FUEL', label: '유가 저렴한 곳' });
    }
    return filters;
}

export function mode2MatchesFilter(item, filterKey) {
    switch (filterKey) {
        case 'LARGE_SIZE':
            return item.sizeTier === 'LARGE';
        case 'EV_CHARGER':
            return Number.isFinite(item.evChargerCount) && item.evChargerCount > 0;
        case 'CHEAP_FUEL':
            return item.fuelPriceTier === 'CHEAPEST' || item.fuelPriceTier === 'BELOW_AVERAGE';
        default:
            return false;
    }
}

/**
 * 선택된 필터 키 목록을 모두 만족하는 아이템만 남긴다(AND). 선택된 필터가 없으면 전체 통과.
 */
export function mode2FilterItems(items, selectedFilterKeys) {
    if (!selectedFilterKeys || selectedFilterKeys.length === 0) {
        return items;
    }
    return items.filter((item) => selectedFilterKeys.every((filterKey) => mode2MatchesFilter(item, filterKey)));
}

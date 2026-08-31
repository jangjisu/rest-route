/**
 * "목적지로 추천받기" 결과에 붙는 조건 필터 · 배지 판정. RouteRestStopItem 응답 필드를 그대로
 * 사용하며, "제일 저렴"/"유가 저렴한 곳" 판정만 fuelPriceTier(백엔드 계산)를 그대로 읽는다.
 */

const AMPLE_RESTROOM_TOILET_COUNT = 20;

export const CONDITION_FILTERS = [
    { key: 'LARGE_SIZE', label: '규모 큰 곳' },
    { key: 'AMPLE_RESTROOM', label: '화장실 넉넉한 곳' },
    { key: 'HAS_FOOD', label: '먹거리 있는 곳' },
    { key: 'EV_CHARGER', label: 'EV 충전' },
    { key: 'CHEAP_FUEL', label: '유가 저렴한 곳' }
];

function totalToiletCount(item) {
    const male = Number.isFinite(item.maleToiletCount) ? item.maleToiletCount : 0;
    const female = Number.isFinite(item.femaleToiletCount) ? item.femaleToiletCount : 0;
    return male + female;
}

export function matchesFilter(item, filterKey) {
    switch (filterKey) {
        case 'LARGE_SIZE':
            return item.sizeTier === 'LARGE';
        case 'AMPLE_RESTROOM':
            return totalToiletCount(item) >= AMPLE_RESTROOM_TOILET_COUNT;
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
 * 카드에 붙일 배지 목록. 조건에 안 맞으면 해당 배지를 만들지 않는다.
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
        badges.push({ key: 'FUEL_BELOW_AVERAGE', label: '저렴' });
    }
    if (item.hasEvCharger === true) {
        badges.push({ key: 'EV_CHARGER', label: 'EV 충전' });
    }
    if ((item.comparisonSummary?.foodMenuCount ?? 0) > 0) {
        badges.push({ key: 'HAS_FOOD', label: '먹거리' });
    }

    return badges;
}

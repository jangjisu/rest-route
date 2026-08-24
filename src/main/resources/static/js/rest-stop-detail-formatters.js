export const CONVENIENCE_FALLBACK = '편의시설 정보 없음';
const TOP_SALES_RANKING_LIMIT = 5;

export function isMissingValue(value) {
    return value === null
        || value === undefined
        || (typeof value !== 'string' && typeof value !== 'number')
        || (typeof value === 'string' && value.trim() === '');
}

export function formatText(value, fallback) {
    return isMissingValue(value) ? fallback : value;
}

export function formatAvailability(value) {
    if (value === true) {
        return '가능';
    }
    if (value === false) {
        return '불가';
    }
    return '알 수 없음';
}

export function formatFreightOperation(value) {
    if (value === true) {
        return '운영';
    }
    if (value === false) {
        return '미운영';
    }
    return '알 수 없음';
}

export function formatParkingCount(value) {
    const isNumber = typeof value === 'number' && Number.isInteger(value) && value >= 0;
    const isNumericString = typeof value === 'string' && /^\d+$/.test(value.trim());
    const normalizedValue = typeof value === 'string' ? value.trim() : value;
    return isNumber || isNumericString ? `${normalizedValue}대` : '정보 없음';
}

export function formatTotalParkingCount(compactCount, fullSizeCount, disabledCount) {
    const values = [compactCount, fullSizeCount, disabledCount].filter((value) => !isMissingValue(value));
    if (values.length === 0) {
        return '정보 없음';
    }
    const total = values.reduce((sum, value) => sum + Number(value), 0);
    return `${total.toLocaleString()}대`;
}

export function formatParkingBreakdown(fullSizeCount, compactCount, disabledCount) {
    if (isMissingValue(fullSizeCount) && isMissingValue(compactCount) && isMissingValue(disabledCount)) {
        return '정보 없음';
    }
    const toCount = (value) => (isMissingValue(value) ? '0대' : formatParkingCount(value));
    return `${toCount(fullSizeCount)} · ${toCount(compactCount)} · ${toCount(disabledCount)}`;
}

export function formatOilPrice(value) {
    return formatText(value, '정보 없음');
}

export function formatOperationTime(startTime, endTime) {
    if (isMissingValue(startTime) || isMissingValue(endTime)) {
        return '운영시간 정보 없음';
    }

    return `운영시간 ${String(startTime).trim()} ~ ${String(endTime).trim()}`;
}

export function hasFoodMenu(foodMenu) {
    return Boolean(foodMenu) && Array.isArray(foodMenu.menus) && foodMenu.menus.length > 0;
}

export function hasFoodSections(foodMenu) {
    return Boolean(foodMenu)
        && Array.isArray(foodMenu.sections)
        && foodMenu.sections.some((section) => Array.isArray(section?.menus) && section.menus.length > 0);
}

export function orderFoodMenus(menus) {
    if (!Array.isArray(menus)) {
        return [];
    }

    const recommended = menus.filter((menu) => menu?.recommended);
    const others = menus.filter((menu) => !menu?.recommended);
    return [...recommended, ...others];
}

export function formatFoodBadges(menu) {
    if (!menu || typeof menu !== 'object') {
        return [];
    }

    const badges = [];
    if (menu.recommended) {
        badges.push('추천');
    }
    if (menu.bestFood) {
        badges.push('베스트');
    }
    if (menu.premium) {
        badges.push('프리미엄');
    }

    if (!isMissingValue(menu.seasonLabel)) {
        badges.push(menu.seasonLabel);
    }

    return badges;
}

export function formatFoodCost(value) {
    if (isMissingValue(value)) {
        return '가격 정보 없음';
    }

    const text = String(value).trim();
    if (/^\d+$/.test(text)) {
        return `${Number(text).toLocaleString('ko-KR')}원`;
    }

    return text;
}

export function formatSalesRankingMonth(baseYearMonth) {
    const match = String(baseYearMonth).match(/^(\d{4})-(\d{2})$/);
    return match ? `${match[1]}년 ${match[2]}월 기준` : `${baseYearMonth} 기준`;
}

export function sortSalesRankingProducts(products) {
    return sortSalesRankingItems(products, 'productName');
}

export function sortSalesRankingStores(stores) {
    return sortSalesRankingItems(stores, 'storeName');
}

export function normalizeSalesRankingStoreName(value) {
    if (isMissingValue(value)) {
        return '';
    }

    let normalized = String(value);
    const firstAsterisk = normalized.indexOf('*');
    const secondAsterisk = firstAsterisk < 0 ? -1 : normalized.indexOf('*', firstAsterisk + 1);
    if (firstAsterisk >= 0 && secondAsterisk > firstAsterisk) {
        normalized = normalized.slice(firstAsterisk + 1, secondAsterisk);
    }

    normalized = normalized.replace(/^[0-9]+([.)-][0-9]+)?[.)-]?\s*/, '');
    const underscoreIndex = normalized.indexOf('_');
    if (underscoreIndex >= 0) {
        normalized = normalized.slice(underscoreIndex + 1);
    }

    return normalized.trim();
}

function sortSalesRankingItems(items, nameKey) {
    if (!Array.isArray(items)) {
        return [];
    }

    return items
        .filter((item) => Number.isInteger(item?.rank) && item.rank > 0 && !isMissingValue(item?.[nameKey]))
        .sort((first, second) => first.rank - second.rank)
        .slice(0, TOP_SALES_RANKING_LIMIT);
}

export const DATA_TAG_DEFINITIONS = [
    { key: 'food', label: '먹거리' },
    { key: 'parking', label: '주차' },
    { key: 'oil', label: '주유' },
    { key: 'theme', label: '테마' },
    { key: 'event', label: '이벤트' }
];

export function hasThemes(themes) {
    return Array.isArray(themes) && themes.length > 0;
}

export function hasEvents(events) {
    return Array.isArray(events) && events.length > 0;
}

export function hasOilConveniences(conveniences) {
    return Array.isArray(conveniences) && conveniences.length > 0;
}

export function hasOilInfo(oilInfo) {
    if (!oilInfo || typeof oilInfo !== 'object') {
        return false;
    }

    return !isMissingValue(oilInfo.gasolinePrice)
        || !isMissingValue(oilInfo.dieselPrice)
        || !isMissingValue(oilInfo.lpgPrice)
        || !isMissingValue(oilInfo.oilCompany)
        || !isMissingValue(oilInfo.telNo)
        || !isMissingValue(oilInfo.lastRefreshedAt)
        || hasOilConveniences(oilInfo.oilStationConveniences);
}

function isParkingCount(value) {
    if (typeof value === 'number') {
        return Number.isInteger(value) && value >= 0;
    }
    return typeof value === 'string' && /^\d+$/.test(value.trim());
}

export function hasParkingInfo(detail) {
    if (!detail || typeof detail !== 'object') {
        return false;
    }

    return isParkingCount(detail.compactCarParkingCount)
        || isParkingCount(detail.fullSizeCarParkingCount)
        || isParkingCount(detail.disabledParkingCount);
}

export function availableDataTags(detail) {
    if (!detail || typeof detail !== 'object') {
        return [];
    }

    const present = {
        food: hasFoodMenu(detail.foodMenu),
        parking: hasParkingInfo(detail),
        oil: hasOilInfo(detail.oilInfo),
        theme: hasThemes(detail.themes),
        event: hasEvents(detail.events)
    };

    return DATA_TAG_DEFINITIONS.filter((tag) => present[tag.key]);
}

export function hasRenderableRestStopDetail(detail) {
    if (!detail || typeof detail !== 'object') {
        return false;
    }

    const hasTextDetail = [detail.detailImageUrl, detail.address, detail.direction]
        .some((value) => !isMissingValue(value))
        || (Array.isArray(detail.convenienceFacilities) && detail.convenienceFacilities.length > 0)
        || typeof detail.hasMaintenance === 'boolean'
        || typeof detail.allowsTruckParking === 'boolean';
    const evChargerCount = Number(detail.evChargerCount);
    const hasEvCharger = Number.isFinite(evChargerCount) && evChargerCount > 0;
    const hasSalesRanking = !isMissingValue(detail.salesRanking?.baseYearMonth)
        && (sortSalesRankingStores(detail.salesRanking?.storeRankings).length > 0
            || sortSalesRankingProducts(detail.salesRanking?.products).length > 0);

    return hasTextDetail
        || hasParkingInfo(detail)
        || hasEvCharger
        || hasThemes(detail.themes)
        || hasEvents(detail.events)
        || hasFoodMenu(detail.foodMenu)
        || hasOilInfo(detail.oilInfo)
        || hasSalesRanking;
}

export function formatRefreshedAt(value) {
    if (isMissingValue(value)) {
        return '최근 갱신: 갱신 정보 없음';
    }

    const match = String(value).trim().match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    if (!match) {
        return '최근 갱신: 갱신 정보 없음';
    }

    const [, year, month, day, hour, minute] = match;
    return `최근 갱신: ${year}.${month}.${day} ${hour}:${minute}`;
}

import assert from 'node:assert/strict';
import test from 'node:test';

import {
    availableDataTags,
    CONVENIENCE_FALLBACK,
    formatAvailability,
    formatFoodBadges,
    formatFoodCost,
    formatFreightOperation,
    formatOilPrice,
    formatOperationTime,
    formatParkingCount,
    formatRefreshedAt,
    formatSalesRankingMonth,
    normalizeSalesRankingStoreName,
    formatText,
    formatToiletCount,
    hasEvents,
    hasFoodMenu,
    hasFoodSections,
    hasOilInfo,
    hasParkingInfo,
    hasRenderableRestStopDetail,
    hasRestroomInfo,
    hasThemes,
    isMissingValue,
    orderFoodMenus,
    sortSalesRankingProducts,
    sortSalesRankingStores
} from '../../main/resources/static/js/rest-stop-detail-formatters.js';

test('formatText returns the original value when present', () => {
    assert.equal(formatText('서울만남의광장휴게소', '이름 정보 없음'), '서울만남의광장휴게소');
});

test('formatText uses the exact fallback for missing name, route, address and direction values', () => {
    assert.equal(formatText(null, '이름 정보 없음'), '이름 정보 없음');
    assert.equal(formatText('', '노선 정보 없음'), '노선 정보 없음');
    assert.equal(formatText('   ', '주소 정보 없음'), '주소 정보 없음');
    assert.equal(formatText(undefined, '방향 정보 없음'), '방향 정보 없음');
    assert.equal(formatText({}, '이름 정보 없음'), '이름 정보 없음');
    assert.equal(isMissingValue(null), true);
});

test('CONVENIENCE_FALLBACK is the message shown when there are no convenience facilities', () => {
    assert.equal(CONVENIENCE_FALLBACK, '편의시설 정보 없음');
});

test('formatAvailability converts maintenance status and falls back for unknown values', () => {
    assert.equal(formatAvailability(true), '가능');
    assert.equal(formatAvailability(false), '불가');
    assert.equal(formatAvailability(null), '알 수 없음');
    assert.equal(formatAvailability(undefined), '알 수 없음');
});

test('formatFreightOperation converts freight operation status and falls back for unknown values', () => {
    assert.equal(formatFreightOperation(true), '운영');
    assert.equal(formatFreightOperation(false), '미운영');
    assert.equal(formatFreightOperation(null), '알 수 없음');
    assert.equal(formatFreightOperation(undefined), '알 수 없음');
});

test('formatParkingCount appends the unit and keeps zero as a valid count', () => {
    assert.equal(formatParkingCount(12), '12대');
    assert.equal(formatParkingCount('7'), '7대');
    assert.equal(formatParkingCount(0), '0대');
    assert.equal(formatParkingCount(null), '정보 없음');
    assert.equal(formatParkingCount(''), '정보 없음');
    assert.equal(formatParkingCount('   '), '정보 없음');
    assert.equal(formatParkingCount({}), '정보 없음');
    assert.equal(formatParkingCount(-1), '정보 없음');
    assert.equal(formatParkingCount(1.5), '정보 없음');
});

test('formatToiletCount appends the unit and keeps zero as a valid count', () => {
    assert.equal(formatToiletCount(12), '12칸');
    assert.equal(formatToiletCount('7'), '7칸');
    assert.equal(formatToiletCount(0), '0칸');
    assert.equal(formatToiletCount(null), '정보 없음');
    assert.equal(formatToiletCount(''), '정보 없음');
    assert.equal(formatToiletCount('   '), '정보 없음');
    assert.equal(formatToiletCount({}), '정보 없음');
    assert.equal(formatToiletCount(-1), '정보 없음');
    assert.equal(formatToiletCount(1.5), '정보 없음');
});

test('formatOilPrice keeps present price text and falls back for missing values', () => {
    assert.equal(formatOilPrice('1,699원'), '1,699원');
    assert.equal(formatOilPrice(null), '정보 없음');
    assert.equal(formatOilPrice('   '), '정보 없음');
});

test('formatOperationTime joins start and end times when both are present', () => {
    assert.equal(formatOperationTime('00:00', '24:00'), '운영시간 00:00 ~ 24:00');
    assert.equal(formatOperationTime('08:00', ''), '운영시간 정보 없음');
    assert.equal(formatOperationTime(null, '20:00'), '운영시간 정보 없음');
});

test('formatRefreshedAt formats ISO local date time for display', () => {
    assert.equal(formatRefreshedAt('2026-06-16T07:30:00'), '최근 갱신: 2026.06.16 07:30');
    assert.equal(formatRefreshedAt(null), '최근 갱신: 갱신 정보 없음');
    assert.equal(formatRefreshedAt('invalid'), '최근 갱신: 갱신 정보 없음');
});

test('hasFoodMenu is true only when there is at least one menu', () => {
    assert.equal(hasFoodMenu({ menus: [{ foodName: '우동' }] }), true);
    assert.equal(hasFoodMenu({ menus: [] }), false);
    assert.equal(hasFoodMenu(null), false);
    assert.equal(hasFoodMenu({}), false);
});

test('orderFoodMenus places recommended menus first while preserving order', () => {
    const menus = [
        { foodName: '국밥', recommended: false },
        { foodName: '우동', recommended: true },
        { foodName: '돈까스', recommended: false },
        { foodName: '비빔밥', recommended: true }
    ];

    assert.deepEqual(
        orderFoodMenus(menus).map((menu) => menu.foodName),
        ['우동', '비빔밥', '국밥', '돈까스']
    );
    assert.deepEqual(orderFoodMenus(null), []);
});

test('formatFoodCost adds thousands separator and won unit for numeric prices', () => {
    assert.equal(formatFoodCost('7000'), '7,000원');
    assert.equal(formatFoodCost('500'), '500원');
    assert.equal(formatFoodCost('시가'), '시가');
    assert.equal(formatFoodCost(null), '가격 정보 없음');
    assert.equal(formatFoodCost('   '), '가격 정보 없음');
});

test('formatSalesRankingMonth formats a monthly sales ranking label', () => {
    assert.equal(formatSalesRankingMonth('2026-06'), '2026년 06월 기준');
    assert.equal(formatSalesRankingMonth('2026/06'), '2026/06 기준');
});

test('sortSalesRankingProducts returns valid products in top-five rank order', () => {
    assert.deepEqual(
        sortSalesRankingProducts([
            { rank: 6, productName: '여섯번째' },
            { rank: 2, productName: '두번째' },
            { rank: 1, productName: '첫번째' },
            { rank: 0, productName: '잘못된 순위' },
            { rank: 3, productName: '세번째' },
            { rank: 4, productName: '네번째' },
            { rank: 5, productName: '다섯번째' },
            { rank: 7, productName: '일곱번째' },
            { rank: 8, productName: '' }
        ]).map((product) => product.productName),
        ['첫번째', '두번째', '세번째', '네번째', '다섯번째']
    );
});

test('sortSalesRankingStores returns valid stores in top-five rank order', () => {
    assert.deepEqual(
        sortSalesRankingStores([
            { rank: 2, storeName: '두번째 매장' },
            { rank: 1, storeName: '첫번째 매장' },
            { rank: 6, storeName: '여섯번째 매장' },
            { rank: 3, storeName: '세번째 매장' }
        ]).map((store) => store.storeName),
        ['첫번째 매장', '두번째 매장', '세번째 매장', '여섯번째 매장']
    );
});

test('normalizeSalesRankingStoreName removes source prefixes before displaying store names', () => {
    assert.equal(normalizeSalesRankingStoreName('H01_편의점'), '편의점');
    assert.equal(normalizeSalesRankingStoreName('1 편의점'), '편의점');
    assert.equal(normalizeSalesRankingStoreName('2-1 한식(편의점)'), '한식(편의점)');
    assert.equal(normalizeSalesRankingStoreName('03한식전문점'), '한식전문점');
    assert.equal(normalizeSalesRankingStoreName('13파스쿠찌'), '파스쿠찌');
    assert.equal(normalizeSalesRankingStoreName('인*CU편의점*인천'), 'CU편의점');
    assert.equal(normalizeSalesRankingStoreName('  하이샵  '), '하이샵');
});

test('normalizeSalesRankingStoreName keeps names without a recognized prefix', () => {
    assert.equal(normalizeSalesRankingStoreName('하이샵'), '하이샵');
    assert.equal(normalizeSalesRankingStoreName(''), '');
    assert.equal(normalizeSalesRankingStoreName(null), '');
});

test('formatFoodBadges uses backend seasonLabel instead of frontend season code mapping', () => {
    assert.deepEqual(
        formatFoodBadges({ recommended: true, bestFood: true, premium: true, season: 'S', seasonLabel: '여름' }),
        ['추천', '베스트', '프리미엄', '여름']
    );
    assert.deepEqual(formatFoodBadges({ season: 'S' }), []);
    assert.deepEqual(formatFoodBadges(null), []);
});

test('hasFoodSections detects backend grouped recommendation sections', () => {
    assert.equal(hasFoodSections({ sections: [{ key: 'recommended', menus: [{ foodName: '우동' }] }] }), true);
    assert.equal(hasFoodSections({ sections: [{ key: 'recommended', menus: [] }] }), false);
    assert.equal(hasFoodSections({ sections: [] }), false);
    assert.equal(hasFoodSections(null), false);
});

test('hasOilInfo detects any meaningful oil field', () => {
    assert.equal(hasOilInfo({ gasolinePrice: '1700' }), true);
    assert.equal(hasOilInfo({ oilStationConveniences: [{ name: '세차장' }] }), true);
    assert.equal(hasOilInfo({ gasolinePrice: null, dieselPrice: '', oilStationConveniences: [] }), false);
    assert.equal(hasOilInfo(null), false);
});

test('hasParkingInfo detects any non-missing parking count', () => {
    assert.equal(hasParkingInfo({ compactCarParkingCount: 10 }), true);
    assert.equal(hasParkingInfo({ fullSizeCarParkingCount: '0' }), true);
    assert.equal(hasParkingInfo({ disabledParkingCount: 3 }), true);
    assert.equal(hasParkingInfo({ compactCarParkingCount: null, fullSizeCarParkingCount: '없음' }), false);
    assert.equal(hasParkingInfo(null), false);
});

test('hasRestroomInfo detects any non-missing toilet count', () => {
    assert.equal(hasRestroomInfo({ maleToiletCount: 37 }), true);
    assert.equal(hasRestroomInfo({ femaleToiletCount: '0' }), true);
    assert.equal(hasRestroomInfo({ maleToiletCount: null, femaleToiletCount: null }), false);
    assert.equal(hasRestroomInfo(null), false);
});

test('hasThemes is true only when there is at least one theme', () => {
    assert.equal(hasThemes([{ name: '4계절 꽃이 있는 휴게소' }]), true);
    assert.equal(hasThemes([]), false);
    assert.equal(hasThemes(null), false);
    assert.equal(hasThemes(undefined), false);
});

test('hasEvents is true only when there is at least one event', () => {
    assert.equal(hasEvents([{ name: 'TEN+1 이벤트' }]), true);
    assert.equal(hasEvents([]), false);
    assert.equal(hasEvents(null), false);
    assert.equal(hasEvents(undefined), false);
});

test('availableDataTags returns present categories in fixed order', () => {
    const tags = availableDataTags({
        foodMenu: { menus: [{ menuName: '한우국밥' }] },
        compactCarParkingCount: 20,
        oilInfo: { gasolinePrice: '1700' },
        themes: [{ name: '4계절 꽃이 있는 휴게소' }],
        events: [{ name: 'TEN+1 이벤트' }]
    });
    assert.deepEqual(tags.map((tag) => tag.key), ['food', 'parking', 'oil', 'theme', 'event']);
    assert.deepEqual(tags.map((tag) => tag.label), ['먹거리', '주차', '주유', '테마', '이벤트']);
});

test('availableDataTags omits categories without data', () => {
    const tags = availableDataTags({
        foodMenu: { menus: [] },
        compactCarParkingCount: null,
        fullSizeCarParkingCount: null,
        disabledParkingCount: null,
        oilInfo: { gasolinePrice: '1700' },
        themes: [],
        events: []
    });
    assert.deepEqual(tags.map((tag) => tag.key), ['oil']);
});

test('availableDataTags returns empty array for missing detail', () => {
    assert.deepEqual(availableDataTags(null), []);
    assert.deepEqual(availableDataTags({}), []);
});

test('hasRenderableRestStopDetail is false when only rest_stop basic fields are present', () => {
    assert.equal(hasRenderableRestStopDetail({
        serviceAreaCode: 'A00001',
        unitName: '목감(서울)휴게소',
        routeName: '서해안선',
        xValue: '126.0000',
        yValue: '37.0000',
        stdRestCd: '000001',
        evChargerCount: 0,
        themes: [],
        events: [],
        foodMenu: { menus: [], sections: [] },
        oilInfo: null
    }), false);
});

test('hasRenderableRestStopDetail detects each type of displayed related detail', () => {
    const details = [
        { detailImageUrl: '/api/rest-stops/A00001/images/detail' },
        { address: '경기도 시흥시' },
        { direction: '서울' },
        { convenienceFacilities: ['수유실'] },
        { hasMaintenance: true },
        { allowsTruckParking: false },
        { compactCarParkingCount: 0 },
        { evChargerCount: 1 },
        { themes: [{ name: '테마' }] },
        { events: [{ name: '이벤트' }] },
        { foodMenu: { menus: [{ foodName: '우동' }] } },
        { oilInfo: { gasolinePrice: '1,800원' } },
        { salesRanking: { baseYearMonth: '2026-06', products: [{ rank: 1, productName: '우동' }] } }
    ];

    details.forEach((detail) => assert.equal(hasRenderableRestStopDetail(detail), true));
});

test('hasRenderableRestStopDetail ignores empty related detail containers', () => {
    assert.equal(hasRenderableRestStopDetail({
        detailImageUrl: ' ',
        address: null,
        direction: '',
        convenienceFacilities: [],
        hasMaintenance: null,
        allowsTruckParking: null,
        compactCarParkingCount: null,
        fullSizeCarParkingCount: null,
        disabledParkingCount: null,
        evChargerCount: 0,
        themes: [],
        events: [],
        foodMenu: { menus: [] },
        oilInfo: {},
        salesRanking: { baseYearMonth: null, storeRankings: [], products: [] }
    }), false);
});

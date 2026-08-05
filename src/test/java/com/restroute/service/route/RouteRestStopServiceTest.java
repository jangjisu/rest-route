package com.restroute.service.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.client.KakaoMapClient;
import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.client.response.KakaoDirectionsResponse.Road;
import com.restroute.client.response.KakaoDirectionsResponse.Route;
import com.restroute.client.response.KakaoDirectionsResponse.Section;
import com.restroute.client.response.KakaoDirectionsResponse.Summary;
import com.restroute.client.response.KakaoLocalSearchResponse;
import com.restroute.client.response.KakaoLocalSearchResponse.Document;
import com.restroute.controller.response.RouteRestStopResponse;
import com.restroute.controller.response.RouteRestStopResponse.AverageOilPrice;
import com.restroute.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.domain.RestFoodEntity;
import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.service.NationalOilPriceService;
import com.restroute.service.RestStopAggregateQueryService;
import com.restroute.service.RestStopQueryService;
import com.restroute.service.dto.RestStopAggregate;
import com.restroute.service.dto.RestStopRelatedInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 카카오 좌표조회/후보탐색(RouteRestStopCandidateFinder)은 실제 인스턴스를 협력자로 두고,
 * 이 클래스가 담당하는 연관정보 조합/응답 변환만 검증한다. 좌표조회/후보탐색 자체의
 * 실패 케이스는 RouteRestStopCandidateFinderTest가 담당한다.
 */
@ExtendWith(MockitoExtension.class)
class RouteRestStopServiceTest {

    private static final List<Double> VERTEXES = List.of(127.0, 37.0, 127.5, 37.5, 128.0, 38.0);

    @Mock
    private KakaoMapClient kakaoMapClient;

    @Mock
    private RestStopQueryService restStopQueryService;

    @Mock
    private RestStopAggregateQueryService restStopAggregateQueryService;

    private RouteRestStopComparisonSummaryService routeRestStopComparisonSummaryService;

    private RouteRestStopRecommendationTagService routeRestStopRecommendationTagService;

    @Mock
    private NationalOilPriceService nationalOilPriceService;

    private RouteRestStopService service;

    @BeforeEach
    void setUp() {
        lenient().when(nationalOilPriceService.getTodaySummary()).thenReturn(Optional.empty());
        stubRelatedInfoByCode(Map.of());
        routeRestStopComparisonSummaryService = new RouteRestStopComparisonSummaryService();
        routeRestStopRecommendationTagService = new RouteRestStopRecommendationTagService();
        RouteRestStopCandidateFinder finder = new RouteRestStopCandidateFinder(kakaoMapClient, restStopQueryService);
        service = new RouteRestStopService(
                finder,
                routeRestStopComparisonSummaryService,
                routeRestStopRecommendationTagService,
                nationalOilPriceService,
                restStopAggregateQueryService);
    }

    private void stubRelatedInfoByCode(Map<String, RestStopRelatedInfo> overridesByServiceAreaCode) {
        Map<String, RestStopAggregate> aggregates = new HashMap<>();
        overridesByServiceAreaCode.forEach((code, relatedInfo) ->
                aggregates.put(code, new RestStopAggregate(null, relatedInfo, false, false, false, false)));
        stubAggregates(aggregates);
    }

    private void stubAggregates(Map<String, RestStopAggregate> overridesByServiceAreaCode) {
        lenient()
                .doAnswer(invocation -> {
                    Collection<String> codes = invocation.getArgument(0);
                    Map<String, RestStopAggregate> result = new HashMap<>();
                    for (String code : codes) {
                        result.put(
                                code,
                                overridesByServiceAreaCode.getOrDefault(
                                        code,
                                        new RestStopAggregate(null, emptyRelatedInfo(), false, false, false, false)));
                    }
                    return result;
                })
                .when(restStopAggregateQueryService)
                .findByServiceAreaCodesAndAdminOverridden(any(), any());
    }

    private KakaoLocalSearchResponse searchResult(String x, String y, String placeName, String addressName) {
        return new KakaoLocalSearchResponse(List.of(new Document(x, y, placeName, addressName)));
    }

    private KakaoDirectionsResponse directions(int code, Summary summary, List<Double> vertexes) {
        Route route = new Route(code, summary, List.of(new Section(List.of(new Road(vertexes)))));
        return new KakaoDirectionsResponse(List.of(route));
    }

    private RestStopEntity restStop(String code, String name, String route, String lng, String lat) {
        RestStopEntity entity = mock(RestStopEntity.class);
        lenient().when(entity.getServiceAreaCode()).thenReturn(code);
        lenient().when(entity.getUnitName()).thenReturn(name);
        lenient().when(entity.getRouteName()).thenReturn(route);
        lenient().when(entity.getRouteNo()).thenReturn("0010");
        lenient().when(entity.getStdRestCd()).thenReturn(code + "-FOOD");
        lenient().when(entity.getXValue()).thenReturn(lng);
        lenient().when(entity.getYValue()).thenReturn(lat);
        return entity;
    }

    private HighwayServiceAreaInfoEntity parking(String compact, String fullSize, String disabled) {
        HighwayServiceAreaInfoEntity entity = mock(HighwayServiceAreaInfoEntity.class);
        lenient().when(entity.getCompactCarParkingCount()).thenReturn(compact);
        lenient().when(entity.getFullSizeCarParkingCount()).thenReturn(fullSize);
        lenient().when(entity.getDisabledParkingCount()).thenReturn(disabled);
        return entity;
    }

    private RestOilEntity oilConvenience(String standardRestCode, String convenienceName) {
        RestOilEntity entity = mock(RestOilEntity.class);
        lenient().when(entity.getStandardRestCode()).thenReturn(standardRestCode);
        lenient().when(entity.getConvenienceName()).thenReturn(convenienceName);
        return entity;
    }

    private RestOilPriceEntity oilPrice(String gasoline, String diesel, String lpg) {
        RestOilPriceEntity entity = mock(RestOilPriceEntity.class);
        lenient().when(entity.getGasolinePrice()).thenReturn(gasoline);
        lenient().when(entity.getDieselPrice()).thenReturn(diesel);
        lenient().when(entity.getLpgPrice()).thenReturn(lpg);
        return entity;
    }

    private RestStopDetailEntity detail(String convenience, String maintenanceYn, String truckSaYn) {
        RestStopDetailEntity entity = mock(RestStopDetailEntity.class);
        lenient().when(entity.getConvenience()).thenReturn(convenience);
        lenient().when(entity.getMaintenanceYn()).thenReturn(maintenanceYn);
        lenient().when(entity.getTruckSaYn()).thenReturn(truckSaYn);
        return entity;
    }

    @Test
    @DisplayName("경로 1km 이내 휴게소만 경로 순서대로 반환하고, 잘못된 좌표는 건너뛴다")
    void success_filtersAndOrders() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));

        RestStopEntity near0 = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity near1 = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        RestStopEntity near2 = restStop("C", "C휴게소", "경부선", "128.0001", "38.0001");
        RestStopEntity far = restStop("C", "C휴게소", "중부선", "130.0", "40.0");
        RestStopEntity blank = restStop("D", "D", "x", "127.0", "   ");
        RestStopEntity nonNumeric = restStop("E", "E", "x", "127.0", "abc");
        when(restStopQueryService.findAll()).thenReturn(List.of(near1, near0, near2, far, blank, nonNumeric));
        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(response.destination().name()).isEqualTo("부산역");
        assertThat(response.destination().latitude()).isEqualTo(35.0);
        assertThat(response.route().distanceMeters()).isEqualTo(100L);
        assertThat(response.route().durationSeconds()).isEqualTo(200L);
        assertThat(response.route().path()).hasSize(3);
        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::serviceAreaCode)
                .containsExactly("A", "B", "C");
        assertThat(response.restStops().get(0).distanceFromRouteMeters()).isLessThan(50L);
    }

    @Test
    @DisplayName("경로 휴게소는 매핑된 휴게소 코드로 hasEvCharger를 반환한다")
    void success_includesEvChargerFlagFromMappedCodes() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));
        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));
        stubAggregates(Map.of("A", new RestStopAggregate(null, emptyRelatedInfo(), true, false, false, false)));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(response.restStops())
                .singleElement()
                .extracting(RouteRestStopResponse.RouteRestStopItem::hasEvCharger)
                .isEqualTo(true);
    }

    @Test
    @DisplayName("경로 휴게소는 매핑된 휴게소 코드로 hasTheme/hasEvent를 반환한다")
    void success_includesThemeAndEventFlagsFromMappedCodes() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));
        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));
        stubAggregates(Map.of("A", new RestStopAggregate(null, emptyRelatedInfo(), false, false, true, true)));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(response.restStops()).singleElement().satisfies(item -> {
            assertThat(item.hasTheme()).isTrue();
            assertThat(item.hasEvent()).isTrue();
        });
    }

    @Test
    @DisplayName("경로 후보의 목록 이미지 코드를 한 번만 조회하고 이미지가 있는 항목에만 URL을 붙인다")
    void success_attachesListImageUrlsFromSingleBulkLookup() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));
        RestStopEntity first = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity second = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        when(restStopQueryService.findAll()).thenReturn(List.of(first, second));
        stubAggregates(Map.of("A", new RestStopAggregate(null, emptyRelatedInfo(), false, true, false, false)));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::listImageUrl)
                .containsExactly("/api/rest-stops/A/images/list", null);
        verify(restStopAggregateQueryService).findByServiceAreaCodesAndAdminOverridden(List.of("A", "B"), null);
    }

    @Test
    @DisplayName("전국 평균가 요약은 응답 필드로 노출하지 않고 유종별 평균 대비 차이값 계산에만 사용한다")
    void success_usesNationalOilPriceSummaryForPriceDiffsOnly() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));
        when(nationalOilPriceService.getTodaySummary())
                .thenReturn(Optional.of(NationalOilPriceSummary.of(
                        "2026.07.07",
                        AverageOilPrice.of("B027", "휘발유", "1,893원", "-4.19"),
                        AverageOilPrice.of("D047", "자동차용경유", "1,880원", "-4.51"),
                        AverageOilPrice.of("K015", "자동차용부탄", "1,135원", "+0.01"))));

        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestOilEntity oilConvenience = oilConvenience("OIL-A", "쉼터");
        RestOilPriceEntity oilPrice = oilPrice("1,850원", "1,900원", "1,135원");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));
        stubRelatedInfoByCode(Map.of(
                "A",
                relatedInfo(Optional.empty(), List.of(), List.of(oilConvenience), Optional.of(oilPrice), List.of())));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        RouteRestStopResponse.ComparisonSummary summary =
                response.restStops().get(0).comparisonSummary();
        assertThat(summary.gasolinePriceDiffFromAverage()).isEqualTo(-43);
        assertThat(summary.dieselPriceDiffFromAverage()).isEqualTo(20);
        assertThat(summary.lpgPriceDiffFromAverage()).isZero();
    }

    @Test
    @DisplayName("경로 결과에 유종별 최저가, 최대 주차, 먹거리와 시설 태그 및 요약을 추가한다")
    void success_addsComparisonSummaryAndRecommendationTags() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));

        RestStopEntity first = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity second = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        HighwayServiceAreaInfoEntity firstParking = parking("10", "5", "1");
        HighwayServiceAreaInfoEntity secondParking = parking("40", "20", "3");
        RestOilEntity firstOilConvenience = oilConvenience("OIL-A", "쉼터");
        RestOilEntity secondOilConvenience = oilConvenience("OIL-B", "쉼터");
        RestOilEntity thirdOilConvenience = oilConvenience("OIL-B", "샤워실");
        RestOilEntity fourthOilConvenience = oilConvenience("OIL-B", "수면실");
        RestOilPriceEntity firstOilPrice = oilPrice("1,700원", "1,500원", "1,200원");
        RestOilPriceEntity secondOilPrice = oilPrice("1,650원", "1,550원", "1,100원");
        RestFoodEntity firstFood = mock(RestFoodEntity.class);
        RestFoodEntity secondFood = mock(RestFoodEntity.class);
        RestFoodEntity thirdFood = mock(RestFoodEntity.class);
        when(restStopQueryService.findAll()).thenReturn(List.of(first, second));
        stubRelatedInfoByCode(Map.of(
                "A",
                relatedInfo(
                        Optional.empty(),
                        List.of(firstParking),
                        List.of(firstOilConvenience),
                        Optional.of(firstOilPrice),
                        List.of(firstFood)),
                "B",
                relatedInfo(
                        Optional.empty(),
                        List.of(secondParking),
                        List.of(secondOilConvenience, thirdOilConvenience, fourthOilConvenience),
                        Optional.of(secondOilPrice),
                        List.of(secondFood, thirdFood))));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        RouteRestStopResponse.RouteRestStopItem firstItem = response.restStops().get(0);
        RouteRestStopResponse.RouteRestStopItem secondItem =
                response.restStops().get(1);
        assertThat(firstItem.comparisonSummary().gasolinePrice()).isEqualTo("1,700원");
        assertThat(firstItem.comparisonSummary().dieselPrice()).isEqualTo("1,500원");
        assertThat(firstItem.comparisonSummary().lpgPrice()).isEqualTo("1,200원");
        assertThat(firstItem.comparisonSummary().totalParkingCount()).isEqualTo(16);
        assertThat(firstItem.comparisonSummary().foodMenuCount()).isEqualTo(1);
        assertThat(firstItem.comparisonSummary().facilityCount()).isEqualTo(1);
        assertThat(firstItem.recommendationTags())
                .extracting(RouteRestStopResponse.RecommendationTag::label)
                .containsExactly("경유 최저가", "먹거리 있음");

        assertThat(secondItem.comparisonSummary().gasolinePrice()).isEqualTo("1,650원");
        assertThat(secondItem.comparisonSummary().totalParkingCount()).isEqualTo(63);
        assertThat(secondItem.comparisonSummary().foodMenuCount()).isEqualTo(2);
        assertThat(secondItem.comparisonSummary().facilityCount()).isEqualTo(3);
        assertThat(secondItem.recommendationTags())
                .extracting(RouteRestStopResponse.RecommendationTag::label)
                .containsExactly("휘발유 최저가", "LPG 최저가", "주차장 큼", "먹거리 있음", "시설 많음");
    }

    @Test
    @DisplayName("상세 편의시설과 운영 flag를 시설 개수에 포함한다")
    void success_countsDetailConveniencesAndOperationFlagsAsFacilities() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));

        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopDetailEntity detail = detail("수유실/쉼터, 쉼터", "Y", "X");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));
        stubRelatedInfoByCode(
                Map.of("A", relatedInfo(Optional.of(detail), List.of(), List.of(), Optional.empty(), List.of())));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        RouteRestStopResponse.RouteRestStopItem item = response.restStops().get(0);
        assertThat(item.comparisonSummary().facilityCount()).isEqualTo(3);
        assertThat(item.recommendationTags())
                .extracting(RouteRestStopResponse.RecommendationTag::label)
                .containsExactly("시설 많음");
    }

    @Test
    @DisplayName("상세 편의시설 문자열이 없어도 운영 flag 시설 개수를 계산한다")
    void success_countsOperationFlagsWhenDetailConvenienceIsNull() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));

        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopDetailEntity detail = detail(null, "Y", "Y");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));
        stubRelatedInfoByCode(
                Map.of("A", relatedInfo(Optional.of(detail), List.of(), List.of(), Optional.empty(), List.of())));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        RouteRestStopResponse.RouteRestStopItem item = response.restStops().get(0);
        assertThat(item.comparisonSummary().facilityCount()).isEqualTo(2);
        assertThat(item.recommendationTags()).isEmpty();
    }

    @Test
    @DisplayName("가격과 주차 숫자를 해석할 수 없으면 최저가/주차 태그를 붙이지 않는다")
    void success_skipsComparisonTagsWhenNumbersAreUnavailable() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));

        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        HighwayServiceAreaInfoEntity parking =
                parking("", "없음", "999999999999999999999999999999999999999999999999999999999999999999");
        RestOilEntity oilConvenience = oilConvenience("OIL-A", "");
        RestOilPriceEntity oilPrice =
                oilPrice("", "무료", "999999999999999999999999999999999999999999999999999999999999999999");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));
        stubRelatedInfoByCode(Map.of(
                "A",
                relatedInfo(
                        Optional.empty(),
                        List.of(parking),
                        List.of(oilConvenience),
                        Optional.of(oilPrice),
                        List.of())));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        RouteRestStopResponse.RouteRestStopItem item = response.restStops().get(0);
        assertThat(item.comparisonSummary().totalParkingCount()).isNull();
        assertThat(item.comparisonSummary().foodMenuCount()).isZero();
        assertThat(item.comparisonSummary().facilityCount()).isZero();
        assertThat(item.recommendationTags()).isEmpty();
    }

    @Test
    @DisplayName("후보 탐색 단계(RouteRestStopCandidateFinder)에서 정해진 hasDirectionAlternative를 재계산 없이 그대로 응답에 전달한다")
    void passesThroughDirectionAlternativeFromCandidateFinder() {
        when(kakaoMapClient.searchKeyword("목포")).thenReturn(searchResult("126.4", "34.8", "목포역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "126.4,34.8"))
                .thenReturn(directions(0, new Summary(100L, 200L), List.of(127.0, 37.0)));

        RestStopEntity mokpo = restStop("A", "화성(목포)휴게소", "서해안선", "127.0001", "37.0001");
        RestStopEntity seoul = restStop("B", "화성(서울)휴게소", "서해안선", "127.0002", "37.0002");
        when(restStopQueryService.findAll()).thenReturn(List.of(mokpo, seoul));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "목포", null, null, null, 1000);

        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::unitName)
                .containsExactlyInAnyOrder("화성(목포)휴게소", "화성(서울)휴게소");
        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::hasDirectionAlternative)
                .containsExactly(true, true);
    }

    @Test
    @DisplayName("방향 라벨이 비어있거나 휴게소명이 없어도 경로 휴게소 조회를 유지한다")
    void malformedDirectionLabels_areHandledAsIndependentCandidates() {
        when(kakaoMapClient.searchKeyword("목적지")).thenReturn(searchResult("126.4", "34.8", "목적지", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "126.4,34.8"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));

        RestStopEntity blankDirection = restStop("A", "화성()휴게소", "서해안선", "127.5001", "37.5001");
        RestStopEntity unnamedNear = restStop("B", null, "서해안선", "127.0001", "37.0001");
        RestStopEntity unnamedFar = restStop("B", null, "서해안선", "127.005", "37.005");
        when(restStopQueryService.findAll()).thenReturn(List.of(blankDirection, unnamedFar, unnamedNear));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "목적지", null, null, null, 1000);

        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::unitName)
                .containsExactly(null, null, "화성()휴게소");
        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::hasDirectionAlternative)
                .containsExactly(false, false, false);
    }

    private RestStopRelatedInfo emptyRelatedInfo() {
        return relatedInfo(Optional.empty(), List.of(), List.of(), Optional.empty(), List.of());
    }

    private RestStopRelatedInfo relatedInfo(
            Optional<RestStopDetailEntity> detail,
            List<HighwayServiceAreaInfoEntity> infos,
            List<RestOilEntity> oilConveniences,
            Optional<RestOilPriceEntity> oilPrice,
            List<RestFoodEntity> foods) {
        return RestStopRelatedInfo.of(
                detail, infos, oilConveniences, Optional.empty(), oilPrice, foods, List.of(), List.of());
    }
}

package com.restroute.service.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.restroute.repository.RestStopRepository;
import com.restroute.service.NationalOilPriceService;
import com.restroute.service.RestStopEventQueryService;
import com.restroute.service.RestStopRelatedInfo;
import com.restroute.service.RestStopRelatedInfoQueryService;
import com.restroute.service.RestThemeQueryService;
import com.restroute.service.evcharger.EvChargerQueryService;
import com.restroute.service.image.RestStopImageQueryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteRestStopServiceTest {

    private static final List<Double> VERTEXES = List.of(127.0, 37.0, 127.5, 37.5, 128.0, 38.0);

    @Mock
    private KakaoMapClient kakaoMapClient;

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;

    private RouteRestStopComparisonSummaryService routeRestStopComparisonSummaryService;

    private RouteRestStopRecommendationTagService routeRestStopRecommendationTagService;

    @Mock
    private NationalOilPriceService nationalOilPriceService;

    @Mock
    private EvChargerQueryService evChargerQueryService;

    @Mock
    private RestStopImageQueryService restStopImageQueryService;

    @Mock
    private RestThemeQueryService restThemeQueryService;

    @Mock
    private RestStopEventQueryService restStopEventQueryService;

    private RouteRestStopService service;

    @BeforeEach
    void setUp() {
        lenient().when(nationalOilPriceService.getTodaySummary()).thenReturn(Optional.empty());
        lenient()
                .when(evChargerQueryService.findChargerMappedServiceAreaCodes(any()))
                .thenReturn(List.of());
        lenient()
                .when(restStopImageQueryService.findExistingServiceAreaCodes(any()))
                .thenReturn(Set.of());
        lenient()
                .when(restThemeQueryService.findThemeMappedServiceAreaCodes(any()))
                .thenReturn(List.of());
        lenient()
                .when(restStopEventQueryService.findActiveEventMappedServiceAreaCodes(any()))
                .thenReturn(List.of());
        stubRelatedInfoByCode(Map.of());
        routeRestStopComparisonSummaryService = new RouteRestStopComparisonSummaryService();
        routeRestStopRecommendationTagService = new RouteRestStopRecommendationTagService();
        service = new RouteRestStopService(
                kakaoMapClient,
                restStopRepository,
                routeRestStopComparisonSummaryService,
                routeRestStopRecommendationTagService,
                nationalOilPriceService,
                evChargerQueryService,
                restStopImageQueryService,
                restThemeQueryService,
                restStopEventQueryService,
                restStopRelatedInfoQueryService);
    }

    private void stubRelatedInfoByCode(Map<String, RestStopRelatedInfo> overridesByServiceAreaCode) {
        lenient()
                .doAnswer(invocation -> {
                    List<RestStopEntity> stops = invocation.getArgument(0);
                    Map<String, RestStopRelatedInfo> result = new HashMap<>();
                    for (RestStopEntity stop : stops) {
                        String code = stop.getServiceAreaCode();
                        result.put(code, overridesByServiceAreaCode.getOrDefault(code, emptyRelatedInfo()));
                    }
                    return result;
                })
                .when(restStopRelatedInfoQueryService)
                .findAllByRestStops(any());
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
    @DisplayName("목적지 검색 결과가 없으면 NotFound (빈 리스트/ null 모두)")
    void emptySearch_throwsNotFound() {
        when(kakaoMapClient.searchKeyword("없는곳")).thenReturn(new KakaoLocalSearchResponse(List.of()));
        assertThatThrownBy(() -> service.findRouteRestStops(37.0, 127.0, "없는곳", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.searchKeyword("널")).thenReturn(new KakaoLocalSearchResponse(null));
        assertThatThrownBy(() -> service.findRouteRestStops(37.0, 127.0, "널", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    @DisplayName("목적지 좌표를 해석하지 못하면 NotFound")
    void unparsableDestination_throwsNotFound() {
        when(kakaoMapClient.searchKeyword("경도없음")).thenReturn(searchResult(null, "35.0", "곳", null));
        assertThatThrownBy(() -> service.findRouteRestStops(37.0, 127.0, "경도없음", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.searchKeyword("위도없음")).thenReturn(searchResult("129.0", null, "곳", null));
        assertThatThrownBy(() -> service.findRouteRestStops(37.0, 127.0, "위도없음", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    @DisplayName("길찾기에 성공 경로가 없으면 NotFound (result_code!=0, routes 비어있음/null)")
    void noSuccessfulRoute_throwsNotFound() {
        when(kakaoMapClient.searchKeyword(anyString())).thenReturn(searchResult("129.0", "35.0", "부산", null));

        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(104, null, VERTEXES));
        assertThatThrownBy(() -> service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(new KakaoDirectionsResponse(List.of()));
        assertThatThrownBy(() -> service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(new KakaoDirectionsResponse(null));
        assertThatThrownBy(() -> service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }

    @Test
    @DisplayName("result_code별로 출발/도착/근접/기타 안내 메시지를 구분한다")
    void routeFailure_mapsMessageByResultCode() {
        when(kakaoMapClient.searchKeyword(anyString())).thenReturn(searchResult("129.0", "35.0", "부산", null));

        assertFailureMessage(105, "출발지 주변");
        assertFailureMessage(101, "출발지 주변");
        assertFailureMessage(106, "도착지 주변");
        assertFailureMessage(102, "도착지 주변");
        assertFailureMessage(104, "너무 가까워요");
        assertFailureMessage(1, "다시 확인");
    }

    private void assertFailureMessage(int resultCode, String expectedFragment) {
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(resultCode, null, VERTEXES));
        assertThatThrownBy(() -> service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class)
                .hasMessageContaining(expectedFragment);
    }

    @Test
    @DisplayName("경로 좌표가 없으면 NotFound")
    void emptyPolyline_throwsNotFound() {
        when(kakaoMapClient.searchKeyword(anyString())).thenReturn(searchResult("129.0", "35.0", "부산", null));
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(0, null, List.of()));

        assertThatThrownBy(() -> service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000))
                .isInstanceOf(RouteRestStopNotFoundException.class);
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
        when(restStopRepository.findAll()).thenReturn(List.of(near1, near0, near2, far, blank, nonNumeric));
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
        when(restStopRepository.findAll()).thenReturn(List.of(restStop));
        when(evChargerQueryService.findChargerMappedServiceAreaCodes(List.of("A")))
                .thenReturn(List.of("A"));

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
        when(restStopRepository.findAll()).thenReturn(List.of(restStop));
        when(restThemeQueryService.findThemeMappedServiceAreaCodes(List.of("A")))
                .thenReturn(List.of("A"));
        when(restStopEventQueryService.findActiveEventMappedServiceAreaCodes(List.of("A")))
                .thenReturn(List.of("A"));

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
        when(restStopRepository.findAll()).thenReturn(List.of(first, second));
        when(restStopImageQueryService.findExistingServiceAreaCodes(List.of("A", "B")))
                .thenReturn(Set.of("A"));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::listImageUrl)
                .containsExactly("/api/rest-stops/A/images/list", null);
        verify(restStopImageQueryService).findExistingServiceAreaCodes(List.of("A", "B"));
        verify(restStopImageQueryService, never()).findDetailImage(anyString());
        verify(restStopImageQueryService, never()).findListImage(anyString());
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
        when(restStopRepository.findAll()).thenReturn(List.of(restStop));
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
        when(restStopRepository.findAll()).thenReturn(List.of(first, second));
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
    @DisplayName("가장 가까운 도로 구간의 traffic_state로 인근 소통 상황을 채우고, 0이면 배지를 비운다")
    void success_addsNearbyTrafficFromNearestRoadSegment() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        Road jamRoad = new Road("경부선", 10L, 5L, 10, 1, List.of(127.0, 37.0));
        Road smoothRoad = new Road("경부선", 10L, 5L, 90, 4, List.of(127.5, 37.5));
        Road noInfoRoad = new Road("경부선", 10L, 5L, null, 0, List.of(128.0, 38.0));
        Route route =
                new Route(0, new Summary(100L, 200L), List.of(new Section(List.of(jamRoad, smoothRoad, noInfoRoad))));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(new KakaoDirectionsResponse(List.of(route)));

        RestStopEntity near0 = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity near1 = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        RestStopEntity near2 = restStop("C", "C휴게소", "경부선", "128.0001", "38.0001");
        when(restStopRepository.findAll()).thenReturn(List.of(near0, near1, near2));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(response.restStops().get(0).nearbyTraffic().key()).isEqualTo("jam");
        assertThat(response.restStops().get(0).nearbyTraffic().label()).isEqualTo("정체");
        assertThat(response.restStops().get(1).nearbyTraffic().key()).isEqualTo("smooth");
        assertThat(response.restStops().get(2).nearbyTraffic()).isNull();
    }

    @Test
    @DisplayName("상세 편의시설과 운영 flag를 시설 개수에 포함한다")
    void success_countsDetailConveniencesAndOperationFlagsAsFacilities() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));

        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopDetailEntity detail = detail("수유실/쉼터, 쉼터", "Y", "X");
        when(restStopRepository.findAll()).thenReturn(List.of(restStop));
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
        when(restStopRepository.findAll()).thenReturn(List.of(restStop));
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
        when(restStopRepository.findAll()).thenReturn(List.of(restStop));
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
    @DisplayName("상하행 휴게소가 모두 경로 반경에 있으면 둘 다 반환하고 방향 후보 flag를 표시한다")
    void sameRestStopPair_marksDirectionAlternatives() {
        when(kakaoMapClient.searchKeyword("목포")).thenReturn(searchResult("126.4", "34.8", "목포역", null));
        when(kakaoMapClient.getDirections("127.0,37.0", "126.4,34.8"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));

        RestStopEntity mokpo = restStop("A", "화성(목포)휴게소", "서해안선", "127.0001", "37.0001");
        RestStopEntity seoul = restStop("B", "화성(서울)휴게소", "서해안선", "127.0002", "37.0002");
        RestStopEntity next = restStop("C", "서산(목포)휴게소", "서해안선", "127.5001", "37.5001");
        when(restStopRepository.findAll()).thenReturn(List.of(seoul, mokpo, next));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "목포", null, null, null, 1000);

        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::unitName)
                .containsExactly("화성(서울)휴게소", "화성(목포)휴게소", "서산(목포)휴게소");
        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::hasDirectionAlternative)
                .containsExactly(true, true, false);
    }

    @Test
    @DisplayName("목적지 방향을 판단할 수 없어도 상하행 후보를 숨기지 않는다")
    void sameRestStopPair_keepsBothAlternativesWhenDestinationDirectionUnknown() {
        when(kakaoMapClient.searchKeyword("목적지")).thenReturn(searchResult("126.4", "34.8", null, null));
        when(kakaoMapClient.getDirections("127.0,37.0", "126.4,34.8"))
                .thenReturn(directions(0, new Summary(100L, 200L), VERTEXES));

        RestStopEntity nearer = restStop("A", "화성(서울)휴게소", "서해안선", "127.0001", "37.0001");
        RestStopEntity farther = restStop("B", "화성(목포)휴게소", "서해안선", "127.005", "37.005");
        when(restStopRepository.findAll()).thenReturn(List.of(farther, nearer));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "목적지", null, null, null, 1000);

        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::unitName)
                .containsExactly("화성(목포)휴게소", "화성(서울)휴게소");
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
        when(restStopRepository.findAll()).thenReturn(List.of(blankDirection, unnamedFar, unnamedNear));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "목적지", null, null, null, 1000);

        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::unitName)
                .containsExactly(null, null, "화성()휴게소");
        assertThat(response.restStops())
                .extracting(RouteRestStopResponse.RouteRestStopItem::hasDirectionAlternative)
                .containsExactly(false, false, false);
    }

    @Test
    @DisplayName("위도 또는 경도 좌표가 없으면 결과에서 제외한다")
    void invalidRestStopCoordinates_excluded() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산", null));
        when(kakaoMapClient.getDirections(anyString(), anyString()))
                .thenReturn(directions(0, new Summary(1L, 1L), VERTEXES));
        RestStopEntity nullLatitude = restStop("A", "A", "x", "127.0", null);
        RestStopEntity nullLongitude = restStop("B", "B", "x", null, "37.0");
        when(restStopRepository.findAll()).thenReturn(List.of(nullLatitude, nullLongitude));

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(response.restStops()).isEmpty();
    }

    @Test
    @DisplayName("summary가 null이면 거리/시간은 0, placeName이 비면 주소명을 이름으로 쓴다")
    void summaryNullAndAddressFallback() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "", "부산 우동"));
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(0, null, VERTEXES));
        when(restStopRepository.findAll()).thenReturn(List.of());

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(response.destination().name()).isEqualTo("부산 우동");
        assertThat(response.route().distanceMeters()).isZero();
        assertThat(response.route().durationSeconds()).isZero();
        assertThat(response.restStops()).isEmpty();
    }

    @Test
    @DisplayName("목적지 좌표가 주어지면 지오코딩 없이 그 좌표로 경로를 계산한다")
    void destinationCoordinates_skipGeocoding() {
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(10L, 20L), VERTEXES));
        when(restStopRepository.findAll()).thenReturn(List.of());

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, null, 35.0, 129.0, "부산항", 1000);

        assertThat(response.destination().name()).isEqualTo("부산항");
        assertThat(response.destination().latitude()).isEqualTo(35.0);
        assertThat(response.destination().longitude()).isEqualTo(129.0);
        verify(kakaoMapClient, never()).searchKeyword(anyString());
    }

    @Test
    @DisplayName("목적지 좌표가 일부(경도만)면 query 지오코딩으로 폴백한다")
    void partialDestinationCoordinates_fallbackToQuery() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역", null));
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(0, null, VERTEXES));
        when(restStopRepository.findAll()).thenReturn(List.of());

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", 35.0, null, "이름", 1000);

        assertThat(response.destination().name()).isEqualTo("부산역");
    }

    @Test
    @DisplayName("목적지 좌표만 있고 이름이 없거나 비면 기본 이름을 쓴다")
    void destinationCoordinates_defaultName() {
        when(kakaoMapClient.getDirections(anyString(), anyString())).thenReturn(directions(0, null, VERTEXES));
        when(restStopRepository.findAll()).thenReturn(List.of());

        assertThat(service.findRouteRestStops(37.0, 127.0, null, 35.0, 129.0, null, 1000)
                        .destination()
                        .name())
                .isEqualTo("목적지");
        assertThat(service.findRouteRestStops(37.0, 127.0, null, 35.0, 129.0, "  ", 1000)
                        .destination()
                        .name())
                .isEqualTo("목적지");
    }

    @Test
    @DisplayName("summary 값이 null이면 거리/시간 0으로 처리한다")
    void summaryWithNullValues() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산", null));
        when(kakaoMapClient.getDirections(anyString(), anyString()))
                .thenReturn(directions(0, new Summary(null, null), VERTEXES));
        when(restStopRepository.findAll()).thenReturn(List.of());

        RouteRestStopResponse response = service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000);

        assertThat(response.route().distanceMeters()).isZero();
        assertThat(response.route().durationSeconds()).isZero();
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

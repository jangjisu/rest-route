package com.restroute.route.service;

import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.restroute.common.client.KakaoMapClient;
import com.restroute.common.client.response.KakaoDirectionsResponse;
import com.restroute.common.client.response.KakaoDirectionsResponse.Road;
import com.restroute.common.client.response.KakaoDirectionsResponse.Route;
import com.restroute.common.client.response.KakaoDirectionsResponse.Section;
import com.restroute.common.client.response.KakaoDirectionsResponse.Summary;
import com.restroute.common.client.response.KakaoLocalSearchResponse;
import com.restroute.common.client.response.KakaoLocalSearchResponse.Document;
import com.restroute.evcharger.service.EvChargerQueryService;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.service.NationalOilPriceService;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.SizeTier;
import com.restroute.reststop.service.RestStopAggregateQueryService;
import com.restroute.reststop.service.RestStopQueryService;
import com.restroute.reststop.service.dto.RestStopAggregate;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import com.restroute.route.controller.response.FuelPriceTier;
import com.restroute.route.controller.response.RouteRestStopListItemResponse;
import com.restroute.route.controller.response.RouteRestStopResponse.AverageOilPrice;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.route.dto.FuelType;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RouteRestStopListQueryServiceTest {

    private static final List<Double> VERTEXES = List.of(127.0, 37.0, 127.5, 37.5, 128.0, 38.0);

    @Mock
    private KakaoMapClient kakaoMapClient;

    @Mock
    private RestStopQueryService restStopQueryService;

    @Mock
    private RestStopAggregateQueryService restStopAggregateQueryService;

    @Mock
    private EvChargerQueryService evChargerQueryService;

    @Mock
    private NationalOilPriceService nationalOilPriceService;

    private RouteRestStopListQueryService service;

    @BeforeEach
    void setUp() {
        lenient().when(evChargerQueryService.findActiveChargerCounts(any())).thenReturn(Map.of());
        stubAggregates(Map.of());
        service = new RouteRestStopListQueryService(
                new RouteResolverService(kakaoMapClient),
                restStopQueryService,
                new RouteCoordinateReducer(),
                new RouteRestStopMatcher(),
                restStopAggregateQueryService,
                evChargerQueryService,
                nationalOilPriceService,
                new QueriedOilPriceStatsCalculator(),
                new RouteRestStopFuelTierCalculator());
    }

    private void stubAggregates(Map<String, RestStopAggregate> overridesByServiceAreaCode) {
        lenient()
                .doAnswer(invocation -> {
                    List<RestStopEntity> restStops = invocation.getArgument(0);
                    Map<String, RestStopAggregate> result = new HashMap<>();
                    for (RestStopEntity restStop : restStops) {
                        String code = restStop.getServiceAreaCode();
                        result.put(
                                code,
                                overridesByServiceAreaCode.getOrDefault(
                                        code,
                                        new RestStopAggregate(
                                                null,
                                                emptyRelatedInfo(),
                                                false,
                                                false,
                                                false,
                                                false,
                                                null,
                                                null,
                                                false,
                                                null)));
                    }
                    return result;
                })
                .when(restStopAggregateQueryService)
                .findByRestStopsAndAdminOverridden(any(), any());
    }

    private RestStopRelatedInfo emptyRelatedInfo() {
        return RestStopRelatedInfo.of(
                Optional.empty(),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                List.of(),
                List.of());
    }

    private KakaoLocalSearchResponse searchResult(String x, String y, String placeName) {
        return new KakaoLocalSearchResponse(List.of(new Document(x, y, placeName, null)));
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

    @Test
    @DisplayName("경로 반경 안 휴게소를 출발지 기준 거리 오름차순으로 정렬해 반환한다")
    void success_sortsByDistanceFromOrigin() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역"));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L, null), VERTEXES));
        RestStopEntity far = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        RestStopEntity near = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        when(restStopQueryService.findAll()).thenReturn(List.of(far, near));

        List<RouteRestStopListItemResponse> items =
                service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000, null);

        assertThat(items)
                .extracting(RouteRestStopListItemResponse::serviceAreaCode)
                .containsExactly("A", "B");
        assertThat(items.get(0).distanceMeters()).isLessThan(items.get(1).distanceMeters());
    }

    @Test
    @DisplayName("반경 안에 매칭된 휴게소가 없으면 빈 목록을 반환한다(NotFound 아님)")
    void success_returnsEmptyListWhenNoRestStopMatched() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역"));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L, null), VERTEXES));
        RestStopEntity far = restStop("A", "A휴게소", "경부선", "130.0", "40.0");
        when(restStopQueryService.findAll()).thenReturn(List.of(far));

        List<RouteRestStopListItemResponse> items =
                service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000, null);

        assertThat(items).isEmpty();
        verifyNoInteractions(restStopAggregateQueryService, evChargerQueryService);
    }

    @Test
    @DisplayName("규모·이용량 등급은 집계 조회 결과를 그대로 담는다")
    void success_includesSizeTierAndTopTrafficTierFromAggregate() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역"));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L, null), VERTEXES));
        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));
        stubAggregates(Map.of(
                "A",
                new RestStopAggregate(
                        null, emptyRelatedInfo(), false, false, false, false, null, null, true, SizeTier.LARGE)));

        List<RouteRestStopListItemResponse> items =
                service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000, null);

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.sizeTier()).isEqualTo(SizeTier.LARGE);
            assertThat(item.topTrafficTier()).isTrue();
        });
    }

    @Test
    @DisplayName("EV 충전 대수는 배치 조회 결과에서 가져오고, 0대거나 매핑이 없으면 null이다")
    void success_includesEvChargerCountFromBatchLookup() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역"));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L, null), VERTEXES));
        RestStopEntity hasCharger = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity noCharger = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        when(restStopQueryService.findAll()).thenReturn(List.of(hasCharger, noCharger));
        when(evChargerQueryService.findActiveChargerCounts(any())).thenReturn(Map.of("A", 3, "B", 0));

        List<RouteRestStopListItemResponse> items =
                service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000, null);

        assertThat(items)
                .filteredOn(item -> item.serviceAreaCode().equals("A"))
                .singleElement()
                .extracting(RouteRestStopListItemResponse::evChargerCount)
                .isEqualTo(3);
        assertThat(items)
                .filteredOn(item -> item.serviceAreaCode().equals("B"))
                .singleElement()
                .extracting(RouteRestStopListItemResponse::evChargerCount)
                .isNull();
    }

    @Test
    @DisplayName("fuelType이 없으면 전국 평균가를 조회하지 않고 유가 등급은 항상 null이다")
    void success_skipsFuelTierWhenFuelTypeAbsent() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역"));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L, null), VERTEXES));
        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop));

        List<RouteRestStopListItemResponse> items =
                service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000, null);

        assertThat(items)
                .singleElement()
                .extracting(RouteRestStopListItemResponse::fuelPriceTier)
                .isNull();
        verifyNoInteractions(nationalOilPriceService);
    }

    @Test
    @DisplayName("fuelType이 있으면 선택한 유종 하나만 비교해 유가 등급을 계산한다")
    void success_computesFuelTierScopedToSelectedFuelType() {
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(searchResult("129.0", "35.0", "부산역"));
        when(kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0"))
                .thenReturn(directions(0, new Summary(100L, 200L, null), VERTEXES));
        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity cheaperElsewhere = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        when(restStopQueryService.findAll()).thenReturn(List.of(restStop, cheaperElsewhere));
        RestOilPriceEntity oilPrice = RestOilPriceEntity.from(restOilPriceItem("000001", "테스트주유소"));
        ReflectionTestUtils.setField(oilPrice, "dieselPrice", "1,850원");
        RestOilPriceEntity cheaperOilPrice = RestOilPriceEntity.from(restOilPriceItem("000002", "테스트주유소2"));
        ReflectionTestUtils.setField(cheaperOilPrice, "dieselPrice", "1,700원");
        stubAggregates(Map.of(
                "A",
                new RestStopAggregate(
                        null,
                        RestStopRelatedInfo.of(
                                Optional.empty(),
                                List.of(),
                                List.of(),
                                Optional.empty(),
                                Optional.of(oilPrice),
                                List.of(),
                                List.of(),
                                List.of()),
                        false,
                        false,
                        false,
                        false,
                        null,
                        null,
                        false,
                        null),
                "B",
                new RestStopAggregate(
                        null,
                        RestStopRelatedInfo.of(
                                Optional.empty(),
                                List.of(),
                                List.of(),
                                Optional.empty(),
                                Optional.of(cheaperOilPrice),
                                List.of(),
                                List.of(),
                                List.of()),
                        false,
                        false,
                        false,
                        false,
                        null,
                        null,
                        false,
                        null)));
        when(nationalOilPriceService.getTodaySummary())
                .thenReturn(Optional.of(NationalOilPriceSummary.of(
                        "2026.07.07",
                        AverageOilPrice.of("B027", "휘발유", "1,893원", "-4.19"),
                        AverageOilPrice.of("D047", "자동차용경유", "1,900원", "-4.51"),
                        AverageOilPrice.of("K015", "자동차용부탄", "1,135원", "+0.01"))));

        List<RouteRestStopListItemResponse> items =
                service.findRouteRestStops(37.0, 127.0, "부산", null, null, null, 1000, FuelType.DIESEL);

        assertThat(items)
                .filteredOn(item -> item.serviceAreaCode().equals("A"))
                .singleElement()
                .extracting(RouteRestStopListItemResponse::fuelPriceTier)
                .isEqualTo(FuelPriceTier.BELOW_AVERAGE);
    }

    @Test
    @DisplayName("목적지 검색 결과가 없으면 기존과 동일하게 NotFound다")
    void destinationNotFound_throwsNotFound() {
        when(kakaoMapClient.searchKeyword("없는곳")).thenReturn(new KakaoLocalSearchResponse(List.of()));

        assertThatThrownBy(() -> service.findRouteRestStops(37.0, 127.0, "없는곳", null, null, null, 1000, null))
                .isInstanceOf(RouteRestStopNotFoundException.class);
    }
}

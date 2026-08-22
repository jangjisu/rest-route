package com.restroute.service.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.restroute.client.response.KakaoDirectionsResponse.Fare;
import com.restroute.client.response.KakaoDirectionsResponse.Road;
import com.restroute.client.response.KakaoDirectionsResponse.Section;
import com.restroute.client.response.KakaoDirectionsResponse.Summary;
import com.restroute.controller.response.RouteRestStopResponse.AverageOilPrice;
import com.restroute.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.controller.response.RouteRestStopResponse.RouteOption;
import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.domain.RestFoodEntity;
import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.service.RestStopAggregateQueryService;
import com.restroute.service.dto.RestStopAggregate;
import com.restroute.service.dto.RestStopRelatedInfo;
import com.restroute.service.route.dto.ResolvedRoute.RouteGeometry;
import com.restroute.service.route.dto.RoutePath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteOptionAssemblyServiceTest {

    private static final List<Double> VERTEXES = List.of(127.0, 37.0, 127.5, 37.5, 128.0, 38.0);

    @Mock
    private RestStopAggregateQueryService restStopAggregateQueryService;

    private RouteOptionAssemblyService service;

    @BeforeEach
    void setUp() {
        stubRelatedInfoByCode(Map.of());
        service = new RouteOptionAssemblyService(
                new RouteRestStopMatchingService(),
                restStopAggregateQueryService,
                new RouteRestStopComparisonSummaryService(),
                new RouteRestStopRecommendationTagService());
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
                    List<RestStopEntity> restStops = invocation.getArgument(0);
                    Map<String, RestStopAggregate> result = new HashMap<>();
                    for (RestStopEntity restStop : restStops) {
                        String code = restStop.getServiceAreaCode();
                        result.put(
                                code,
                                overridesByServiceAreaCode.getOrDefault(
                                        code,
                                        new RestStopAggregate(null, emptyRelatedInfo(), false, false, false, false)));
                    }
                    return result;
                })
                .when(restStopAggregateQueryService)
                .findByRestStopsAndAdminOverridden(any(), any());
    }

    private RouteGeometry geometry(Summary summary, List<Double> vertexes) {
        return RouteGeometry.of(RoutePath.from(List.of(new Section(List.of(new Road(vertexes)))), 1000L), summary);
    }

    private RestStopEntity restStop(String code, String name, String route, String lng, String lat) {
        RestStopEntity entity = mock(RestStopEntity.class);
        lenient().when(entity.getServiceAreaCode()).thenReturn(code);
        lenient().when(entity.getUnitName()).thenReturn(name);
        lenient().when(entity.getRouteName()).thenReturn(route);
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

    @Test
    void assemble_buildsRouteOptionWithSummaryAndMatchedRestStops() {
        RestStopEntity near = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");

        List<RouteOption> routes = service.assemble(
                List.of(geometry(new Summary(100L, 200L, null), VERTEXES)),
                List.of(near),
                1000,
                Optional.empty());

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).routeIndex()).isZero();
        assertThat(routes.get(0).summary().distanceMeters()).isEqualTo(100L);
        assertThat(routes.get(0).summary().durationSeconds()).isEqualTo(200L);
        assertThat(routes.get(0).restStops())
                .extracting(item -> item.serviceAreaCode())
                .containsExactly("A");
    }

    @Test
    void assemble_includesEvThemeEventFlagsFromAggregates() {
        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        stubAggregates(Map.of("A", new RestStopAggregate(null, emptyRelatedInfo(), true, false, true, true)));

        List<RouteOption> routes = service.assemble(
                List.of(geometry(new Summary(100L, 200L, null), VERTEXES)),
                List.of(restStop),
                1000,
                Optional.empty());

        var item = routes.get(0).restStops().get(0);
        assertThat(item.hasEvCharger()).isTrue();
        assertThat(item.hasTheme()).isTrue();
        assertThat(item.hasEvent()).isTrue();
    }

    @Test
    void assemble_attachesListImageUrlFromSingleBulkLookup() {
        RestStopEntity first = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity second = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        stubAggregates(Map.of("A", new RestStopAggregate(null, emptyRelatedInfo(), false, true, false, false)));

        List<RouteOption> routes = service.assemble(
                List.of(geometry(new Summary(100L, 200L, null), VERTEXES)),
                List.of(first, second),
                1000,
                Optional.empty());

        assertThat(routes.get(0).restStops())
                .extracting(item -> item.listImageUrl())
                .containsExactly("/api/rest-stops/A/images/list", null);
        verify(restStopAggregateQueryService, times(1)).findByRestStopsAndAdminOverridden(any(), isNull());
    }

    @Test
    void assemble_usesNationalOilPriceSummaryForPriceDiffsOnly() {
        RestStopEntity restStop = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestOilEntity oilConvenience = oilConvenience("OIL-A", "쉼터");
        RestOilPriceEntity oilPrice = oilPrice("1,850원", "1,900원", "1,135원");
        stubRelatedInfoByCode(Map.of(
                "A",
                relatedInfo(Optional.empty(), List.of(), List.of(oilConvenience), Optional.of(oilPrice), List.of())));
        Optional<NationalOilPriceSummary> summary = Optional.of(NationalOilPriceSummary.of(
                "2026.07.07",
                AverageOilPrice.of("B027", "휘발유", "1,893원", "-4.19"),
                AverageOilPrice.of("D047", "자동차용경유", "1,880원", "-4.51"),
                AverageOilPrice.of("K015", "자동차용부탄", "1,135원", "+0.01")));

        List<RouteOption> routes = service.assemble(
                List.of(geometry(new Summary(100L, 200L, null), VERTEXES)), List.of(restStop), 1000, summary);

        var comparisonSummary = routes.get(0).restStops().get(0).comparisonSummary();
        assertThat(comparisonSummary.gasolinePriceDiffFromAverage()).isEqualTo(-43);
        assertThat(comparisonSummary.dieselPriceDiffFromAverage()).isEqualTo(20);
        assertThat(comparisonSummary.lpgPriceDiffFromAverage()).isZero();
    }

    @Test
    void assemble_addsRecommendationTagsAcrossCandidates() {
        RestStopEntity first = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity second = restStop("B", "B휴게소", "경부선", "127.5001", "37.5001");
        HighwayServiceAreaInfoEntity firstParking = parking("10", "5", "1");
        HighwayServiceAreaInfoEntity secondParking = parking("40", "20", "3");
        RestOilEntity firstOilConvenience = oilConvenience("OIL-A", "쉼터");
        RestOilEntity secondOilConvenience = oilConvenience("OIL-B", "쉼터");
        RestOilPriceEntity firstOilPrice = oilPrice("1,700원", "1,500원", "1,200원");
        RestOilPriceEntity secondOilPrice = oilPrice("1,650원", "1,550원", "1,100원");
        RestFoodEntity firstFood = mock(RestFoodEntity.class);
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
                        List.of(secondOilConvenience),
                        Optional.of(secondOilPrice),
                        List.of())));

        List<RouteOption> routes = service.assemble(
                List.of(geometry(new Summary(100L, 200L, null), VERTEXES)),
                List.of(first, second),
                1000,
                Optional.empty());

        var firstItem = routes.get(0).restStops().get(0);
        var secondItem = routes.get(0).restStops().get(1);
        assertThat(firstItem.recommendationTags())
                .extracting(tag -> tag.label())
                .containsExactly("경유 최저가", "먹거리 있음");
        assertThat(secondItem.recommendationTags())
                .extracting(tag -> tag.label())
                .containsExactly("휘발유 최저가", "LPG 최저가", "주차장 큼");
    }

    @Test
    void assemble_zeroesTollFareWhenFarePresentButTollNull() {
        List<RouteOption> routes = service.assemble(
                List.of(geometry(new Summary(100L, 200L, new Fare(null)), VERTEXES)),
                List.of(),
                1000,
                Optional.empty());

        assertThat(routes.get(0).summary().tollFareWon()).isZero();
    }

    @Test
    void assemble_zeroesDistanceAndDurationWhenSummaryIsNull() {
        List<RouteOption> routes =
                service.assemble(List.of(geometry(null, VERTEXES)), List.of(), 1000, Optional.empty());

        assertThat(routes.get(0).summary().distanceMeters()).isZero();
        assertThat(routes.get(0).summary().durationSeconds()).isZero();
    }

    @Test
    void assemble_matchesEachAlternativeRouteIndependently_andQueriesAggregatesOnce() {
        List<Double> routeBVertexes = List.of(129.0, 39.0, 129.5, 39.5);
        RestStopEntity nearRouteA = restStop("A", "A휴게소", "경부선", "127.0001", "37.0001");
        RestStopEntity nearRouteB = restStop("B", "B휴게소", "동해선", "129.0001", "39.0001");

        List<RouteOption> routes = service.assemble(
                List.of(
                        geometry(new Summary(100L, 200L, new Fare(1000)), VERTEXES),
                        geometry(new Summary(150L, 300L, new Fare(0)), routeBVertexes)),
                List.of(nearRouteA, nearRouteB),
                1000,
                Optional.empty());

        assertThat(routes).hasSize(2);
        assertThat(routes.get(0).routeIndex()).isZero();
        assertThat(routes.get(0).summary().tollFareWon()).isEqualTo(1000L);
        assertThat(routes.get(0).restStops()).extracting(item -> item.serviceAreaCode()).containsExactly("A");
        assertThat(routes.get(1).routeIndex()).isEqualTo(1);
        assertThat(routes.get(1).summary().tollFareWon()).isZero();
        assertThat(routes.get(1).restStops()).extracting(item -> item.serviceAreaCode()).containsExactly("B");
        verify(restStopAggregateQueryService, times(1)).findByRestStopsAndAdminOverridden(any(), any());
    }
}

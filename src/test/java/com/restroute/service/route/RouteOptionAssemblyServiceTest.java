package com.restroute.service.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.restroute.client.response.KakaoDirectionsResponse.Fare;
import com.restroute.client.response.KakaoDirectionsResponse.Summary;
import com.restroute.controller.response.RouteRestStopResponse.AverageOilPrice;
import com.restroute.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.controller.response.RouteRestStopResponse.RouteOption;
import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.reststopcontent.domain.RestFoodEntity;
import com.restroute.service.RestStopAggregateQueryService;
import com.restroute.service.dto.RestStopAggregate;
import com.restroute.service.dto.RestStopRelatedInfo;
import com.restroute.service.route.dto.ResolvedRoute.RouteGeometry;
import com.restroute.service.route.dto.RouteCandidate;
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

    @Mock
    private RestStopAggregateQueryService restStopAggregateQueryService;

    private RouteOptionAssemblyService service;

    @BeforeEach
    void setUp() {
        stubRelatedInfoByCode(Map.of());
        service = new RouteOptionAssemblyService(
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

    private RouteGeometry geometry(Summary summary) {
        return RouteGeometry.of(RoutePath.of(List.of(), List.of()), summary);
    }

    private RouteRestStopItem item(String code, double lat, double lng) {
        return RouteRestStopItem.of(code, code + "휴게소", "경부선", lat, lng, 100L);
    }

    private RestStopEntity restStop(String code) {
        RestStopEntity entity = mock(RestStopEntity.class);
        lenient().when(entity.getServiceAreaCode()).thenReturn(code);
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
    void attachDetails_buildsRouteOptionWithSummaryAndMatchedRestStops() {
        RestStopEntity near = restStop("A");
        RouteCandidate candidate =
                new RouteCandidate(0, geometry(new Summary(100L, 200L, null)), List.of(item("A", 37.0001, 127.0001)));

        List<RouteOption> routes = service.attachDetails(List.of(candidate), List.of(near), Optional.empty());

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).routeIndex()).isZero();
        assertThat(routes.get(0).summary().distanceMeters()).isEqualTo(100L);
        assertThat(routes.get(0).summary().durationSeconds()).isEqualTo(200L);
        assertThat(routes.get(0).restStops())
                .extracting(RouteRestStopItem::serviceAreaCode)
                .containsExactly("A");
    }

    @Test
    void attachDetails_includesEvThemeEventFlagsFromAggregates() {
        RestStopEntity restStop = restStop("A");
        RouteCandidate candidate =
                new RouteCandidate(0, geometry(new Summary(100L, 200L, null)), List.of(item("A", 37.0001, 127.0001)));
        stubAggregates(Map.of("A", new RestStopAggregate(null, emptyRelatedInfo(), true, false, true, true)));

        List<RouteOption> routes = service.attachDetails(List.of(candidate), List.of(restStop), Optional.empty());

        var resultItem = routes.get(0).restStops().get(0);
        assertThat(resultItem.hasEvCharger()).isTrue();
        assertThat(resultItem.hasTheme()).isTrue();
        assertThat(resultItem.hasEvent()).isTrue();
    }

    @Test
    void attachDetails_attachesListImageUrlFromSingleBulkLookup() {
        RestStopEntity first = restStop("A");
        RestStopEntity second = restStop("B");
        RouteCandidate candidate = new RouteCandidate(
                0,
                geometry(new Summary(100L, 200L, null)),
                List.of(item("A", 37.0001, 127.0001), item("B", 37.5001, 127.5001)));
        stubAggregates(Map.of("A", new RestStopAggregate(null, emptyRelatedInfo(), false, true, false, false)));

        List<RouteOption> routes = service.attachDetails(List.of(candidate), List.of(first, second), Optional.empty());

        assertThat(routes.get(0).restStops())
                .extracting(RouteRestStopItem::listImageUrl)
                .containsExactly("/api/rest-stops/A/images/list", null);
        verify(restStopAggregateQueryService, times(1)).findByRestStopsAndAdminOverridden(any(), isNull());
    }

    @Test
    void attachDetails_usesNationalOilPriceSummaryForPriceDiffsOnly() {
        RestStopEntity restStop = restStop("A");
        RouteCandidate candidate =
                new RouteCandidate(0, geometry(new Summary(100L, 200L, null)), List.of(item("A", 37.0001, 127.0001)));
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

        List<RouteOption> routes = service.attachDetails(List.of(candidate), List.of(restStop), summary);

        var comparisonSummary = routes.get(0).restStops().get(0).comparisonSummary();
        assertThat(comparisonSummary.gasolinePriceDiffFromAverage()).isEqualTo(-43);
        assertThat(comparisonSummary.dieselPriceDiffFromAverage()).isEqualTo(20);
        assertThat(comparisonSummary.lpgPriceDiffFromAverage()).isZero();
    }

    @Test
    void attachDetails_addsRecommendationTagsAcrossCandidates() {
        RestStopEntity first = restStop("A");
        RestStopEntity second = restStop("B");
        RouteCandidate candidate = new RouteCandidate(
                0,
                geometry(new Summary(100L, 200L, null)),
                List.of(item("A", 37.0001, 127.0001), item("B", 37.5001, 127.5001)));
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

        List<RouteOption> routes = service.attachDetails(List.of(candidate), List.of(first, second), Optional.empty());

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
    void attachDetails_zeroesTollFareWhenFarePresentButTollNull() {
        RouteCandidate candidate = new RouteCandidate(0, geometry(new Summary(100L, 200L, new Fare(null))), List.of());

        List<RouteOption> routes = service.attachDetails(List.of(candidate), List.of(), Optional.empty());

        assertThat(routes.get(0).summary().tollFareWon()).isZero();
    }

    @Test
    void attachDetails_zeroesDistanceAndDurationWhenSummaryIsNull() {
        RouteCandidate candidate = new RouteCandidate(0, geometry(null), List.of());

        List<RouteOption> routes = service.attachDetails(List.of(candidate), List.of(), Optional.empty());

        assertThat(routes.get(0).summary().distanceMeters()).isZero();
        assertThat(routes.get(0).summary().durationSeconds()).isZero();
    }

    @Test
    void attachDetails_buildsIndependentRouteOptionsPerCandidate_andQueriesAggregatesOnce() {
        RestStopEntity nearRouteA = restStop("A");
        RestStopEntity nearRouteB = restStop("B");
        RouteCandidate candidateA = new RouteCandidate(
                0, geometry(new Summary(100L, 200L, new Fare(1000))), List.of(item("A", 37.0001, 127.0001)));
        RouteCandidate candidateB = new RouteCandidate(
                1, geometry(new Summary(150L, 300L, new Fare(0))), List.of(item("B", 39.0001, 129.0001)));

        List<RouteOption> routes = service.attachDetails(
                List.of(candidateA, candidateB), List.of(nearRouteA, nearRouteB), Optional.empty());

        assertThat(routes).hasSize(2);
        assertThat(routes.get(0).routeIndex()).isZero();
        assertThat(routes.get(0).summary().tollFareWon()).isEqualTo(1000L);
        assertThat(routes.get(0).restStops())
                .extracting(RouteRestStopItem::serviceAreaCode)
                .containsExactly("A");
        assertThat(routes.get(1).routeIndex()).isEqualTo(1);
        assertThat(routes.get(1).summary().tollFareWon()).isZero();
        assertThat(routes.get(1).restStops())
                .extracting(RouteRestStopItem::serviceAreaCode)
                .containsExactly("B");
        verify(restStopAggregateQueryService, times(1)).findByRestStopsAndAdminOverridden(any(), any());
    }
}

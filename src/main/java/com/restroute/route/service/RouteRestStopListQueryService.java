package com.restroute.route.service;

import com.restroute.common.client.response.KakaoDirectionsResponse;
import com.restroute.evcharger.service.EvChargerQueryService;
import com.restroute.evcharger.service.util.CoordinateDistanceCalculator;
import com.restroute.oilprice.service.NationalOilPriceService;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.RestStopAggregateQueryService;
import com.restroute.reststop.service.RestStopQueryService;
import com.restroute.reststop.service.dto.RestStopAggregate;
import com.restroute.route.controller.response.FuelPriceTier;
import com.restroute.route.controller.response.RouteRestStopListItemResponse;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.route.dto.FuelTypeSelection;
import com.restroute.route.service.RouteResolverService.RawRouteResult;
import com.restroute.route.service.dto.QueriedOilPriceStats;
import com.restroute.route.service.dto.ResolvedRoute.RouteGeometry;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * finder "목적지로 추천받기" 전용 경로 휴게소 조회. 목적지 해석·경로 매칭은 기존 route 도메인 내부
 * 부품(RouteResolverService/RouteCoordinateReducer/RouteRestStopMatcher)을 그대로 재사용하되, 응답
 * 조립은 {@link RouteOptionAssemblyService}를 거치지 않고 이 서비스가 직접 한다 — 지도 화면과 계약을
 * 공유하지 않기 위해서다(도메인 문서 참고). finder는 첫 번째 경로 후보만 쓰므로 대안 경로는 계산하지
 * 않는다.
 */
@Service
@RequiredArgsConstructor
public class RouteRestStopListQueryService {

    private final RouteResolverService routeResolverService;
    private final RestStopQueryService restStopQueryService;
    private final RouteCoordinateReducer routeCoordinateReducer;
    private final RouteRestStopMatcher routeRestStopMatcher;
    private final RestStopAggregateQueryService restStopAggregateQueryService;
    private final EvChargerQueryService evChargerQueryService;
    private final NationalOilPriceService nationalOilPriceService;
    private final QueriedOilPriceStatsCalculator queriedOilPriceStatsCalculator;
    private final RouteRestStopFuelTierCalculator routeRestStopFuelTierCalculator;

    public List<RouteRestStopListItemResponse> findRouteRestStops(
            double originLatitude,
            double originLongitude,
            String destinationQuery,
            Double destinationLatitude,
            Double destinationLongitude,
            String destinationName,
            int radiusMeters,
            FuelTypeSelection fuelSelection) {
        RawRouteResult raw = routeResolverService.resolveDestinationAndRoute(
                originLatitude,
                originLongitude,
                destinationQuery,
                destinationLatitude,
                destinationLongitude,
                destinationName);
        RouteGeometry firstRoute = firstReducedRoute(raw.routes());

        List<RestStopEntity> allRestStops = restStopQueryService.findAll();
        List<RouteRestStopItem> matched = routeRestStopMatcher.match(firstRoute.path(), radiusMeters, allRestStops);
        if (matched.isEmpty()) {
            return List.of();
        }

        Map<String, RestStopAggregate> aggregatesByServiceAreaCode = aggregatesFor(matched, allRestStops);
        Map<String, Integer> evChargerCountsByServiceAreaCode =
                evChargerQueryService.findActiveChargerCounts(aggregatesByServiceAreaCode.keySet());
        QueriedOilPriceStats queriedOilPriceStats =
                queriedOilPriceStatsCalculator.calculate(aggregatesByServiceAreaCode.values());
        Optional<NationalOilPriceSummary> nationalOilPriceSummary =
                fuelSelection.wantsFuelPriceInfo() ? nationalOilPriceService.getTodaySummary() : Optional.empty();

        return matched.stream()
                .map(item -> toItem(
                        item,
                        originLatitude,
                        originLongitude,
                        aggregatesByServiceAreaCode.get(item.serviceAreaCode()),
                        evChargerCountsByServiceAreaCode,
                        fuelSelection,
                        queriedOilPriceStats,
                        nationalOilPriceSummary))
                .sorted(Comparator.comparingDouble(RouteRestStopListItemResponse::distanceMeters))
                .toList();
    }

    /**
     * finder는 대안 경로를 쓰지 않으므로 첫 번째 경로만 축소한다. 폴리라인이 비면(축소 후 빈 경로)
     * 기존과 동일하게 NotFound로 끝낸다.
     */
    private RouteGeometry firstReducedRoute(List<KakaoDirectionsResponse.Route> rawRoutes) {
        return rawRoutes.stream()
                .map(routeCoordinateReducer::reduce)
                .filter(geometry -> !geometry.path().isEmpty())
                .findFirst()
                .orElseThrow(RouteRestStopNotFoundException::emptyRoutePath);
    }

    private Map<String, RestStopAggregate> aggregatesFor(
            List<RouteRestStopItem> matched, List<RestStopEntity> allRestStops) {
        Set<String> serviceAreaCodes =
                matched.stream().map(RouteRestStopItem::serviceAreaCode).collect(Collectors.toSet());
        List<RestStopEntity> selected = allRestStops.stream()
                .filter(restStop -> serviceAreaCodes.contains(restStop.getServiceAreaCode()))
                .toList();
        return restStopAggregateQueryService.findByRestStopsAndAdminOverridden(selected, null);
    }

    private RouteRestStopListItemResponse toItem(
            RouteRestStopItem item,
            double originLatitude,
            double originLongitude,
            RestStopAggregate aggregate,
            Map<String, Integer> evChargerCountsByServiceAreaCode,
            FuelTypeSelection fuelSelection,
            QueriedOilPriceStats queriedOilPriceStats,
            Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        double distanceMeters =
                CoordinateDistanceCalculator.meters(originLatitude, originLongitude, item.latitude(), item.longitude());
        Integer evChargerCount = evChargerCount(evChargerCountsByServiceAreaCode.get(item.serviceAreaCode()));
        FuelPriceTier fuelPriceTier = routeRestStopFuelTierCalculator.tier(
                fuelSelection, aggregate.relatedInfo().oilPrice(), queriedOilPriceStats, nationalOilPriceSummary);

        return RouteRestStopListItemResponse.of(
                item.serviceAreaCode(),
                item.unitName(),
                item.routeName(),
                distanceMeters,
                aggregate.sizeTier(),
                aggregate.topTrafficTier(),
                evChargerCount,
                fuelPriceTier);
    }

    private Integer evChargerCount(Integer count) {
        if (count == null || count <= 0) {
            return null;
        }
        return count;
    }
}

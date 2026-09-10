package com.restroute.route.service;

import com.restroute.evcharger.service.EvChargerQueryService;
import com.restroute.evcharger.service.util.CoordinateDistanceCalculator;
import com.restroute.oilprice.dto.FuelTypeSelection;
import com.restroute.oilprice.dto.NationalOilPriceSummary;
import com.restroute.oilprice.service.NationalOilPriceService;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.RestStopAggregateQueryService;
import com.restroute.reststop.service.dto.RestStopAggregate;
import com.restroute.route.controller.response.FuelPriceTier;
import com.restroute.route.controller.response.RouteRestStopListItemResponse;
import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.route.service.dto.QueriedOilPriceStats;
import com.restroute.route.service.dto.RouteCandidates;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * finder "목적지로 추천받기" 전용 경로 휴게소 조회. 경로 위 후보는 {@link RouteCandidateFinder}가
 * 찾고, 이 서비스는 첫 경로의 후보에 거리·유가·EV 충전 정보를 얹어 평평한 목록으로 만든다.
 *
 * <p>응답 조립을 {@link RouteOptionAssemblyService}에 맡기지 않는 건 지도 화면과 응답 계약을
 * 공유하지 않기 위해서다(도메인 문서 참고).
 */
@Service
@RequiredArgsConstructor
public class RouteRestStopListQueryService {

    private final DestinationResolver destinationResolver;
    private final RouteCandidateFinder routeCandidateFinder;
    private final RestStopAggregateQueryService restStopAggregateQueryService;
    private final EvChargerQueryService evChargerQueryService;
    private final NationalOilPriceService nationalOilPriceService;
    private final QueriedOilPriceStatsCalculator queriedOilPriceStatsCalculator;
    private final RouteRestStopFuelTierCalculator routeRestStopFuelTierCalculator;

    public List<RouteRestStopListItemResponse> findRouteRestStops(
            double originLatitude,
            double originLongitude,
            Double destinationLatitude,
            Double destinationLongitude,
            String destinationName,
            FuelTypeSelection fuelSelection) {
        Destination destination =
                destinationResolver.resolve(destinationLatitude, destinationLongitude, destinationName);
        RouteCandidates found = routeCandidateFinder.find(originLatitude, originLongitude, destination);

        List<RouteRestStopItem> matched = found.first().items();
        if (matched.isEmpty()) {
            return List.of();
        }

        Map<String, RestStopAggregate> aggregatesByServiceAreaCode = aggregatesFor(matched, found.allRestStops());
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

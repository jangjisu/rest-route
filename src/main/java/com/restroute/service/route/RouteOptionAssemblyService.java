package com.restroute.service.route;

import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.controller.response.RouteRestStopResponse.RouteOption;
import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.controller.response.RouteRestStopResponse.RouteSummary;
import com.restroute.domain.RestStopEntity;
import com.restroute.service.RestStopAggregateQueryService;
import com.restroute.service.dto.RestStopAggregate;
import com.restroute.service.route.dto.ResolvedRoute.RouteGeometry;
import com.restroute.service.route.dto.RoutePath;
import com.restroute.service.route.dto.RouteRestStopComparison;
import com.restroute.service.route.dto.RouteRestStopRecommendationStandards;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 대안 경로별로 휴게소를 매칭하고, 이미지/EV충전/테마/이벤트/비교요약/추천태그를 붙여
 * 최종 RouteOption 목록으로 조립한다. 집계 조회는 대안 경로 전체에 대해 한 번만 수행한다.
 */
@Service
@RequiredArgsConstructor
public class RouteOptionAssemblyService {

    private static final String LIST_IMAGE_URL_FORMAT = "/api/rest-stops/%s/images/list";

    private final RouteRestStopMatchingService routeRestStopMatchingService;
    private final RestStopAggregateQueryService restStopAggregateQueryService;
    private final RouteRestStopComparisonSummaryService routeRestStopComparisonSummaryService;
    private final RouteRestStopRecommendationTagService routeRestStopRecommendationTagService;

    public List<RouteOption> assemble(
            List<RouteGeometry> routes,
            List<RestStopEntity> allRestStops,
            int radiusMeters,
            Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        List<RouteCandidate> candidates = IntStream.range(0, routes.size())
                .mapToObj(routeIndex -> buildRouteCandidate(routeIndex, routes.get(routeIndex), allRestStops, radiusMeters))
                .toList();

        Map<String, RestStopAggregate> aggregatesByServiceAreaCode = aggregatesForCandidates(candidates, allRestStops);

        return candidates.stream()
                .map(candidate -> toRouteOption(candidate, aggregatesByServiceAreaCode, nationalOilPriceSummary))
                .toList();
    }

    private RouteCandidate buildRouteCandidate(
            int routeIndex, RouteGeometry geometry, List<RestStopEntity> allRestStops, int radiusMeters) {
        List<RouteRestStopItem> items = routeRestStopMatchingService.match(geometry.path(), radiusMeters, allRestStops);
        return new RouteCandidate(routeIndex, geometry, items);
    }

    /**
     * 대안 경로 전체의 후보 서비스 영역 코드를 합쳐서, 이미지/EV/테마/이벤트/상세/주유/먹거리
     * 집계를 요청당 한 번만 조회한다 — 경로마다 반복하지 않는다. 이미 요청 초기에 조회해둔
     * allRestStops에서 필요한 엔티티만 선별해 넘기므로 RestStopEntity를 다시 조회하지 않는다.
     */
    private Map<String, RestStopAggregate> aggregatesForCandidates(
            List<RouteCandidate> candidates, List<RestStopEntity> allRestStops) {
        Set<String> serviceAreaCodes = candidates.stream()
                .flatMap(candidate -> candidate.items().stream())
                .map(RouteRestStopItem::serviceAreaCode)
                .collect(Collectors.toSet());
        if (serviceAreaCodes.isEmpty()) {
            return Map.of();
        }

        List<RestStopEntity> selected = allRestStops.stream()
                .filter(restStop -> serviceAreaCodes.contains(restStop.getServiceAreaCode()))
                .toList();
        return restStopAggregateQueryService.findByRestStopsAndAdminOverridden(selected, null);
    }

    private RouteOption toRouteOption(
            RouteCandidate candidate,
            Map<String, RestStopAggregate> aggregatesByServiceAreaCode,
            Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        List<RouteRestStopItem> restStops =
                buildResponseItems(candidate.items(), aggregatesByServiceAreaCode, nationalOilPriceSummary);
        return RouteOption.of(
                candidate.routeIndex(),
                routeSummary(candidate.geometry().summary(), candidate.geometry().path()),
                restStops);
    }

    private record RouteCandidate(int routeIndex, RouteGeometry geometry, List<RouteRestStopItem> items) {}

    private List<RouteRestStopItem> buildResponseItems(
            List<RouteRestStopItem> items,
            Map<String, RestStopAggregate> aggregatesByServiceAreaCode,
            Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        List<RouteRestStopComparison> comparisons = items.stream()
                .map(item -> RouteRestStopComparison.of(
                        item,
                        routeRestStopComparisonSummaryService.create(
                                aggregatesByServiceAreaCode
                                        .get(item.serviceAreaCode())
                                        .relatedInfo(),
                                nationalOilPriceSummary)))
                .toList();
        RouteRestStopRecommendationStandards recommendationStandards =
                routeRestStopRecommendationTagService.standards(comparisons);

        return comparisons.stream()
                .map(comparison -> {
                    RestStopAggregate aggregate =
                            aggregatesByServiceAreaCode.get(comparison.item().serviceAreaCode());
                    return comparison
                            .item()
                            .withListImageUrl(listImageUrl(
                                    aggregate.hasListImage(), comparison.item().serviceAreaCode()))
                            .withEvCharger(aggregate.hasEvCharger())
                            .withTheme(aggregate.hasTheme())
                            .withEvent(aggregate.hasEvent())
                            .withComparison(
                                    comparison.summary(),
                                    routeRestStopRecommendationTagService.create(comparison, recommendationStandards));
                })
                .toList();
    }

    private String listImageUrl(boolean hasListImage, String serviceAreaCode) {
        if (!hasListImage) {
            return null;
        }
        return LIST_IMAGE_URL_FORMAT.formatted(serviceAreaCode);
    }

    private RouteSummary routeSummary(KakaoDirectionsResponse.Summary summary, RoutePath path) {
        long distance = summaryValue(summary, true);
        long duration = summaryValue(summary, false);
        return RouteSummary.of(distance, duration, tollFareWon(summary), path.path());
    }

    private long summaryValue(KakaoDirectionsResponse.Summary summary, boolean distance) {
        if (summary == null) {
            return 0L;
        }
        Long value = distance ? summary.distance() : summary.duration();
        return value == null ? 0L : value;
    }

    private long tollFareWon(KakaoDirectionsResponse.Summary summary) {
        if (summary == null || summary.fare() == null || summary.fare().toll() == null) {
            return 0L;
        }
        return summary.fare().toll();
    }
}

package com.restroute.route.service;

import com.restroute.common.client.response.KakaoDirectionsResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.RestStopAggregateQueryService;
import com.restroute.reststop.service.dto.RestStopAggregate;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteOption;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteSummary;
import com.restroute.route.service.dto.RouteCandidate;
import com.restroute.route.service.dto.RoutePath;
import com.restroute.route.service.dto.RouteRestStopComparison;
import com.restroute.route.service.dto.RouteRestStopRecommendationStandards;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 방향 필터링까지 끝난 휴게소 후보(RouteCandidate)에 이미지/EV충전/테마/이벤트/비교요약/추천태그를
 * 붙여 최종 RouteOption 목록으로 조립한다. 집계 조회는 대안 경로 전체에 대해 한 번만 수행한다.
 */
@Service
@RequiredArgsConstructor
public class RouteOptionAssemblyService {

    private static final String LIST_IMAGE_URL_FORMAT = "/api/rest-stops/%s/images/list";

    private final RestStopAggregateQueryService restStopAggregateQueryService;
    private final RouteRestStopComparisonSummaryService routeRestStopComparisonSummaryService;
    private final RouteRestStopRecommendationTagService routeRestStopRecommendationTagService;

    public List<RouteOption> attachDetails(
            List<RouteCandidate> candidates,
            List<RestStopEntity> allRestStops,
            Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        Map<String, RestStopAggregate> aggregatesByServiceAreaCode = aggregatesForCandidates(candidates, allRestStops);

        return candidates.stream()
                .map(candidate -> toRouteOption(candidate, aggregatesByServiceAreaCode, nationalOilPriceSummary))
                .toList();
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
                routeSummary(
                        candidate.geometry().summary(), candidate.geometry().path()),
                restStops);
    }

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

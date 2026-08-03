package com.restroute.service.route;

import com.restroute.controller.response.RouteRestStopResponse;
import com.restroute.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.service.NationalOilPriceService;
import com.restroute.service.RestStopAggregateQueryService;
import com.restroute.service.dto.RestStopAggregate;
import com.restroute.service.route.dto.RouteRestStopCandidate;
import com.restroute.service.route.dto.RouteRestStopComparison;
import com.restroute.service.route.dto.RouteRestStopRecommendationStandards;
import com.restroute.service.route.dto.RouteSearchResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteRestStopService {

    private static final String LIST_IMAGE_URL_FORMAT = "/api/rest-stops/%s/images/list";

    private final RouteRestStopCandidateFinder routeRestStopCandidateFinder;
    private final RouteRestStopComparisonSummaryService routeRestStopComparisonSummaryService;
    private final RouteRestStopRecommendationTagService routeRestStopRecommendationTagService;
    private final NationalOilPriceService nationalOilPriceService;
    private final RestStopAggregateQueryService restStopAggregateQueryService;

    public RouteRestStopResponse findRouteRestStops(
            double originLatitude,
            double originLongitude,
            String destinationQuery,
            Double destinationLatitude,
            Double destinationLongitude,
            String destinationName,
            int radiusMeters) {
        RouteSearchResult searchResult = routeRestStopCandidateFinder.findCandidates(
                originLatitude,
                originLongitude,
                destinationQuery,
                destinationLatitude,
                destinationLongitude,
                destinationName,
                radiusMeters);

        Optional<NationalOilPriceSummary> nationalOilPriceSummary = nationalOilPriceService.getTodaySummary();
        List<RouteRestStopItem> restStops = buildResponseItems(searchResult.candidates(), nationalOilPriceSummary);
        return RouteRestStopResponse.of(searchResult.destination(), searchResult.routeSummary(), restStops);
    }

    private List<RouteRestStopItem> buildResponseItems(
            List<RouteRestStopCandidate> candidates, Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        Map<String, Long> groupCounts = candidates.stream()
                .filter(RouteRestStopCandidate::hasDirectionGroup)
                .map(RouteRestStopCandidate::groupKey)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<String> candidateServiceAreaCodes = candidates.stream()
                .map(candidate -> candidate.restStop().getServiceAreaCode())
                .toList();
        Map<String, RestStopAggregate> aggregatesByServiceAreaCode =
                restStopAggregateQueryService.findByServiceAreaCodesAndAdminOverridden(candidateServiceAreaCodes, null);
        List<RouteRestStopComparison> comparisons = candidates.stream()
                .map(candidate -> RouteRestStopComparison.of(
                        candidate,
                        routeRestStopComparisonSummaryService.create(
                                aggregatesByServiceAreaCode
                                        .get(candidate.restStop().getServiceAreaCode())
                                        .relatedInfo(),
                                nationalOilPriceSummary)))
                .toList();
        RouteRestStopRecommendationStandards recommendationStandards =
                routeRestStopRecommendationTagService.standards(comparisons);
        return comparisons.stream()
                .sorted(Comparator.comparingInt(
                        comparison -> comparison.candidate().routeIndex()))
                .map(comparison -> {
                    RestStopAggregate aggregate = aggregatesByServiceAreaCode.get(
                            comparison.candidate().restStop().getServiceAreaCode());
                    return comparison
                            .candidate()
                            .item()
                            .withListImageUrl(listImageUrl(
                                    aggregate.hasListImage(),
                                    comparison.candidate().restStop().getServiceAreaCode()))
                            .withEvCharger(aggregate.hasEvCharger())
                            .withTheme(aggregate.hasTheme())
                            .withEvent(aggregate.hasEvent())
                            .withDirectionAlternative(groupCounts.getOrDefault(
                                            comparison.candidate().groupKey(), 0L)
                                    > 1)
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
}

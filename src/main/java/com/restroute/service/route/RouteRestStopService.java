package com.restroute.service.route;

import com.restroute.client.KakaoMapClient;
import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.client.response.KakaoLocalSearchResponse;
import com.restroute.controller.response.RouteRestStopResponse;
import com.restroute.controller.response.RouteRestStopResponse.Destination;
import com.restroute.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.controller.response.RouteRestStopResponse.NearbyTraffic;
import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.controller.response.RouteRestStopResponse.RouteSummary;
import com.restroute.domain.RestStopEntity;
import com.restroute.service.NationalOilPriceService;
import com.restroute.service.RestStopAggregateQueryService;
import com.restroute.service.RestStopQueryService;
import com.restroute.service.dto.RestStopAggregate;
import com.restroute.service.route.dto.IndexedMatch;
import com.restroute.service.route.dto.MatchedRestStop;
import com.restroute.service.route.dto.NearbyTrafficStatus;
import com.restroute.service.route.dto.RoutePath;
import com.restroute.service.route.dto.RouteRestStopCandidate;
import com.restroute.service.route.dto.RouteRestStopComparison;
import com.restroute.service.route.dto.RouteRestStopRecommendationStandards;
import com.restroute.service.route.exception.RouteRestStopNotFoundException;
import com.restroute.service.route.util.RouteCoordinateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteRestStopService {

    private static final String LIST_IMAGE_URL_FORMAT = "/api/rest-stops/%s/images/list";

    private final KakaoMapClient kakaoMapClient;
    private final RestStopQueryService restStopQueryService;
    private final RestStopAggregateQueryService restStopAggregateQueryService;
    private final RouteRestStopComparisonSummaryService routeRestStopComparisonSummaryService;
    private final RouteRestStopRecommendationTagService routeRestStopRecommendationTagService;
    private final NationalOilPriceService nationalOilPriceService;

    public RouteRestStopResponse findRouteRestStops(
            double originLatitude,
            double originLongitude,
            String destinationQuery,
            Double destinationLatitude,
            Double destinationLongitude,
            String destinationName,
            int radiusMeters) {
        Destination destination =
                resolveDestination(destinationQuery, destinationLatitude, destinationLongitude, destinationName);

        KakaoDirectionsResponse directions = kakaoMapClient.getDirections(
                RouteCoordinateFormat.toParam(originLongitude, originLatitude),
                RouteCoordinateFormat.toParam(destination.longitude(), destination.latitude()));
        if (directions.failedToRoute()) {
            KakaoDirectionsResponse.Route failedRoute = directions.firstRoute();
            throw new RouteRestStopNotFoundException(
                    routeFailureMessage(failedRoute == null ? null : failedRoute.resultCode()));
        }

        KakaoDirectionsResponse.Route route = directions.firstRoute();
        RoutePath path = RoutePath.from(route);
        if (path.isEmpty()) {
            throw new RouteRestStopNotFoundException("경로 좌표가 없습니다.");
        }

        List<RouteRestStopCandidate> matched = matchRestStopsToPath(path, radiusMeters);
        List<RouteRestStopItem> directionFiltered = removeUnreachableSide(matched, path);

        Optional<NationalOilPriceSummary> nationalOilPriceSummary = nationalOilPriceService.getTodaySummary();
        List<RouteRestStopItem> restStops = buildResponseItems(directionFiltered, nationalOilPriceSummary);

        return RouteRestStopResponse.of(destination, routeSummary(route, path), restStops);
    }

    private String routeFailureMessage(Integer resultCode) {
        int code = resultCode == null ? -1 : resultCode;
        return switch (code) {
            case 101, 105 -> "출발지 주변에서 도로를 찾지 못했어요. 출발지를 도로에 가까운 위치로 바꿔주세요.";
            case 102, 106 -> "도착지 주변에서 도로를 찾지 못했어요. 도착지를 도로에 가까운 위치로 바꿔주세요.";
            case 104 -> "출발지와 도착지가 너무 가까워요. 좀 더 떨어진 위치를 선택해주세요.";
            default -> "경로를 찾지 못했어요. 출발지와 도착지를 다시 확인해주세요.";
        };
    }

    private Destination resolveDestination(
            String destinationQuery, Double destinationLatitude, Double destinationLongitude, String destinationName) {
        if (destinationLatitude == null || destinationLongitude == null) {
            return destinationFromQuery(destinationQuery);
        }
        String name = destinationName == null || destinationName.isBlank() ? "목적지" : destinationName;
        return Destination.of(name, destinationLatitude, destinationLongitude);
    }

    private Destination destinationFromQuery(String destinationQuery) {
        KakaoLocalSearchResponse search = kakaoMapClient.searchKeyword(destinationQuery);
        if (search.isEmpty()) {
            throw new RouteRestStopNotFoundException("목적지 검색 결과가 없습니다: " + destinationQuery);
        }

        KakaoLocalSearchResponse.Document document = search.first();
        Double longitude = RouteCoordinateFormat.parse(document.x());
        Double latitude = RouteCoordinateFormat.parse(document.y());
        if (longitude == null || latitude == null) {
            throw new RouteRestStopNotFoundException("목적지 좌표를 해석하지 못했습니다.");
        }

        return Destination.of(document.label(), latitude, longitude);
    }

    /**
     * 1번: 휴게소 전체를 경로에 매칭한다. 경로상 같은 지점(routeIndex)에 매칭된 휴게소가
     * 여럿이면(드묾) 하나의 RouteRestStopCandidate에 묶인다.
     */
    private List<RouteRestStopCandidate> matchRestStopsToPath(RoutePath path, int radiusMeters) {
        Map<Integer, List<MatchedRestStop>> matchesByRouteIndex = restStopQueryService.findAll().stream()
                .map(restStop -> matchOne(restStop, path, radiusMeters))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        IndexedMatch::routeIndex,
                        Collectors.mapping(IndexedMatch::matchedRestStop, Collectors.toList())));

        return matchesByRouteIndex.entrySet().stream()
                .map(entry -> RouteRestStopCandidate.of(entry.getKey(), entry.getValue()))
                .toList();
    }

    private IndexedMatch matchOne(RestStopEntity restStop, RoutePath path, int radiusMeters) {
        Double latitude = RouteCoordinateFormat.parse(restStop.getYValue());
        Double longitude = RouteCoordinateFormat.parse(restStop.getXValue());
        if (latitude == null || longitude == null) {
            return null;
        }

        RoutePath.Nearest nearest = path.nearestTo(latitude, longitude);
        if (nearest.distanceMeters() > radiusMeters) {
            return null;
        }

        RouteRestStopItem item = RouteRestStopItem.of(
                        restStop.getServiceAreaCode(),
                        restStop.getUnitName(),
                        restStop.getRouteName(),
                        latitude,
                        longitude,
                        Math.round(nearest.distanceMeters()))
                .withNearbyTraffic(nearbyTraffic(path.trafficStateAt(nearest.index())));
        return IndexedMatch.of(nearest.index(), MatchedRestStop.of(restStop, item));
    }

    private NearbyTraffic nearbyTraffic(Integer trafficState) {
        return NearbyTrafficStatus.from(trafficState)
                .map(status -> NearbyTraffic.of(status.key(), status.label()))
                .orElse(null);
    }

    /**
     * 3+4번: 같은 이름의 방향 페어(groupKey)가 2개 이상이면, 진행방향 기준으로 실제로 갈 수
     * 있는 쪽(RIGHT) 하나만 남긴다. 판별이 애매하면(RIGHT가 0개거나 2개 이상) 아무것도
     * 제거하지 않고 hasDirectionAlternative만 켠다. 페어가 아닌 후보는 건드리지 않는다.
     */
    private List<RouteRestStopItem> removeUnreachableSide(List<RouteRestStopCandidate> candidates, RoutePath path) {
        List<IndexedMatch> flattened = candidates.stream()
                .flatMap(candidate -> candidate.restStops().stream()
                        .map(matchedRestStop -> IndexedMatch.of(candidate.routeIndex(), matchedRestStop)))
                .toList();

        Map<String, List<IndexedMatch>> groups = flattened.stream()
                .filter(indexed -> indexed.matchedRestStop().hasDirectionGroup())
                .collect(Collectors.groupingBy(
                        indexed -> indexed.matchedRestStop().groupKey()));

        Map<String, IndexedMatch> winnerByGroupKey = new HashMap<>();
        Set<String> ambiguousGroupKeys = new HashSet<>();
        groups.forEach((groupKey, group) -> {
            if (group.size() < 2) {
                return;
            }
            List<IndexedMatch> reachableSide = group.stream()
                    .filter(indexed -> sideOfTravel(indexed, path) == RoutePath.Side.RIGHT)
                    .toList();
            if (reachableSide.size() != 1) {
                ambiguousGroupKeys.add(groupKey);
                return;
            }
            winnerByGroupKey.put(groupKey, reachableSide.get(0));
        });

        return flattened.stream()
                .filter(indexed -> isKept(indexed, winnerByGroupKey))
                .sorted(Comparator.comparingInt(IndexedMatch::routeIndex))
                .map(indexed ->
                        ambiguousGroupKeys.contains(indexed.matchedRestStop().groupKey())
                                ? indexed.matchedRestStop().item().withDirectionAlternative(true)
                                : indexed.matchedRestStop().item())
                .toList();
    }

    private RoutePath.Side sideOfTravel(IndexedMatch indexed, RoutePath path) {
        RouteRestStopItem item = indexed.matchedRestStop().item();
        return path.sideOfTravel(indexed.routeIndex(), item.latitude(), item.longitude());
    }

    private boolean isKept(IndexedMatch indexed, Map<String, IndexedMatch> winnerByGroupKey) {
        IndexedMatch winner = winnerByGroupKey.get(indexed.matchedRestStop().groupKey());
        return winner == null || winner == indexed;
    }

    /**
     * 5번: 방향 필터링까지 끝난 휴게소들에 이미지/EV충전/테마/이벤트/비교요약/추천태그를 붙여
     * 최종 응답으로 만든다. RouteRestStopCandidate/RestStopEntity는 더 이상 필요 없다 —
     * serviceAreaCode가 이미 item에 있다.
     */
    private List<RouteRestStopItem> buildResponseItems(
            List<RouteRestStopItem> items, Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        List<String> serviceAreaCodes =
                items.stream().map(RouteRestStopItem::serviceAreaCode).toList();
        Map<String, RestStopAggregate> aggregatesByServiceAreaCode =
                restStopAggregateQueryService.findByServiceAreaCodesAndAdminOverridden(serviceAreaCodes, null);

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

    private RouteSummary routeSummary(KakaoDirectionsResponse.Route route, RoutePath path) {
        long distance = summaryValue(route, true);
        long duration = summaryValue(route, false);
        return RouteSummary.of(distance, duration, path.path());
    }

    private long summaryValue(KakaoDirectionsResponse.Route route, boolean distance) {
        KakaoDirectionsResponse.Summary summary = route.summary();
        if (summary == null) {
            return 0L;
        }
        Long value = distance ? summary.distance() : summary.duration();
        return value == null ? 0L : value;
    }
}

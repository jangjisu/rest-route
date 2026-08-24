package com.restroute.route.service;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.route.controller.response.RouteRestStopResponse.NearbyTraffic;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.route.service.dto.IndexedMatch;
import com.restroute.route.service.dto.MatchedRestStop;
import com.restroute.route.service.dto.NearbyTrafficStatus;
import com.restroute.route.service.dto.RoutePath;
import com.restroute.route.service.dto.RouteRestStopCandidate;
import com.restroute.route.service.util.RouteCoordinateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 휴게소 전체를 경로 반경 안에서 매칭하고, 같은 이름의 상·하행 페어가 함께 잡히면 진행방향 기준으로
 * 실제로 갈 수 있는 쪽 하나만 남긴다. 판별이 애매하면 아무것도 제거하지 않고 hasDirectionAlternative만 켠다.
 * 의존성 없는 순수 알고리즘이라 서비스가 아닌 컴포넌트로 분류한다.
 */
@Component
public class RouteRestStopMatcher {

    private static final int AMBIGUITY_CHECK_MIN_GROUP_SIZE = 2;
    private static final int SINGLE_REACHABLE_MATCH_COUNT = 1;

    public List<RouteRestStopItem> match(RoutePath path, int radiusMeters, List<RestStopEntity> allRestStops) {
        List<RouteRestStopCandidate> matched = matchRestStopsToPath(path, radiusMeters, allRestStops);
        return removeUnreachableSide(matched, path);
    }

    private List<RouteRestStopCandidate> matchRestStopsToPath(
            RoutePath path, int radiusMeters, List<RestStopEntity> allRestStops) {
        Map<Integer, List<MatchedRestStop>> matchesByRouteIndex = allRestStops.stream()
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
            if (group.size() < AMBIGUITY_CHECK_MIN_GROUP_SIZE) {
                return;
            }
            List<IndexedMatch> reachableSide = group.stream()
                    .filter(indexed -> sideOfTravel(indexed, path) == RoutePath.Side.RIGHT)
                    .toList();
            if (reachableSide.size() != SINGLE_REACHABLE_MATCH_COUNT) {
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
        return winner == null || winner.equals(indexed);
    }
}

package com.restroute.service.route;

import com.restroute.client.KakaoMapClient;
import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.client.response.KakaoLocalSearchResponse;
import com.restroute.controller.response.RouteRestStopResponse.Destination;
import com.restroute.controller.response.RouteRestStopResponse.NearbyTraffic;
import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.controller.response.RouteRestStopResponse.RouteSummary;
import com.restroute.domain.RestStopEntity;
import com.restroute.service.RestStopQueryService;
import com.restroute.service.route.dto.NearbyTrafficStatus;
import com.restroute.service.route.dto.RoutePolyline;
import com.restroute.service.route.dto.RouteRestStopCandidate;
import com.restroute.service.route.dto.RouteSearchResult;
import com.restroute.service.route.exception.RouteRestStopNotFoundException;
import com.restroute.service.route.util.RouteCoordinateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 카카오 길찾기 API로 경로 좌표를 조회하고, 그 경로 반경 안에 있는 휴게소 후보를 찾는다.
 * CONTEXT.md의 "경로 위 후보" 조회 단계만 담당하며, 후보에 대한 전체 정보 조합/응답 변환은
 * RouteRestStopService가 맡는다.
 */
@Service
@RequiredArgsConstructor
public class RouteRestStopCandidateFinder {

    private static final int MAX_POLYLINE_POINTS = 300;

    private final KakaoMapClient kakaoMapClient;
    private final RestStopQueryService restStopQueryService;

    public RouteSearchResult findCandidates(
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
        RoutePolyline polyline = RoutePolyline.fromRoute(route).downsample(MAX_POLYLINE_POINTS);
        if (polyline.isEmpty()) {
            throw new RouteRestStopNotFoundException("경로 좌표가 없습니다.");
        }

        return RouteSearchResult.of(
                destination, routeSummary(route, polyline), candidatesNearRoute(polyline, radiusMeters));
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

    private List<RouteRestStopCandidate> candidatesNearRoute(RoutePolyline polyline, int radiusMeters) {
        List<RouteRestStopCandidate> candidates = restStopQueryService.findAll().stream()
                .map(restStop -> buildCandidate(restStop, polyline, radiusMeters))
                .filter(Objects::nonNull)
                .toList();
        return filterByDirection(candidates, polyline);
    }

    /**
     * 같은 이름의 상행/하행 페어(groupKey)가 경로 근처에 2개 이상 있을 때만, 진행방향 기준으로
     * 실제로 갈 수 있는 쪽(RIGHT) 하나만 남긴다. 판별이 애매하면(RIGHT가 0개거나 2개 이상)
     * 아무것도 제거하지 않고 hasDirectionAlternative만 켜서 프론트엔드가 안내하게 한다.
     * 페어가 아닌 후보(hasDirectionGroup=false)는 건드리지 않는다.
     */
    private List<RouteRestStopCandidate> filterByDirection(
            List<RouteRestStopCandidate> candidates, RoutePolyline polyline) {
        Map<String, List<RouteRestStopCandidate>> groups = candidates.stream()
                .filter(RouteRestStopCandidate::hasDirectionGroup)
                .collect(Collectors.groupingBy(RouteRestStopCandidate::groupKey));

        Map<String, RouteRestStopCandidate> winnerByGroupKey = new HashMap<>();
        Set<String> ambiguousGroupKeys = new HashSet<>();
        groups.forEach((groupKey, group) -> {
            if (group.size() < 2) {
                return;
            }
            List<RouteRestStopCandidate> reachableSide = group.stream()
                    .filter(candidate -> sideOfTravel(candidate, polyline) == RoutePolyline.Side.RIGHT)
                    .toList();
            if (reachableSide.size() != 1) {
                ambiguousGroupKeys.add(groupKey);
                return;
            }
            winnerByGroupKey.put(groupKey, reachableSide.get(0));
        });

        return candidates.stream()
                .filter(candidate -> isKept(candidate, winnerByGroupKey))
                .map(candidate -> ambiguousGroupKeys.contains(candidate.groupKey())
                        ? withDirectionAlternative(candidate)
                        : candidate)
                .toList();
    }

    private RoutePolyline.Side sideOfTravel(RouteRestStopCandidate candidate, RoutePolyline polyline) {
        return polyline.sideOfTravel(
                candidate.routeIndex(),
                candidate.item().latitude(),
                candidate.item().longitude());
    }

    private boolean isKept(RouteRestStopCandidate candidate, Map<String, RouteRestStopCandidate> winnerByGroupKey) {
        RouteRestStopCandidate winner = winnerByGroupKey.get(candidate.groupKey());
        return winner == null || winner == candidate;
    }

    private RouteRestStopCandidate withDirectionAlternative(RouteRestStopCandidate candidate) {
        return new RouteRestStopCandidate(
                candidate.restStop(),
                candidate.groupKey(),
                candidate.hasDirectionGroup(),
                candidate.routeIndex(),
                candidate.item().withDirectionAlternative(true));
    }

    private RouteRestStopCandidate buildCandidate(RestStopEntity restStop, RoutePolyline polyline, int radiusMeters) {
        Double latitude = RouteCoordinateFormat.parse(restStop.getYValue());
        Double longitude = RouteCoordinateFormat.parse(restStop.getXValue());
        if (latitude == null || longitude == null) {
            return null;
        }

        RoutePolyline.Nearest nearest = polyline.nearest(latitude, longitude);
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
                .withNearbyTraffic(nearbyTraffic(
                        polyline.coordinates().get(nearest.index()).trafficState()));
        return RouteRestStopCandidate.of(restStop, item, nearest.index());
    }

    private NearbyTraffic nearbyTraffic(Integer trafficState) {
        return NearbyTrafficStatus.from(trafficState)
                .map(status -> NearbyTraffic.of(status.key(), status.label()))
                .orElse(null);
    }

    private RouteSummary routeSummary(KakaoDirectionsResponse.Route route, RoutePolyline polyline) {
        long distance = summaryValue(route, true);
        long duration = summaryValue(route, false);
        List<List<Double>> path = polyline.coordinates().stream()
                .map(coordinate -> List.of(coordinate.longitude(), coordinate.latitude()))
                .toList();
        return RouteSummary.of(distance, duration, path);
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

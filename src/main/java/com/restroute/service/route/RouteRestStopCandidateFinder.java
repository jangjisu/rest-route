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
import java.util.ArrayList;
import java.util.List;
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
        List<RouteRestStopCandidate> candidates = new ArrayList<>();
        for (RestStopEntity restStop : restStopQueryService.findAll()) {
            RouteRestStopCandidate candidate = buildCandidate(restStop, polyline, radiusMeters);
            if (candidate == null) {
                continue;
            }
            candidates.add(candidate);
        }
        return candidates;
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

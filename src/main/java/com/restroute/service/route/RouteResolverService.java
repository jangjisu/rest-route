package com.restroute.service.route;

import com.restroute.client.KakaoMapClient;
import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.client.response.KakaoLocalSearchResponse;
import com.restroute.controller.response.RouteRestStopResponse.Destination;
import com.restroute.service.route.dto.ResolvedRoute;
import com.restroute.service.route.dto.ResolvedRoute.RouteGeometry;
import com.restroute.service.route.dto.RoutePath;
import com.restroute.service.route.exception.RouteRestStopNotFoundException;
import com.restroute.service.route.util.RouteCoordinateFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 목적지를 정하고, 카카오 길찾기를 호출해서 대안 경로까지 포함한 경로 좌표열(RoutePath)을 만든다.
 * 길찾기 실패는 여기서 바로 예외로 끝낸다. 개별 경로의 좌표가 비어있으면 그 경로만 제외하고,
 * 전부 비어있으면 예외로 끝낸다.
 */
@Service
@RequiredArgsConstructor
public class RouteResolverService {

    private final KakaoMapClient kakaoMapClient;

    public ResolvedRoute resolve(
            double originLatitude,
            double originLongitude,
            String destinationQuery,
            Double destinationLatitude,
            Double destinationLongitude,
            String destinationName) {
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

        List<RouteGeometry> routes = directions.routes().stream()
                .map(this::toGeometry)
                .filter(geometry -> !geometry.path().isEmpty())
                .toList();
        if (routes.isEmpty()) {
            throw new RouteRestStopNotFoundException("경로 좌표가 없습니다.");
        }

        return ResolvedRoute.of(destination, routes);
    }

    private RouteGeometry toGeometry(KakaoDirectionsResponse.Route route) {
        RoutePath path = RoutePath.from(route.sections(), totalDistanceMeters(route.summary()));
        return RouteGeometry.of(path, route.summary());
    }

    private long totalDistanceMeters(KakaoDirectionsResponse.Summary summary) {
        if (summary == null || summary.distance() == null) {
            return 0L;
        }
        return summary.distance();
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
}

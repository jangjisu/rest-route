package com.restroute.route.service;

import com.restroute.common.client.KakaoMapClient;
import com.restroute.common.client.response.KakaoDirectionsResponse;
import com.restroute.common.client.response.KakaoLocalSearchResponse;
import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import com.restroute.route.service.util.RouteCoordinateFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 목적지를 정하고, 카카오 길찾기를 호출해서 대안 경로까지 포함한 원본 경로 응답을 받아온다.
 * 좌표열 축약(다운샘플링)은 여기서 하지 않는다 — 원본 그대로 RouteRestStopService에 돌려준다.
 * 길찾기 실패는 여기서 바로 예외로 끝낸다.
 */
@Service
@RequiredArgsConstructor
public class RouteResolverService {

    private final KakaoMapClient kakaoMapClient;

    public RawRouteResult resolveDestinationAndRoute(
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
            throw RouteRestStopNotFoundException.routeNotFound(
                    routeFailureMessage(failedRoute == null ? null : failedRoute.resultCode()));
        }

        return new RawRouteResult(destination, directions.routes());
    }

    public record RawRouteResult(Destination destination, List<KakaoDirectionsResponse.Route> routes) {}

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
            throw RouteRestStopNotFoundException.destinationNotFound(destinationQuery);
        }

        KakaoLocalSearchResponse.Document document = search.first();
        Double longitude = RouteCoordinateFormat.parse(document.x());
        Double latitude = RouteCoordinateFormat.parse(document.y());
        if (longitude == null || latitude == null) {
            throw RouteRestStopNotFoundException.destinationCoordinateUnresolved();
        }

        return Destination.of(document.label(), latitude, longitude);
    }
}

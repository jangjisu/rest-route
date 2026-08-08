package com.restroute.service.route.dto;

import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.controller.response.RouteRestStopResponse.Destination;
import java.util.List;

/**
 * 목적지 해석 + 카카오 길찾기 호출 + 경로 좌표열 축약까지 끝난 결과.
 * 대안 경로(alternatives)까지 포함해 1개 이상의 RouteGeometry를 담는다.
 */
public record ResolvedRoute(Destination destination, List<RouteGeometry> routes) {

    public static ResolvedRoute of(Destination destination, List<RouteGeometry> routes) {
        return new ResolvedRoute(destination, routes);
    }

    public record RouteGeometry(RoutePath path, KakaoDirectionsResponse.Summary summary) {

        public static RouteGeometry of(RoutePath path, KakaoDirectionsResponse.Summary summary) {
            return new RouteGeometry(path, summary);
        }
    }
}

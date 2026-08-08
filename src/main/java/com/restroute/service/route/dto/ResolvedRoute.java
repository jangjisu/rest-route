package com.restroute.service.route.dto;

import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.controller.response.RouteRestStopResponse.Destination;

/**
 * 목적지 해석 + 카카오 길찾기 호출 + 경로 좌표열 축약까지 끝난 결과.
 * summary는 최종 응답의 거리/시간(RouteSummary) 조립에만 쓴다.
 */
public record ResolvedRoute(Destination destination, RoutePath path, KakaoDirectionsResponse.Summary summary) {

    public static ResolvedRoute of(Destination destination, RoutePath path, KakaoDirectionsResponse.Summary summary) {
        return new ResolvedRoute(destination, path, summary);
    }
}

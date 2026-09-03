package com.restroute.route.service.exception;

import com.restroute.common.BusinessException;
import com.restroute.common.ResponseCode;

public class RouteRestStopNotFoundException extends BusinessException {

    private RouteRestStopNotFoundException(String message) {
        super(ResponseCode.NOT_FOUND, message);
    }

    /**
     * 카카오 길찾기가 경로 자체를 못 찾았을 때 — reason은 결과 코드별 안내 문구를 호출부가
     * 채워서 넘긴다(출발/도착지 도로 없음, 두 지점이 너무 가까움 등).
     */
    public static RouteRestStopNotFoundException routeNotFound(String reason) {
        return new RouteRestStopNotFoundException(reason);
    }

    public static RouteRestStopNotFoundException destinationNotFound(String destinationQuery) {
        return new RouteRestStopNotFoundException("목적지 검색 결과가 없습니다: " + destinationQuery);
    }

    public static RouteRestStopNotFoundException destinationCoordinateUnresolved() {
        return new RouteRestStopNotFoundException("목적지 좌표를 해석하지 못했습니다.");
    }

    public static RouteRestStopNotFoundException emptyRoutePath() {
        return new RouteRestStopNotFoundException("경로 좌표가 없습니다.");
    }
}

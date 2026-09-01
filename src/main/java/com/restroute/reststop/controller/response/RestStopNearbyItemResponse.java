package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.SizeTier;

/**
 * "이름·거리로 찾기" 목록 하나의 응답. distanceMeters/evChargerCount/fuelBelowAverage는 요청에
 * 대응하는 값(위치/관심 항목)이 없으면 null이다 — 그때 프런트는 해당 값을 그냥 표시하지 않는다.
 * 규모·이용량·볼거리·이벤트 네 태그는 위치·검색어·관심 항목 유무와 무관하게 항상 계산된다.
 */
public record RestStopNearbyItemResponse(
        String unitCode,
        String unitName,
        String routeNo,
        String routeName,
        String xValue,
        String yValue,
        String stdRestCd,
        String serviceAreaCode,
        Double distanceMeters,
        SizeTier sizeTier,
        boolean topTrafficTier,
        boolean hasTheme,
        boolean hasEvent,
        Integer evChargerCount,
        Boolean fuelBelowAverage) {

    public static RestStopNearbyItemResponse of(
            RestStopEntity restStop,
            Double distanceMeters,
            SizeTier sizeTier,
            boolean topTrafficTier,
            boolean hasTheme,
            boolean hasEvent,
            Integer evChargerCount,
            Boolean fuelBelowAverage) {
        return new RestStopNearbyItemResponse(
                restStop.getUnitCode(),
                restStop.getUnitName(),
                restStop.getRouteNo(),
                restStop.getRouteName(),
                restStop.getXValue(),
                restStop.getYValue(),
                restStop.getStdRestCd(),
                restStop.getServiceAreaCode(),
                distanceMeters,
                sizeTier,
                topTrafficTier,
                hasTheme,
                hasEvent,
                evChargerCount,
                fuelBelowAverage);
    }
}

package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.SizeTier;
import com.restroute.reststop.service.dto.RestStopAggregate;

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

    /**
     * EV 충전기 수를 채우는 쪽 — fuelBelowAverage는 항상 null이다. aggregate가 없으면(집계가
     * 아직 안 된 휴게소) 규모·이용량·볼거리·이벤트는 각각 null/false로 남는다.
     */
    public static RestStopNearbyItemResponse ofEvChargerInfo(
            RestStopEntity restStop, Double distanceMeters, RestStopAggregate aggregate, Integer evChargerCount) {
        if (aggregate == null) {
            return of(restStop, distanceMeters, null, false, false, false, evChargerCount, null);
        }
        return of(
                restStop,
                distanceMeters,
                aggregate.sizeTier(),
                aggregate.topTrafficTier(),
                aggregate.hasTheme(),
                aggregate.hasEvent(),
                evChargerCount,
                null);
    }

    /**
     * 유가 정보를 채우는 쪽 — evChargerCount는 항상 null이다. aggregate가 없으면(집계가 아직
     * 안 된 휴게소) 규모·이용량·볼거리·이벤트는 각각 null/false로 남는다.
     */
    public static RestStopNearbyItemResponse ofFuelPriceInfo(
            RestStopEntity restStop, Double distanceMeters, RestStopAggregate aggregate, Boolean fuelBelowAverage) {
        if (aggregate == null) {
            return of(restStop, distanceMeters, null, false, false, false, null, fuelBelowAverage);
        }
        return of(
                restStop,
                distanceMeters,
                aggregate.sizeTier(),
                aggregate.topTrafficTier(),
                aggregate.hasTheme(),
                aggregate.hasEvent(),
                null,
                fuelBelowAverage);
    }

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

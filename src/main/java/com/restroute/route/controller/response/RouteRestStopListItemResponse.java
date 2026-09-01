package com.restroute.route.controller.response;

import com.restroute.reststop.domain.SizeTier;

/**
 * finder "목적지로 추천받기" 전용 경로 휴게소 응답. 기존 {@link RouteRestStopResponse.RouteRestStopItem}과
 * 달리 대안 경로·이미지·먹거리 비교 같은 지도 화면 전용 필드는 없고, 대신 출발지 기준 서버 계산 거리와
 * 요청에 실은 유종 하나로 스코프된 유가 등급을 담는다.
 */
public record RouteRestStopListItemResponse(
        String serviceAreaCode,
        String unitName,
        String routeName,
        double distanceMeters,
        SizeTier sizeTier,
        boolean topTrafficTier,
        Integer evChargerCount,
        FuelPriceTier fuelPriceTier) {

    public static RouteRestStopListItemResponse of(
            String serviceAreaCode,
            String unitName,
            String routeName,
            double distanceMeters,
            SizeTier sizeTier,
            boolean topTrafficTier,
            Integer evChargerCount,
            FuelPriceTier fuelPriceTier) {
        return new RouteRestStopListItemResponse(
                serviceAreaCode,
                unitName,
                routeName,
                distanceMeters,
                sizeTier,
                topTrafficTier,
                evChargerCount,
                fuelPriceTier);
    }
}

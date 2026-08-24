package com.restroute.route.service.dto;

public record RouteRestStopRecommendationStandards(
        Integer lowestGasolinePrice, Integer lowestDieselPrice, Integer lowestLpgPrice, Integer largestParkingCount) {

    public static RouteRestStopRecommendationStandards of(
            Integer lowestGasolinePrice,
            Integer lowestDieselPrice,
            Integer lowestLpgPrice,
            Integer largestParkingCount) {
        return new RouteRestStopRecommendationStandards(
                lowestGasolinePrice, lowestDieselPrice, lowestLpgPrice, largestParkingCount);
    }
}

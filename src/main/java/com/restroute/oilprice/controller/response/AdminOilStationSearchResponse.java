package com.restroute.oilprice.controller.response;

import com.restroute.oilprice.domain.RestOilPriceEntity;

public record AdminOilStationSearchResponse(
        Long id,
        String standardRestName,
        String routeName,
        String serviceAreaAddress,
        String direction,
        String linkedRestStopName) {

    public static AdminOilStationSearchResponse from(RestOilPriceEntity entity, String linkedRestStopName) {
        return new AdminOilStationSearchResponse(
                entity.getId(),
                entity.getServiceAreaName(),
                entity.getRouteName(),
                entity.getServiceAreaAddress(),
                entity.getDirection(),
                linkedRestStopName);
    }
}

package com.restroute.controller.response;

import com.restroute.domain.RestOilPriceEntity;

public record AdminOilStationSearchResponse(
        Long id, String standardRestName, String routeName, String linkedRestStopName) {

    public static AdminOilStationSearchResponse from(RestOilPriceEntity entity, String linkedRestStopName) {
        return new AdminOilStationSearchResponse(
                entity.getId(), entity.getServiceAreaName(), entity.getRouteName(), linkedRestStopName);
    }
}

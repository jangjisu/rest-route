package com.restroute.oilprice.controller.response;

import com.restroute.oilprice.domain.RestOilPriceEntity;

public record AdminOilStationLinkResponse(
        Long id,
        String standardRestName,
        String restStopServiceAreaCode,
        String restStopName,
        boolean adminOverridden) {

    public static AdminOilStationLinkResponse from(
            RestOilPriceEntity entity, String restStopName, boolean adminOverridden) {
        return new AdminOilStationLinkResponse(
                entity.getId(),
                entity.getServiceAreaName(),
                entity.getRestStopServiceAreaCode(),
                restStopName,
                adminOverridden);
    }
}

package com.restroute.controller.response;

import com.restroute.domain.RestOilPriceEntity;

public record AdminOilStationLinkResponse(
        Long id,
        String standardRestName,
        String restStopServiceAreaCode,
        String restStopName,
        boolean adminOverridden) {

    public static AdminOilStationLinkResponse from(RestOilPriceEntity entity, String restStopName) {
        return new AdminOilStationLinkResponse(
                entity.getId(),
                entity.getServiceAreaName(),
                entity.getRestStopServiceAreaCode(),
                restStopName,
                entity.isAdminOverridden());
    }
}

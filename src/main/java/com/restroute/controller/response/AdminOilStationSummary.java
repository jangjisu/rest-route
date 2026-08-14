package com.restroute.controller.response;

import com.restroute.domain.RestOilPriceEntity;

public record AdminOilStationSummary(
        Long id,
        String standardRestName,
        String routeName,
        String serviceAreaAddress,
        String direction,
        boolean adminOverridden) {

    public static AdminOilStationSummary from(RestOilPriceEntity entity, boolean adminOverridden) {
        return new AdminOilStationSummary(
                entity.getId(),
                entity.getServiceAreaName(),
                entity.getRouteName(),
                entity.getServiceAreaAddress(),
                entity.getDirection(),
                adminOverridden);
    }
}

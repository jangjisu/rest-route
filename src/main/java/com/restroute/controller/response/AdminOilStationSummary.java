package com.restroute.controller.response;

import com.restroute.domain.RestOilPriceEntity;

public record AdminOilStationSummary(Long id, String standardRestName, boolean adminOverridden) {

    public static AdminOilStationSummary from(RestOilPriceEntity entity) {
        return new AdminOilStationSummary(entity.getId(), entity.getServiceAreaName(), entity.isAdminOverridden());
    }
}

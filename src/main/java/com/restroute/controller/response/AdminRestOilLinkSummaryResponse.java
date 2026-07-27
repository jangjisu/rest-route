package com.restroute.controller.response;

import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopEntity;

public record AdminRestOilLinkSummaryResponse(
        String serviceAreaCode, String unitName, String routeName, AdminOilStationSummary linkedOilStation) {

    public static AdminRestOilLinkSummaryResponse from(RestStopEntity restStop, RestOilPriceEntity linkedOilStation) {
        return new AdminRestOilLinkSummaryResponse(
                restStop.getServiceAreaCode(),
                restStop.getUnitName(),
                restStop.getRouteName(),
                linkedOilStation == null ? null : AdminOilStationSummary.from(linkedOilStation));
    }
}

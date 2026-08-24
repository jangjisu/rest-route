package com.restroute.oilprice.controller.response;

import com.restroute.domain.RestStopEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;

public record AdminRestOilLinkSummaryResponse(
        String serviceAreaCode, String unitName, String routeName, AdminOilStationSummary linkedOilStation) {

    public static AdminRestOilLinkSummaryResponse from(
            RestStopEntity restStop, RestOilPriceEntity linkedOilStation, boolean adminOverridden) {
        return new AdminRestOilLinkSummaryResponse(
                restStop.getServiceAreaCode(),
                restStop.getUnitName(),
                restStop.getRouteName(),
                linkedOilStation == null ? null : AdminOilStationSummary.from(linkedOilStation, adminOverridden));
    }
}

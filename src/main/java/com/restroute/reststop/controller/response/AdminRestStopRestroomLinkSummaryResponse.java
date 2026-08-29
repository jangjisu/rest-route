package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopRestroomEntity;

public record AdminRestStopRestroomLinkSummaryResponse(
        String serviceAreaCode, String unitName, String routeName, AdminRestroomSummary linkedRestroom) {

    public static AdminRestStopRestroomLinkSummaryResponse from(
            RestStopEntity restStop, RestStopRestroomEntity linkedRestroom) {
        return new AdminRestStopRestroomLinkSummaryResponse(
                restStop.getServiceAreaCode(),
                restStop.getUnitName(),
                restStop.getRouteName(),
                linkedRestroom == null ? null : AdminRestroomSummary.from(linkedRestroom));
    }
}

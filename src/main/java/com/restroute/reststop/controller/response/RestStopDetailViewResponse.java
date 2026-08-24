package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopEntity;

public record RestStopDetailViewResponse(
        String serviceAreaCode,
        String unitCode,
        String unitName,
        String routeNo,
        String routeName,
        String xValue,
        String yValue,
        String stdRestCd) {

    public static RestStopDetailViewResponse from(RestStopEntity restStop) {
        return new RestStopDetailViewResponse(
                restStop.getServiceAreaCode(),
                restStop.getUnitCode(),
                restStop.getUnitName(),
                restStop.getRouteNo(),
                restStop.getRouteName(),
                restStop.getXValue(),
                restStop.getYValue(),
                restStop.getStdRestCd());
    }
}

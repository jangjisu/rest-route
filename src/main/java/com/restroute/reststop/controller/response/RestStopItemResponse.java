package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopEntity;

public record RestStopItemResponse(
        String unitCode,
        String unitName,
        String routeNo,
        String routeName,
        String xValue,
        String yValue,
        String stdRestCd,
        String serviceAreaCode) {

    public static RestStopItemResponse from(RestStopEntity restStop) {
        return new RestStopItemResponse(
                restStop.getUnitCode(),
                restStop.getUnitName(),
                restStop.getRouteNo(),
                restStop.getRouteName(),
                restStop.getXValue(),
                restStop.getYValue(),
                restStop.getStdRestCd(),
                restStop.getServiceAreaCode());
    }
}

package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.domain.RestStopEntity;

public record AdminRestStopEditableResponse(
        String serviceAreaCode,
        String unitCode,
        String unitName,
        String routeNo,
        String routeName,
        String xValue,
        String yValue,
        String telNo,
        String brand,
        String routeCode,
        String svarAddr,
        String convenience,
        String maintenanceYn,
        String truckSaYn,
        boolean adminOverridden) {

    public static AdminRestStopEditableResponse of(RestStopEntity restStop, RestStopDetailEntity detail) {
        boolean hasDetail = detail != null;
        return new AdminRestStopEditableResponse(
                restStop.getServiceAreaCode(),
                restStop.getUnitCode(),
                restStop.getUnitName(),
                restStop.getRouteNo(),
                restStop.getRouteName(),
                restStop.getXValue(),
                restStop.getYValue(),
                hasDetail ? detail.getTelNo() : null,
                hasDetail ? detail.getBrand() : null,
                hasDetail ? detail.getRouteCode() : null,
                hasDetail ? detail.getSvarAddr() : null,
                hasDetail ? detail.getConvenience() : null,
                hasDetail ? detail.getMaintenanceYn() : null,
                hasDetail ? detail.getTruckSaYn() : null,
                restStop.isAdminOverridden() || (hasDetail && detail.isAdminOverridden()));
    }
}

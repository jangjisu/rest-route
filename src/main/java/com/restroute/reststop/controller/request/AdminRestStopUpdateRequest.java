package com.restroute.reststop.controller.request;

public record AdminRestStopUpdateRequest(
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
        String truckSaYn) {}

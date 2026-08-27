package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopRestroomEntity;

public record AdminRestroomSummary(
        Long id, String sourceRestStopName, String routeName, String maleToiletCount, String femaleToiletCount) {

    public static AdminRestroomSummary from(RestStopRestroomEntity entity) {
        return new AdminRestroomSummary(
                entity.getId(),
                entity.getSourceRestStopName(),
                entity.getRouteName(),
                entity.getMaleToiletCount(),
                entity.getFemaleToiletCount());
    }
}

package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopRestroomEntity;

public record AdminRestroomSearchResponse(
        Long id, String sourceRestStopName, String routeName, String linkedRestStopName) {

    public static AdminRestroomSearchResponse from(RestStopRestroomEntity entity, String linkedRestStopName) {
        return new AdminRestroomSearchResponse(
                entity.getId(), entity.getSourceRestStopName(), entity.getRouteName(), linkedRestStopName);
    }
}

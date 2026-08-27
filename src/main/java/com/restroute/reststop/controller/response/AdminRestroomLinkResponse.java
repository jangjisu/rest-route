package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopRestroomEntity;

public record AdminRestroomLinkResponse(
        Long id, String sourceRestStopName, String restStopServiceAreaCode, String restStopName) {

    public static AdminRestroomLinkResponse from(RestStopRestroomEntity entity, String restStopName) {
        return new AdminRestroomLinkResponse(
                entity.getId(), entity.getSourceRestStopName(), entity.getRestStopServiceAreaCode(), restStopName);
    }
}

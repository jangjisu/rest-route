package com.restroute.controller.response;

public record AnalyticsConfigResponse(String measurementId) {

    public static AnalyticsConfigResponse of(String measurementId) {
        return new AnalyticsConfigResponse(measurementId);
    }
}

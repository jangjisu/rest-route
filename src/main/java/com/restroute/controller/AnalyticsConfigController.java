package com.restroute.controller;

import com.restroute.common.ApiResponse;
import com.restroute.controller.response.AnalyticsConfigResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics-config")
public class AnalyticsConfigController {

    private final String measurementId;

    public AnalyticsConfigController(@Value("${google.analytics.measurement-id:}") String measurementId) {
        this.measurementId = measurementId;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AnalyticsConfigResponse>> getAnalyticsConfig() {
        return ResponseEntity.ok(ApiResponse.success(AnalyticsConfigResponse.of(measurementId)));
    }
}

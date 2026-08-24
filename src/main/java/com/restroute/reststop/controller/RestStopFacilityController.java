package com.restroute.reststop.controller;

import com.restroute.common.ApiResponse;
import com.restroute.common.ResponseCode;
import com.restroute.reststop.controller.response.RestStopFacilityResponse;
import com.restroute.reststop.service.RestStopFacilityQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rest-stops")
public class RestStopFacilityController {

    private final RestStopFacilityQueryService restStopFacilityQueryService;

    @GetMapping("/{serviceAreaCode}/facilities")
    public ResponseEntity<ApiResponse<RestStopFacilityResponse>> getRestStopFacilities(
            @PathVariable String serviceAreaCode) {
        return restStopFacilityQueryService
                .findByServiceAreaCode(serviceAreaCode)
                .map(facilities -> ResponseEntity.ok(ApiResponse.success(facilities)))
                .orElseGet(() -> ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                        .body(ApiResponse.error(ResponseCode.NOT_FOUND)));
    }
}

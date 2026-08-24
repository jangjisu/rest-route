package com.restroute.reststop.controller;

import com.restroute.common.ApiResponse;
import com.restroute.common.ResponseCode;
import com.restroute.reststop.controller.response.RestStopBasicInfoResponse;
import com.restroute.reststop.service.RestStopBasicInfoQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rest-stops")
public class RestStopBasicInfoController {

    private final RestStopBasicInfoQueryService restStopBasicInfoQueryService;

    @GetMapping("/{serviceAreaCode}/basic-info")
    public ResponseEntity<ApiResponse<RestStopBasicInfoResponse>> getRestStopBasicInfo(
            @PathVariable String serviceAreaCode) {
        return restStopBasicInfoQueryService
                .findByServiceAreaCode(serviceAreaCode)
                .map(basicInfo -> ResponseEntity.ok(ApiResponse.success(basicInfo)))
                .orElseGet(() -> ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                        .body(ApiResponse.error(ResponseCode.NOT_FOUND)));
    }
}

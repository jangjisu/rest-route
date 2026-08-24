package com.restroute.oilprice.controller;

import com.restroute.common.ApiResponse;
import com.restroute.common.ResponseCode;
import com.restroute.oilprice.controller.response.OilInfoResponse;
import com.restroute.oilprice.service.RestOilPriceRefreshService;
import com.restroute.oilprice.service.RestStopOilInfoQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rest-stops")
public class RestStopOilInfoController {

    private final RestStopOilInfoQueryService restStopOilInfoQueryService;
    private final RestOilPriceRefreshService restOilPriceRefreshService;

    @GetMapping("/{serviceAreaCode}/oil-info")
    public ResponseEntity<ApiResponse<OilInfoResponse>> getRestStopOilInfo(@PathVariable String serviceAreaCode) {
        return restStopOilInfoQueryService
                .findByServiceAreaCode(serviceAreaCode)
                .map(oilInfo -> ResponseEntity.ok(ApiResponse.success(oilInfo)))
                .orElseGet(() -> ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                        .body(ApiResponse.error(ResponseCode.NOT_FOUND)));
    }

    @PostMapping("/{serviceAreaCode}/oil-price/refresh")
    public ResponseEntity<ApiResponse<OilInfoResponse>> refreshRestOilPrice(@PathVariable String serviceAreaCode) {
        return restOilPriceRefreshService
                .refreshByServiceAreaCode(serviceAreaCode)
                .map(oilInfo -> ResponseEntity.ok(ApiResponse.success(oilInfo)))
                .orElseGet(() -> ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                        .body(ApiResponse.error(ResponseCode.NOT_FOUND)));
    }
}

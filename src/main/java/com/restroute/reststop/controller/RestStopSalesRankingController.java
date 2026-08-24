package com.restroute.reststop.controller;

import com.restroute.common.ApiResponse;
import com.restroute.common.ResponseCode;
import com.restroute.reststop.controller.response.RestStopSalesRankingResponse;
import com.restroute.reststop.service.RestStopSalesRankingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rest-stops")
public class RestStopSalesRankingController {

    private final RestStopSalesRankingQueryService salesRankingQueryService;

    @GetMapping("/{serviceAreaCode}/sales-rankings")
    public ResponseEntity<ApiResponse<RestStopSalesRankingResponse>> getSalesRankings(
            @PathVariable String serviceAreaCode) {
        return salesRankingQueryService
                .findByServiceAreaCode(serviceAreaCode)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)))
                .orElseGet(() -> ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                        .body(ApiResponse.error(ResponseCode.NOT_FOUND)));
    }
}

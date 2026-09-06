package com.restroute.oilprice.controller;

import com.restroute.common.ApiResponse;
import com.restroute.common.ResponseCode;
import com.restroute.oilprice.dto.NationalOilPriceSummary;
import com.restroute.oilprice.service.NationalOilPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/national-oil-prices")
public class NationalOilPriceController {

    private final NationalOilPriceService nationalOilPriceService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<NationalOilPriceSummary>> getNationalOilPriceSummary() {
        return nationalOilPriceService
                .getTodaySummary()
                .map(summary -> ResponseEntity.ok(ApiResponse.success(summary)))
                .orElseGet(() -> ResponseEntity.status(ResponseCode.EXTERNAL_API_UNAVAILABLE.getHttpStatus())
                        .body(ApiResponse.error(ResponseCode.EXTERNAL_API_UNAVAILABLE)));
    }
}

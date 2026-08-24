package com.restroute.reststopcontent.controller;

import com.restroute.common.ApiResponse;
import com.restroute.common.ResponseCode;
import com.restroute.reststopcontent.controller.response.FoodMenuResponse;
import com.restroute.reststopcontent.service.RestStopFoodMenuQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rest-stops")
public class RestStopFoodController {

    private final RestStopFoodMenuQueryService restStopFoodMenuQueryService;

    @GetMapping("/{serviceAreaCode}/foods")
    public ResponseEntity<ApiResponse<FoodMenuResponse>> getRestStopFoods(@PathVariable String serviceAreaCode) {
        return restStopFoodMenuQueryService
                .findByServiceAreaCode(serviceAreaCode)
                .map(foodMenu -> ResponseEntity.ok(ApiResponse.success(foodMenu)))
                .orElseGet(() -> ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                        .body(ApiResponse.error(ResponseCode.NOT_FOUND)));
    }
}

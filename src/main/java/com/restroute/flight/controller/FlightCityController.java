package com.restroute.flight.controller;

import com.restroute.common.ApiResponse;
import com.restroute.flight.controller.response.FlightCityResponse;
import com.restroute.flight.service.FlightCityQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flights/cities")
public class FlightCityController {

    private final FlightCityQueryService flightCityQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlightCityResponse>>> search(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(flightCityQueryService.search(keyword, region)));
    }
}

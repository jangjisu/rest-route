package com.restroute.flight.controller;

import com.restroute.common.ApiResponse;
import com.restroute.flight.controller.response.FlightCountryResponse;
import com.restroute.flight.service.FlightCountryQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flights/countries")
public class FlightCountryController {

    private final FlightCountryQueryService flightCountryQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlightCountryResponse>>> search(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(flightCountryQueryService.search(keyword)));
    }
}

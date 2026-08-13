package com.restroute.flight.controller;

import com.restroute.common.ApiResponse;
import com.restroute.flight.controller.response.FlightAirportResponse;
import com.restroute.flight.service.FlightAirportQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flights/airports")
public class FlightAirportController {

    private final FlightAirportQueryService flightAirportQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlightAirportResponse>>> search(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(flightAirportQueryService.search(keyword)));
    }
}

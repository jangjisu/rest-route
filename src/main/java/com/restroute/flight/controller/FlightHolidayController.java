package com.restroute.flight.controller;

import com.restroute.common.ApiResponse;
import com.restroute.flight.controller.response.FlightHolidayResponse;
import com.restroute.flight.service.FlightHolidayQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flights/holidays")
public class FlightHolidayController {

    private final FlightHolidayQueryService flightHolidayQueryService;

    /** months를 여러 개 주면(예: ?months=9&months=10) 그 달들만, 생략하면 전체 공휴일을 반환한다. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FlightHolidayResponse>>> findAll(
            @RequestParam(required = false) List<Integer> months) {
        return ResponseEntity.ok(ApiResponse.success(flightHolidayQueryService.findAll(months)));
    }
}

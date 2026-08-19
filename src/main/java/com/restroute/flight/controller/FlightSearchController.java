package com.restroute.flight.controller;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightApiResponse;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import com.restroute.flight.service.FlightSearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 실 연동 검색 — {@link FlightSearchMockController}와 요청/응답 계약은 같고, 데이터만 Travelpayouts에서 실제로 가져온다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flights")
public class FlightSearchController {

    private final FlightSearchService flightSearchService;

    @GetMapping("/search")
    public ResponseEntity<FlightApiResponse<List<FlightDealResponse>>> search(
            @ModelAttribute FlightSearchRequestDto request) {
        FlightDealSearchResponse result = flightSearchService.search(request);
        return ResponseEntity.ok(FlightApiResponse.success(result.items(), result.meta()));
    }
}

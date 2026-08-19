package com.restroute.flight.controller;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightApiResponse;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import com.restroute.flight.service.FlightSearchMockService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드 개발용 모킹 컨트롤러. 요청/응답 계약은 실제 grouped_prices 연동 기준으로 확정된
 * 것이지만, 응답 데이터 자체는 아직 고정 가짜 데이터(FlightSearchMockFixture)다. 프론트 개발이
 * 운영 환경에서도 이 계약으로 붙어야 해서 프로파일 제한 없이 항상 활성화된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flights")
public class FlightSearchMockController {

    private final FlightSearchMockService flightSearchMockService;

    @GetMapping("/search/mock")
    public ResponseEntity<FlightApiResponse<List<FlightDealResponse>>> searchMock(
            @ModelAttribute FlightSearchRequestDto request) {
        FlightDealSearchResponse result = flightSearchMockService.search(request);
        return ResponseEntity.ok(FlightApiResponse.success(result.items(), result.meta()));
    }
}

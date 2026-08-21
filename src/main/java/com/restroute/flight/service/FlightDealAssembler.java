package com.restroute.flight.service;

import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.service.util.FlightDealResponses;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Travelpayouts 원본을 매핑 → 필터 → 공휴일 채우기 → 전체 최저가 표시 → 정렬까지 거쳐 최종 딜 목록으로 조립한다. */
@Component
@RequiredArgsConstructor
class FlightDealAssembler {

    private final FlightDealResponseMapper responseMapper;
    private final FlightDealPostFilter postFilter;
    private final FlightDealHolidayEnricher holidayEnricher;

    List<FlightDealResponse> assemble(List<TravelpayoutsPriceItem> rawItems, FlightSearchRequestDto request) {
        // Travelpayouts 원본 -> 응답 DTO 변환 (공항/항공사 이름·isLowCost 채우기, id는 아직 미정)
        List<FlightDealResponse> mapped = responseMapper.mapAll(rawItems);
        // 사용자가 원하지 않는 항목 제거 (요청 범위 밖, 주말/공휴일 출발 제외, 경유 제외).
        List<FlightDealResponse> filtered = postFilter.apply(mapped, request);
        // 필터를 통과해 최종적으로 남은 항목에만 holidays를 실제 값으로 채운다.
        List<FlightDealResponse> withHolidays = holidayEnricher.enrich(filtered);
        // 남은 항목 중 전체 최저가 한 건에만 isLowestInRange=true를 표시한다.
        List<FlightDealResponse> marked = FlightDealResponses.markLowestInRange(withHolidays);
        return FlightDealResponses.sorted(marked, request.parsedSort());
    }
}

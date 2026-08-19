package com.restroute.flight.service;

import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Travelpayouts 원본을 매핑 → 필터 → 공휴일 채우기 → 전체 최저가 표시까지 거쳐 최종 딜 목록으로 조립한다. */
@Component
@RequiredArgsConstructor
class FlightDealAssembler {

    private final FlightRangeSearchResponseMapper responseMapper;
    private final FlightDealPostFilter postFilter;
    private final FlightDealHolidayEnricher holidayEnricher;

    List<FlightDealResponse> assemble(
            List<TravelpayoutsPriceItem> rawItems, String token, FlightSearchRequestDto request) {
        List<FlightDealResponse> mapped = responseMapper.mapAll(rawItems, token);
        List<FlightDealResponse> filtered = postFilter.apply(mapped, request);
        List<FlightDealResponse> withHolidays = holidayEnricher.enrich(filtered);
        return FlightDealResponses.markLowestInRange(withHolidays);
    }
}

package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import java.util.List;

/**
 * {@link FlightSearchService}가 실제 딜 목록을 어떻게 구해오는지 위임하는 전략 — 실 연동은
 * {@link FlightRealDealFetcher}, 모킹은 {@link FlightSearchMockService}가 각자 구현체를 준다.
 */
@FunctionalInterface
interface FlightDealFetcher {
    List<FlightDealResponse> fetch(FlightSearchRequestDto request);
}

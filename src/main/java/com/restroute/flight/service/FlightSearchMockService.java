package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import com.restroute.flight.service.util.FlightDealResponses;
import com.restroute.flight.service.util.FlightSearchMockFixture;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 프론트엔드 개발용 모킹 검색. {@link FlightSearchService}에 고정 가짜 데이터를 주는
 * {@link FlightDealFetcher}를 주입해서 그대로 위임한다.
 */
@Service
public class FlightSearchMockService {

    private static final int TOTAL_SIZE = 77;

    private final FlightSearchService delegate;

    @Autowired
    public FlightSearchMockService(FlightDealSessionStore sessionStore) {
        this.delegate = FlightSearchService.create(sessionStore, FlightSearchMockService::fetchMockDeals);
    }

    public FlightSearchMockService() {
        this(FlightDealSessionStore.create());
    }

    public FlightDealSearchResponse search(FlightSearchRequestDto request) {
        return delegate.search(request);
    }

    private static List<FlightDealResponse> fetchMockDeals(FlightSearchRequestDto request) {
        List<FlightDealResponse> items = FlightSearchMockFixture.generateAll(request, TOTAL_SIZE);
        return FlightDealResponses.sorted(items, request.parsedSort());
    }
}

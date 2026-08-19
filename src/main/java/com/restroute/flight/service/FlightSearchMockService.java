package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.service.util.FlightSearchMockFixture;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 프론트엔드 개발용 모킹 검색. {@link FlightSearchService}를 상속해 {@link #fetchDeals}만 오버라이드한다. */
@Service
public class FlightSearchMockService extends FlightSearchService {

    private static final int TOTAL_SIZE = 77;

    @Autowired
    public FlightSearchMockService(FlightDealSessionStore sessionStore) {
        super(sessionStore, null, null, null);
    }

    public FlightSearchMockService() {
        this(FlightDealSessionStore.create());
    }

    @Override
    protected List<FlightDealResponse> fetchDeals(FlightSearchRequestDto request, String token) {
        return FlightSearchMockFixture.generateAll(request, token, TOTAL_SIZE);
    }
}

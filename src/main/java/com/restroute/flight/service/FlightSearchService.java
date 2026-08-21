package com.restroute.flight.service;

import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchMode;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import com.restroute.flight.service.util.FlightParallelPriceCalls;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 항공권 실 연동 검색 진입점. 무한스크롤을 위해 첫 조회 결과를 세션에 cursor로 저장해두고,
 * 이후 요청은 그 세션을 이어서 페이지만 잘라 준다({@link FlightDealSessionStore}).
 */
@Primary
@Service
@RequiredArgsConstructor
public class FlightSearchService {

    private final FlightDealSessionStore sessionStore;
    private final FlightRangeCallPlanner rangeCallPlanner;
    private final FlightFixedCallPlanner fixedCallPlanner;
    private final FlightDealAssembler dealAssembler;

    public FlightDealSearchResponse search(FlightSearchRequestDto request) {
        if (request.isFirstRequest()) {
            return sessionStore.create(request, request.boundedLimit(), () -> fetchDeals(request));
        }
        return sessionStore.find(request, request.cursor(), request.boundedLimit());
    }

    protected List<FlightDealResponse> fetchDeals(FlightSearchRequestDto request) {
        List<Callable<List<TravelpayoutsPriceItem>>> calls = FlightSearchMode.isRange(request.parsedSearchMode())
                ? rangeCallPlanner.plan(request)
                : fixedCallPlanner.plan(request);
        List<TravelpayoutsPriceItem> rawItems = FlightParallelPriceCalls.runAll(calls);
        return dealAssembler.assemble(rawItems, request);
    }
}

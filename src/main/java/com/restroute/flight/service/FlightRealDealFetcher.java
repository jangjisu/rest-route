package com.restroute.flight.service;

import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchMode;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.service.util.FlightParallelPriceCalls;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * RANGE/FIXED 모드에 맞는 플래너로 호출 목록을 만들고, 병렬 실행 후 조립하는 실 연동
 * {@link FlightDealFetcher}.
 */
@Component
@RequiredArgsConstructor
class FlightRealDealFetcher implements FlightDealFetcher {

    private final FlightRangeCallPlanner rangeCallPlanner;
    private final FlightFixedCallPlanner fixedCallPlanner;
    private final FlightDealAssembler dealAssembler;

    @Override
    public List<FlightDealResponse> fetch(FlightSearchRequestDto request) {
        List<Callable<List<TravelpayoutsPriceItem>>> calls = FlightSearchMode.isRange(request.parsedSearchMode())
                ? rangeCallPlanner.plan(request)
                : fixedCallPlanner.plan(request);
        List<TravelpayoutsPriceItem> rawItems = FlightParallelPriceCalls.runAll(calls);
        return dealAssembler.assemble(rawItems, request);
    }
}

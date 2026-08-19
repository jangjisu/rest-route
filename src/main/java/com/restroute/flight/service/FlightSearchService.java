package com.restroute.flight.service;

import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightDealSort;
import com.restroute.flight.controller.dto.FlightSearchMode;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
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
    private final FlightRangeSearchExecutor rangeExecutor;
    private final FlightFixedSearchExecutor fixedExecutor;
    private final FlightDealAssembler dealAssembler;

    public FlightDealSearchResponse search(FlightSearchRequestDto request) {
        if (request.isFirstRequest()) {
            return sessionStore.create(request, request.boundedLimit(), token -> fetch(request, token));
        }
        return sessionStore.find(request, request.cursor(), request.boundedLimit());
    }

    private List<FlightDealResponse> fetch(FlightSearchRequestDto request, String token) {
        return sorted(fetchDeals(request, token), request.parsedSort());
    }

    protected List<FlightDealResponse> fetchDeals(FlightSearchRequestDto request, String token) {
        List<TravelpayoutsPriceItem> rawItems = FlightSearchMode.isRange(request.parsedSearchMode())
                ? rangeExecutor.execute(request)
                : fixedExecutor.execute(request);
        return dealAssembler.assemble(rawItems, token, request);
    }

    static List<FlightDealResponse> sorted(List<FlightDealResponse> items, FlightDealSort sort) {
        return items.stream().sorted(comparatorFor(sort)).toList();
    }

    private static Comparator<FlightDealResponse> comparatorFor(FlightDealSort sort) {
        return switch (sort) {
            case PRICE -> Comparator.comparingInt(deal -> deal.price().amount());
            case DATE ->
                Comparator.comparing(
                        deal -> OffsetDateTime.parse(deal.departure().departAt()));
        };
    }
}

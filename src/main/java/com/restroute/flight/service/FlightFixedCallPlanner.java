package com.restroute.flight.service;

import com.restroute.flight.client.TravelpayoutsClient;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.service.util.FlightSearchDestinations;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * FIXED 검색 — destination 단위(직접 지정/sector 국가들+전체/생략)마다 정확한 출발일·귀국일로
 * 딱 한 번씩만 물어볼 {@link Callable} 목록을 만든다. 실행(병렬 호출·dedup)은 {@link
 * com.restroute.flight.service.util.FlightParallelPriceCalls}가 RANGE/FIXED 공통으로 한다.
 *
 * <p>RANGE와 달리 nights·개월 팬아웃이 없어 destination 단위 수만큼만 호출을 만들고, sector면
 * {@link FlightSearchDestinations#withAggregateIfBudgetAllows}로 "전체" 조회를 예산 안에서만
 * 얹는다(예산 규칙은 {@code docs/domain/flight.md} "정책과 불변 조건" 참고). 단위 하나당 결과는
 * 그 날짜 조합이 인벤토리에 있으면 1건, 없으면 0건이다 — 억지로 범위를 넓히지 않는다.
 */
@Component
@RequiredArgsConstructor
class FlightFixedCallPlanner {

    private final TravelpayoutsClient travelpayoutsClient;

    List<Callable<List<TravelpayoutsPriceItem>>> plan(FlightSearchRequestDto request) {
        List<String> destinations = FlightSearchDestinations.resolve(request);
        List<String> withAggregate = FlightSearchDestinations.isSectorBased(request)
                ? FlightSearchDestinations.withAggregateIfBudgetAllows(destinations, 1)
                : destinations;
        List<String> callDestinations = FlightSearchDestinations.paddedForCalls(withAggregate);

        return callDestinations.stream()
                .<Callable<List<TravelpayoutsPriceItem>>>map(destination -> () -> List.copyOf(travelpayoutsClient
                        .groupedPricesForExactDates(request.origin(), destination, request.dateFrom(), request.dateTo())
                        .dataOrEmpty()
                        .values()))
                .toList();
    }
}

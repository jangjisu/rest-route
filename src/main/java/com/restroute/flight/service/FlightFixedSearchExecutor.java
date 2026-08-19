package com.restroute.flight.service;

import com.restroute.flight.client.TravelpayoutsClient;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * FIXED 검색 — destination 단위(직접 지정/sector 국가들+전체/생략)마다 정확한 출발일·귀국일로
 * 딱 한 번씩만 조회한다. RANGE와 달리 nights·개월 팬아웃이 없어(FIXED는 애초에 nights를 안
 * 받고, 날짜도 하나뿐이라 걸치는 달이 항상 1개다) destination 단위 수만큼만 호출한다 — sector로
 * 국가가 여러 개 잡혀도 최대 10번(9개국+전체) 정도라 RANGE처럼 예산 걱정은 없다.
 *
 * <p>sector로 국가가 잡혔으면 {@link FlightSearchDestinations#withAggregate}로 "전체" 조회를
 * 하나 더 얹는다(RANGE와 동일하게) — direct destination이면 그대로 하나만 조회한다.
 *
 * <p>단위 하나당 결과는 그 정확한 날짜 조합이 실제 인벤토리에 있으면 1건, 없으면 0건이다 —
 * 억지로 범위를 넓혀서 다른 조합을 보여주지 않는다. 국가별 조회와 전체 조회가 같은 딜을 중복
 * 반환할 수 있어, {@link FlightParallelPriceCalls#runAll}이 합치면서 중복을 제거한다.
 */
@Component
@RequiredArgsConstructor
class FlightFixedSearchExecutor {

    private final TravelpayoutsClient travelpayoutsClient;

    List<TravelpayoutsPriceItem> execute(FlightSearchRequestDto request) {
        List<String> destinations = FlightSearchDestinations.resolve(request);
        List<String> withAggregate = FlightSearchDestinations.isSectorBased(request)
                ? FlightSearchDestinations.withAggregate(destinations)
                : destinations;
        List<String> callDestinations = FlightSearchDestinations.paddedForCalls(withAggregate);

        List<Callable<List<TravelpayoutsPriceItem>>> calls = callDestinations.stream()
                .<Callable<List<TravelpayoutsPriceItem>>>map(destination -> () -> List.copyOf(travelpayoutsClient
                        .groupedPricesForExactDates(request.origin(), destination, request.dateFrom(), request.dateTo())
                        .dataOrEmpty()
                        .values()))
                .toList();

        return FlightParallelPriceCalls.runAll(calls);
    }
}

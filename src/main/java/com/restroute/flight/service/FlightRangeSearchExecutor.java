package com.restroute.flight.service;

import com.restroute.flight.client.TravelpayoutsClient;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link FlightRangeSearchPlanner}가 세운 계획대로 Travelpayouts 호출을 전부 병렬 실행하고,
 * 결과를 요청한 날짜 범위 안으로만 걸러서 하나의 목록으로 합친다. 계획 수립까지 여기서 하므로
 * 호출부는 request 하나만 넘기면 된다.
 *
 * <p>plan의 destinations가 비어있으면(destination 생략) 그 축은 반복 없이 1번만 도는데, 이때
 * destination 파라미터 자체를 넘기지 않아야(null) grouped_prices가 알아서 여러 목적지를
 * 섞어준다.
 */
@Component
@RequiredArgsConstructor
class FlightRangeSearchExecutor {

    private final TravelpayoutsClient travelpayoutsClient;

    List<TravelpayoutsPriceItem> execute(FlightSearchRequestDto request) {
        return execute(
                request.origin(),
                FlightRangeSearchPlanner.plan(request),
                request.parsedDateFrom(),
                request.parsedDateTo());
    }

    /** 계획을 이미 세운 상태에서 호출·병합만 한다 — plan을 직접 주고 세밀하게 검증할 때 쓴다(테스트 전용 진입점). */
    List<TravelpayoutsPriceItem> execute(
            String origin, FlightRangeSearchPlan plan, LocalDate dateFrom, LocalDate dateTo) {
        List<TravelpayoutsPriceItem> allItems = FlightParallelPriceCalls.runAll(buildCalls(origin, plan));
        return withinRange(allItems, dateFrom, dateTo);
    }

    private List<Callable<List<TravelpayoutsPriceItem>>> buildCalls(String origin, FlightRangeSearchPlan plan) {
        List<String> destinations = FlightSearchDestinations.paddedForCalls(plan.destinations());

        List<Callable<List<TravelpayoutsPriceItem>>> calls = new ArrayList<>();
        for (String destination : destinations) {
            for (String month : plan.months()) {
                for (FlightRangeSearchPlan.NightsWindow window : plan.nightsWindows()) {
                    calls.add(() -> List.copyOf(travelpayoutsClient
                            .groupedPrices(origin, destination, month, window.min(), window.max())
                            .dataOrEmpty()
                            .values()));
                }
            }
        }
        return calls;
    }

    /** 달 단위로 부른 응답엔 그 달 전체가 오므로, 실제 요청한 dateFrom~dateTo 밖의 항목은 걸러낸다. */
    private static List<TravelpayoutsPriceItem> withinRange(
            List<TravelpayoutsPriceItem> items, LocalDate dateFrom, LocalDate dateTo) {
        return items.stream()
                .filter(item -> {
                    LocalDate departureDate =
                            OffsetDateTime.parse(item.departureAt()).toLocalDate();
                    return !departureDate.isBefore(dateFrom) && !departureDate.isAfter(dateTo);
                })
                .toList();
    }
}

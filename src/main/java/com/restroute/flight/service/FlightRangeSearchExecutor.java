package com.restroute.flight.service;

import com.restroute.flight.client.TravelpayoutsClient;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link FlightRangeSearchPlanner}가 세운 계획대로 Travelpayouts 호출을 전부 가상 스레드로
 * 병렬 실행하고, 결과를 요청한 날짜 범위 안으로만 걸러서 하나의 목록으로 합친다.
 *
 * <p>plan의 destinations가 비어있으면(destination 생략) 그 축은 반복 없이 1번만 도는데, 이때
 * destination 파라미터 자체를 넘기지 않아야(null) grouped_prices가 알아서 여러 목적지를
 * 섞어준다.
 *
 * <p>호출 중 하나라도 실패하면(외부 API 오류 등) 전체 검색을 그대로 실패시킨다 — 일부만
 * 성공한 결과를 조용히 보여주지 않는다.
 */
@Component
@RequiredArgsConstructor
class FlightRangeSearchExecutor {

    private final TravelpayoutsClient travelpayoutsClient;

    List<TravelpayoutsPriceItem> execute(
            String origin, FlightRangeSearchPlan plan, LocalDate dateFrom, LocalDate dateTo) {
        List<TravelpayoutsPriceItem> allItems = runAll(buildCalls(origin, plan));
        return withinRange(allItems, dateFrom, dateTo);
    }

    private List<Callable<List<TravelpayoutsPriceItem>>> buildCalls(String origin, FlightRangeSearchPlan plan) {
        List<String> destinations =
                plan.destinations().isEmpty() ? Collections.singletonList(null) : plan.destinations();

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

    private static List<TravelpayoutsPriceItem> runAll(List<Callable<List<TravelpayoutsPriceItem>>> calls) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<TravelpayoutsPriceItem>>> futures =
                    calls.stream().map(executor::submit).toList();
            List<TravelpayoutsPriceItem> results = new ArrayList<>();
            for (Future<List<TravelpayoutsPriceItem>> future : futures) {
                results.addAll(resultOf(future));
            }
            return results;
        }
    }

    /**
     * ExecutionException을 풀어서 원인을 그대로 다시 던진다 — 이 원인 자체가 이미 그 스레드
     * 안에서 발생한 시점의 스택트레이스를 갖고 있으므로, 그대로 던지는 게 스택트레이스를
     * 보존하는 올바른 방법이다(PMD가 이 관용구를 못 알아봐서 억제한다).
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    private static List<TravelpayoutsPriceItem> resultOf(Future<List<TravelpayoutsPriceItem>> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
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

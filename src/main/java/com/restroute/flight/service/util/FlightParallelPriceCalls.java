package com.restroute.flight.service.util;

import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Travelpayouts 호출 여러 개를 가상 스레드로 병렬 실행하고 결과를 하나로 합친다. RANGE·FIXED
 * 실행기 둘 다 "호출 목록을 만드는 방식"만 다르고 "그걸 병렬로 부르고 합치는" 방식은 같아서
 * 여기 하나로 공유한다.
 *
 * <p>호출 중 하나라도 실패하면(외부 API 오류 등) 전체를 그대로 실패시킨다 — 일부만 성공한
 * 결과를 조용히 보여주지 않는다.
 *
 * <p>국가별 조회와 "전체"(destination 생략) 조회를 함께 하는 경우, 같은 항공권이 양쪽에 모두
 * 잡혀서 중복으로 올 수 있다 — destinationAirport·departureAt·returnAt·flightNumber가 모두
 * 같으면 같은 항공권으로 보고 더 싼 쪽만 남긴다.
 */
public final class FlightParallelPriceCalls {

    private FlightParallelPriceCalls() {}

    public static List<TravelpayoutsPriceItem> runAll(List<Callable<List<TravelpayoutsPriceItem>>> calls) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<TravelpayoutsPriceItem>>> futures =
                    calls.stream().map(executor::submit).toList();
            List<TravelpayoutsPriceItem> results = new ArrayList<>();
            for (Future<List<TravelpayoutsPriceItem>> future : futures) {
                results.addAll(resultOf(future));
            }
            return deduped(results);
        }
    }

    private static List<TravelpayoutsPriceItem> deduped(List<TravelpayoutsPriceItem> items) {
        Map<String, TravelpayoutsPriceItem> cheapestByKey = new LinkedHashMap<>();
        for (TravelpayoutsPriceItem item : items) {
            String key = dedupKeyOf(item);
            TravelpayoutsPriceItem existing = cheapestByKey.get(key);
            if (existing == null || item.price() < existing.price()) {
                cheapestByKey.put(key, item);
            }
        }
        return List.copyOf(cheapestByKey.values());
    }

    private static String dedupKeyOf(TravelpayoutsPriceItem item) {
        return String.join(
                "|",
                String.valueOf(item.destinationAirport()),
                String.valueOf(item.departureAt()),
                String.valueOf(item.returnAt()),
                String.valueOf(item.flightNumber()));
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
}

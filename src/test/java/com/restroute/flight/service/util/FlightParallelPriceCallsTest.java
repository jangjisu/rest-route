package com.restroute.flight.service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.flight.client.exception.TravelpayoutsApiException;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightParallelPriceCallsTest {

    private static TravelpayoutsPriceItem itemAt(String destinationAirport, String flightNumber, int price) {
        return new TravelpayoutsPriceItem(
                "SEL",
                "OSA",
                "ICN",
                destinationAirport,
                price,
                "LJ",
                flightNumber,
                "2026-09-15T09:00:00+09:00",
                "2026-09-18T09:00:00+09:00",
                0,
                0,
                90,
                90,
                90,
                "gate",
                "link");
    }

    private static Callable<List<TravelpayoutsPriceItem>> callReturning(TravelpayoutsPriceItem... items) {
        return () -> List.of(items);
    }

    @Test
    @DisplayName("여러 호출 결과를 하나로 합친다")
    void runAll_mergesResultsFromAllCalls() {
        List<TravelpayoutsPriceItem> result = FlightParallelPriceCalls.runAll(
                List.of(callReturning(itemAt("KIX", "1", 89000)), callReturning(itemAt("FUK", "2", 95000))));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("목적지·출발일·귀국일·편명이 모두 같으면 같은 항공권으로 보고 하나만 남긴다")
    void runAll_dedupesSameFlightAcrossCalls() {
        TravelpayoutsPriceItem fromSectorCall = itemAt("KIX", "777", 89000);
        TravelpayoutsPriceItem fromAggregateCall = itemAt("KIX", "777", 89000);

        List<TravelpayoutsPriceItem> result = FlightParallelPriceCalls.runAll(
                List.of(callReturning(fromSectorCall), callReturning(fromAggregateCall)));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("중복인데 가격이 다르면 더 싼 쪽을 남긴다")
    void runAll_keepsCheaperWhenDuplicatePricesDiffer() {
        TravelpayoutsPriceItem expensive = itemAt("KIX", "777", 95000);
        TravelpayoutsPriceItem cheaper = itemAt("KIX", "777", 89000);

        List<TravelpayoutsPriceItem> result =
                FlightParallelPriceCalls.runAll(List.of(callReturning(expensive), callReturning(cheaper)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).price()).isEqualTo(89000);
    }

    @Test
    @DisplayName("편명이 다르면 목적지·날짜가 같아도 다른 항공권으로 본다")
    void runAll_keepsBothWhenFlightNumberDiffers() {
        TravelpayoutsPriceItem first = itemAt("KIX", "1", 89000);
        TravelpayoutsPriceItem second = itemAt("KIX", "2", 89000);

        List<TravelpayoutsPriceItem> result =
                FlightParallelPriceCalls.runAll(List.of(callReturning(first), callReturning(second)));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("호출 하나라도 실패하면 전체가 그대로 실패한다")
    void runAll_propagatesFailure() {
        Callable<List<TravelpayoutsPriceItem>> failing = () -> {
            throw new TravelpayoutsApiException("grouped prices", "boom");
        };

        assertThatThrownBy(() -> FlightParallelPriceCalls.runAll(List.of(failing)))
                .isInstanceOf(TravelpayoutsApiException.class);
    }
}

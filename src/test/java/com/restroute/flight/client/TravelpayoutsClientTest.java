package com.restroute.flight.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.restroute.flight.client.exception.TravelpayoutsApiException;
import com.restroute.flight.client.response.TravelpayoutsGroupedPricesResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TravelpayoutsClientTest {

    @Mock
    private TravelpayoutsFeignClient travelpayoutsFeignClient;

    private TravelpayoutsClient travelpayoutsClient;

    @BeforeEach
    void setUp() {
        travelpayoutsClient = new TravelpayoutsClient(travelpayoutsFeignClient, "test-token");
    }

    @Test
    @DisplayName("groupedPrices는 krw 통화와 토큰으로, return_at 없이 grouped_prices를 호출한다")
    void groupedPrices_passesCurrencyAndToken() {
        TravelpayoutsGroupedPricesResponse response = new TravelpayoutsGroupedPricesResponse(true, "krw", Map.of());
        when(travelpayoutsFeignClient.groupedPrices("ICN", "OSA", "2026-08", null, 1, 3, "krw", "test-token"))
                .thenReturn(response);

        TravelpayoutsGroupedPricesResponse result = travelpayoutsClient.groupedPrices("ICN", "OSA", "2026-08", 1, 3);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("응답이 null이면 TravelpayoutsApiException을 던진다")
    void groupedPrices_throwsOnNullResponse() {
        when(travelpayoutsFeignClient.groupedPrices("ICN", "OSA", "2026-08", null, null, null, "krw", "test-token"))
                .thenReturn(null);

        assertThatThrownBy(() -> travelpayoutsClient.groupedPrices("ICN", "OSA", "2026-08", null, null))
                .isInstanceOf(TravelpayoutsApiException.class)
                .hasMessageContaining("빈 응답");
    }

    @Test
    @DisplayName("응답의 success가 false면 TravelpayoutsApiException을 던진다")
    void groupedPrices_throwsWhenSuccessFalse() {
        TravelpayoutsGroupedPricesResponse response = new TravelpayoutsGroupedPricesResponse(false, "krw", Map.of());
        when(travelpayoutsFeignClient.groupedPrices("ICN", "OSA", "2026-08", null, null, null, "krw", "test-token"))
                .thenReturn(response);

        assertThatThrownBy(() -> travelpayoutsClient.groupedPrices("ICN", "OSA", "2026-08", null, null))
                .isInstanceOf(TravelpayoutsApiException.class)
                .hasMessageContaining("success=false");
    }

    @Test
    @DisplayName("호출이 런타임 예외를 던지면 TravelpayoutsApiException으로 감싼다")
    void groupedPrices_wrapsRuntimeException() {
        when(travelpayoutsFeignClient.groupedPrices("ICN", "OSA", "2026-08", null, null, null, "krw", "test-token"))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> travelpayoutsClient.groupedPrices("ICN", "OSA", "2026-08", null, null))
                .isInstanceOf(TravelpayoutsApiException.class)
                .hasMessageContaining("boom");
    }

    @Test
    @DisplayName("groupedPricesForExactDates는 departure_at/return_at을 정확한 날짜로, min/max_trip_duration 없이 호출한다")
    void groupedPricesForExactDates_passesExactDatesWithoutDurationRange() {
        TravelpayoutsGroupedPricesResponse response = new TravelpayoutsGroupedPricesResponse(true, "krw", Map.of());
        when(travelpayoutsFeignClient.groupedPrices(
                        "ICN", "OSA", "2026-09-15", "2026-09-22", null, null, "krw", "test-token"))
                .thenReturn(response);

        TravelpayoutsGroupedPricesResponse result =
                travelpayoutsClient.groupedPricesForExactDates("ICN", "OSA", "2026-09-15", "2026-09-22");

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("groupedPricesForExactDates도 응답이 null이면 TravelpayoutsApiException을 던진다")
    void groupedPricesForExactDates_throwsOnNullResponse() {
        when(travelpayoutsFeignClient.groupedPrices(
                        "ICN", "OSA", "2026-09-15", "2026-09-22", null, null, "krw", "test-token"))
                .thenReturn(null);

        assertThatThrownBy(
                        () -> travelpayoutsClient.groupedPricesForExactDates("ICN", "OSA", "2026-09-15", "2026-09-22"))
                .isInstanceOf(TravelpayoutsApiException.class)
                .hasMessageContaining("빈 응답");
    }

    @Test
    @DisplayName("groupedPricesForExactDates도 success가 false면 TravelpayoutsApiException을 던진다")
    void groupedPricesForExactDates_throwsWhenSuccessFalse() {
        TravelpayoutsGroupedPricesResponse response = new TravelpayoutsGroupedPricesResponse(false, "krw", Map.of());
        when(travelpayoutsFeignClient.groupedPrices(
                        "ICN", "OSA", "2026-09-15", "2026-09-22", null, null, "krw", "test-token"))
                .thenReturn(response);

        assertThatThrownBy(
                        () -> travelpayoutsClient.groupedPricesForExactDates("ICN", "OSA", "2026-09-15", "2026-09-22"))
                .isInstanceOf(TravelpayoutsApiException.class)
                .hasMessageContaining("success=false");
    }
}

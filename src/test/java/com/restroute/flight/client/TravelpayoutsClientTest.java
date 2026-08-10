package com.restroute.flight.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.restroute.flight.client.exception.TravelpayoutsApiException;
import com.restroute.flight.client.response.TravelpayoutsCityItem;
import com.restroute.flight.client.response.TravelpayoutsGroupedPricesResponse;
import java.util.List;
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
    @DisplayName("groupedPrices는 krw 통화와 토큰으로 grouped_prices를 호출한다")
    void groupedPrices_passesCurrencyAndToken() {
        TravelpayoutsGroupedPricesResponse response = new TravelpayoutsGroupedPricesResponse(true, "krw", Map.of());
        when(travelpayoutsFeignClient.groupedPrices("ICN", "OSA", "2026-08", 1, 3, "krw", "test-token"))
                .thenReturn(response);

        TravelpayoutsGroupedPricesResponse result = travelpayoutsClient.groupedPrices("ICN", "OSA", "2026-08", 1, 3);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("citiesData는 전체 도시 목록을 그대로 반환한다")
    void citiesData_returnsFullCityList() {
        List<TravelpayoutsCityItem> items = List.of(new TravelpayoutsCityItem("Osaka", "OSA", "JP", true));
        when(travelpayoutsFeignClient.citiesData()).thenReturn(items);

        List<TravelpayoutsCityItem> result = travelpayoutsClient.citiesData();

        assertThat(result).isSameAs(items);
    }

    @Test
    @DisplayName("응답이 null이면 TravelpayoutsApiException을 던진다")
    void groupedPrices_throwsOnNullResponse() {
        when(travelpayoutsFeignClient.groupedPrices("ICN", "OSA", "2026-08", null, null, "krw", "test-token"))
                .thenReturn(null);

        assertThatThrownBy(() -> travelpayoutsClient.groupedPrices("ICN", "OSA", "2026-08", null, null))
                .isInstanceOf(TravelpayoutsApiException.class)
                .hasMessageContaining("빈 응답");
    }

    @Test
    @DisplayName("호출이 런타임 예외를 던지면 TravelpayoutsApiException으로 감싼다")
    void groupedPrices_wrapsRuntimeException() {
        when(travelpayoutsFeignClient.groupedPrices("ICN", "OSA", "2026-08", null, null, "krw", "test-token"))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> travelpayoutsClient.groupedPrices("ICN", "OSA", "2026-08", null, null))
                .isInstanceOf(TravelpayoutsApiException.class)
                .hasMessageContaining("boom");
    }
}

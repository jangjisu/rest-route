package com.restroute.flight.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TravelpayoutsGroupedPricesResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("grouped_prices 응답 JSON을 필드에 매핑한다")
    void readValue_mapsGroupedPricesFields() throws Exception {
        String json = """
                {
                  "success": true,
                  "currency": "krw",
                  "data": {
                    "2026-08-26": {
                      "origin": "SEL",
                      "destination": "OSA",
                      "origin_airport": "ICN",
                      "destination_airport": "KIX",
                      "price": 171845,
                      "airline": "WE",
                      "flight_number": "513",
                      "departure_at": "2026-08-26T17:45:00+09:00",
                      "return_at": "2026-08-29T14:35:00+09:00",
                      "transfers": 0,
                      "return_transfers": 0,
                      "duration": 230,
                      "duration_to": 110,
                      "duration_back": 120,
                      "gate": "Trip.com",
                      "link": "/search/ICN2608OSA29081?t=abc"
                    }
                  }
                }
                """;

        TravelpayoutsGroupedPricesResponse response =
                objectMapper.readValue(json, TravelpayoutsGroupedPricesResponse.class);

        assertThat(response.success()).isTrue();
        assertThat(response.currency()).isEqualTo("krw");
        TravelpayoutsPriceItem item = response.data().get("2026-08-26");
        assertThat(item.originAirport()).isEqualTo("ICN");
        assertThat(item.destinationAirport()).isEqualTo("KIX");
        assertThat(item.price()).isEqualTo(171845);
        assertThat(item.flightNumber()).isEqualTo("513");
        assertThat(item.gate()).isEqualTo("Trip.com");
        assertThat(item.link()).isEqualTo("/search/ICN2608OSA29081?t=abc");
    }

    @Test
    @DisplayName("dataOrEmpty는 data가 null이면 빈 맵을 반환한다")
    void dataOrEmpty_returnsEmptyMap_whenDataIsNull() {
        TravelpayoutsGroupedPricesResponse response = new TravelpayoutsGroupedPricesResponse(true, "krw", null);

        assertThat(response.dataOrEmpty()).isEmpty();
    }

    @Test
    @DisplayName("dataOrEmpty는 data가 있으면 그대로 반환한다")
    void dataOrEmpty_returnsDataAsIs_whenPresent() {
        Map<String, TravelpayoutsPriceItem> data = Map.of(
                "2026-08-26",
                new TravelpayoutsPriceItem(
                        "SEL",
                        "OSA",
                        "ICN",
                        "KIX",
                        171845,
                        "WE",
                        "513",
                        "2026-08-26",
                        "2026-08-29",
                        0,
                        0,
                        230,
                        110,
                        120,
                        "Trip.com",
                        "/search/x"));
        TravelpayoutsGroupedPricesResponse response = new TravelpayoutsGroupedPricesResponse(true, "krw", data);

        assertThat(response.dataOrEmpty()).isSameAs(data);
    }
}

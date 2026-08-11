package com.restroute.flight.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TravelpayoutsCityItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("cities.json 응답 필드를 매핑하고 나머지 필드는 무시한다")
    void readValue_mapsCityFields() throws Exception {
        String json = """
                {
                  "name": "Osaka",
                  "code": "OSA",
                  "country_code": "JP",
                  "has_flightable_airport": true,
                  "time_zone": "Asia/Tokyo",
                  "coordinates": { "lat": 34.6, "lon": 135.5 }
                }
                """;

        TravelpayoutsCityItem item = objectMapper.readValue(json, TravelpayoutsCityItem.class);

        assertThat(item.name()).isEqualTo("Osaka");
        assertThat(item.code()).isEqualTo("OSA");
        assertThat(item.countryCode()).isEqualTo("JP");
        assertThat(item.hasFlightableAirport()).isTrue();
    }
}

package com.restroute.flight.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TravelpayoutsAirlineItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("airlines.json 응답 필드를 매핑하고 나머지 필드는 무시한다")
    void readValue_mapsAirlineFields() throws Exception {
        String json = """
                {
                  "code": "7C",
                  "name": "Jeju Air",
                  "is_lowcost": true,
                  "name_translations": { "en": "Jeju Air" }
                }
                """;

        TravelpayoutsAirlineItem item = objectMapper.readValue(json, TravelpayoutsAirlineItem.class);

        assertThat(item.code()).isEqualTo("7C");
        assertThat(item.name()).isEqualTo("Jeju Air");
    }
}

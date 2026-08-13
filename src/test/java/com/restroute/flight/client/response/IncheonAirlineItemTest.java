package com.restroute.flight.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IncheonAirlineItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("getServiceAirlineInfo 응답 필드를 매핑하고 나머지 필드는 무시한다")
    void readValue_mapsAirlineFields() throws Exception {
        String json = """
                {
                  "airlineImage": "https://odp.airport.kr/apiPortal/airlineIconDown?IATA_CODE=KE",
                  "airlineName": "대한항공",
                  "airlineTel": "1588-2001",
                  "airlineIcTel": "1588-2001",
                  "airlineIata": "KE",
                  "airlineIcao": "KAL"
                }
                """;

        IncheonAirlineItem item = objectMapper.readValue(json, IncheonAirlineItem.class);

        assertThat(item.iataCode()).isEqualTo("KE");
        assertThat(item.name()).isEqualTo("대한항공");
    }
}

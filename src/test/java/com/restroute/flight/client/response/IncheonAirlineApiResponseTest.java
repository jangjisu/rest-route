package com.restroute.flight.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IncheonAirlineApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("response.body.items 중첩 구조를 매핑한다")
    void readValue_mapsNestedItems() throws Exception {
        String json = """
                {
                  "response": {
                    "header": { "resultCode": "00", "resultMsg": "NORMAL SERVICE." },
                    "body": {
                      "items": [
                        { "airlineIata": "KE", "airlineName": "대한항공" },
                        { "airlineIata": "7C", "airlineName": "제주항공" }
                      ]
                    }
                  }
                }
                """;

        IncheonAirlineApiResponse response = objectMapper.readValue(json, IncheonAirlineApiResponse.class);

        assertThat(response.itemsOrEmpty())
                .extracting(IncheonAirlineItem::iataCode)
                .containsExactly("KE", "7C");
    }

    @Test
    @DisplayName("body나 items가 없으면 itemsOrEmpty는 빈 목록을 반환한다")
    void itemsOrEmpty_returnsEmptyList_whenBodyMissing() throws Exception {
        IncheonAirlineApiResponse response = objectMapper.readValue("{}", IncheonAirlineApiResponse.class);

        assertThat(response.itemsOrEmpty()).isEmpty();
    }
}

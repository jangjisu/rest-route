package com.restroute.flight.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TravelpayoutsCountryItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("countries.json 응답 필드를 매핑하고 나머지 필드는 무시한다")
    void readValue_mapsCountryFields() throws Exception {
        String json = """
                {
                  "code": "JP",
                  "name": "일본",
                  "currency": "JPY",
                  "name_translations": { "en": "Japan" }
                }
                """;

        TravelpayoutsCountryItem item = objectMapper.readValue(json, TravelpayoutsCountryItem.class);

        assertThat(item.code()).isEqualTo("JP");
        assertThat(item.name()).isEqualTo("일본");
        assertThat(item.engName()).isEqualTo("Japan");
    }

    @Test
    @DisplayName("번역이 없으면 engName은 name으로 대체된다")
    void engName_fallsBackToName_whenTranslationMissing() {
        TravelpayoutsCountryItem item = new TravelpayoutsCountryItem("JP", "일본", null);

        assertThat(item.engName()).isEqualTo("일본");
    }
}

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
                  "name": "오사카",
                  "code": "OSA",
                  "country_code": "JP",
                  "has_flightable_airport": true,
                  "time_zone": "Asia/Tokyo",
                  "coordinates": { "lat": 34.6, "lon": 135.5 },
                  "name_translations": { "en": "Osaka" }
                }
                """;

        TravelpayoutsCityItem item = objectMapper.readValue(json, TravelpayoutsCityItem.class);

        assertThat(item.name()).isEqualTo("오사카");
        assertThat(item.code()).isEqualTo("OSA");
        assertThat(item.countryCode()).isEqualTo("JP");
        assertThat(item.hasFlightableAirport()).isTrue();
    }

    @Test
    @DisplayName("한글 name이 있으면 korName은 name을, engName은 번역을 반환한다")
    void korNameAndEngName_bothAvailable() {
        TravelpayoutsCityItem item = new TravelpayoutsCityItem(
                "오사카", "OSA", "JP", true, new TravelpayoutsCityItem.NameTranslations("Osaka"));

        assertThat(item.korName()).isEqualTo("오사카");
        assertThat(item.engName()).isEqualTo("Osaka");
    }

    @Test
    @DisplayName("name이 한글이 아니면 korName은 null이고, engName은 번역을 반환한다")
    void korName_isNull_whenNameIsNotKorean() {
        TravelpayoutsCityItem item = new TravelpayoutsCityItem(
                "São Jorge", "SJZ", "PT", true, new TravelpayoutsCityItem.NameTranslations("Sao Jorge"));

        assertThat(item.korName()).isNull();
        assertThat(item.engName()).isEqualTo("Sao Jorge");
    }

    @Test
    @DisplayName("name이 null이고 번역도 없으면 engName은 null이다")
    void engName_isNull_whenNothingAvailable() {
        TravelpayoutsCityItem item = new TravelpayoutsCityItem(null, "XXX", "ZZ", true, null);

        assertThat(item.korName()).isNull();
        assertThat(item.engName()).isNull();
    }

    @Test
    @DisplayName("name만 있고 번역이 없으면 engName은 name으로 대체된다")
    void engName_fallsBackToName_whenTranslationMissing() {
        TravelpayoutsCityItem item = new TravelpayoutsCityItem("Iturup Island", "ITU", "RU", true, null);

        assertThat(item.engName()).isEqualTo("Iturup Island");
    }
}

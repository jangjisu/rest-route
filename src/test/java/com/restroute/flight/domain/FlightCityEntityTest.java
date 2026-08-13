package com.restroute.flight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.client.response.TravelpayoutsCityItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightCityEntityTest {

    @Test
    @DisplayName("생성자로 전달한 필드를 그대로 노출한다")
    void constructor_exposesGivenFields() {
        FlightCityEntity entity = new FlightCityEntity("OSA", "오사카", "Osaka", "JP");

        assertThat(entity.getCode()).isEqualTo("OSA");
        assertThat(entity.getKorName()).isEqualTo("오사카");
        assertThat(entity.getEngName()).isEqualTo("Osaka");
        assertThat(entity.getCountryCode()).isEqualTo("JP");
    }

    @Test
    @DisplayName("from은 item의 korName/engName을 그대로 매핑한다")
    void from_mapsKorNameAndEngName() {
        TravelpayoutsCityItem item = new TravelpayoutsCityItem(
                "오사카", "OSA", "JP", true, new TravelpayoutsCityItem.NameTranslations("Osaka"));

        FlightCityEntity entity = FlightCityEntity.from(item);

        assertThat(entity.getCode()).isEqualTo("OSA");
        assertThat(entity.getKorName()).isEqualTo("오사카");
        assertThat(entity.getEngName()).isEqualTo("Osaka");
        assertThat(entity.getCountryCode()).isEqualTo("JP");
    }

    @Test
    @DisplayName("한글 name이 없으면 from은 korName을 null로 매핑한다")
    void from_setsKorNameNull_whenNameIsNotKorean() {
        TravelpayoutsCityItem item = new TravelpayoutsCityItem(
                null, "YHR", "CA", true, new TravelpayoutsCityItem.NameTranslations("Chevery"));

        FlightCityEntity entity = FlightCityEntity.from(item);

        assertThat(entity.getKorName()).isNull();
        assertThat(entity.getEngName()).isEqualTo("Chevery");
    }
}

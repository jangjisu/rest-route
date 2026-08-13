package com.restroute.flight.domain;

import static org.assertj.core.api.Assertions.assertThat;

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
}

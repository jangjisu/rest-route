package com.restroute.flight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightCountryEntityTest {

    @Test
    @DisplayName("생성자로 전달한 필드를 그대로 노출한다")
    void constructor_exposesGivenFields() {
        FlightCountryEntity entity = new FlightCountryEntity("JP", "일본", "Japan");

        assertThat(entity.getCode()).isEqualTo("JP");
        assertThat(entity.getKorName()).isEqualTo("일본");
        assertThat(entity.getEngName()).isEqualTo("Japan");
    }
}

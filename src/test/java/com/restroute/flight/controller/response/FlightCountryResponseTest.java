package com.restroute.flight.controller.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.domain.FlightCountryEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightCountryResponseTest {

    @Test
    @DisplayName("엔티티 필드를 응답 DTO로 그대로 매핑한다")
    void from_mapsEntityFields() {
        FlightCountryEntity entity = new FlightCountryEntity("JP", "일본", "Japan");

        FlightCountryResponse response = FlightCountryResponse.from(entity);

        assertThat(response.code()).isEqualTo("JP");
        assertThat(response.korName()).isEqualTo("일본");
        assertThat(response.engName()).isEqualTo("Japan");
    }
}

package com.restroute.flight.controller.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.domain.FlightCityEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightCityResponseTest {

    @Test
    @DisplayName("엔티티 필드를 응답 DTO로 그대로 매핑한다")
    void from_mapsEntityFields() {
        FlightCityEntity entity = new FlightCityEntity("OSA", "Osaka", "JP");

        FlightCityResponse response = FlightCityResponse.from(entity);

        assertThat(response.code()).isEqualTo("OSA");
        assertThat(response.name()).isEqualTo("Osaka");
        assertThat(response.countryCode()).isEqualTo("JP");
    }
}

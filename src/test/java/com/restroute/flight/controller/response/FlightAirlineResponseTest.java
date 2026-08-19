package com.restroute.flight.controller.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.domain.FlightAirlineEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightAirlineResponseTest {

    @Test
    @DisplayName("엔티티 필드를 응답 DTO로 그대로 매핑한다")
    void from_mapsEntityFields() {
        FlightAirlineEntity entity = new FlightAirlineEntity("7C", "제주항공", "Jeju Air", true);

        FlightAirlineResponse response = FlightAirlineResponse.from(entity);

        assertThat(response.code()).isEqualTo("7C");
        assertThat(response.korName()).isEqualTo("제주항공");
        assertThat(response.engName()).isEqualTo("Jeju Air");
    }
}

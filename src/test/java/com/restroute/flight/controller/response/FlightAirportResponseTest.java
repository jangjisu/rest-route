package com.restroute.flight.controller.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.domain.FlightAirportEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightAirportResponseTest {

    @Test
    @DisplayName("엔티티 필드를 응답 DTO로 그대로 매핑한다")
    void from_mapsEntityFields() {
        FlightAirportEntity entity =
                new FlightAirportEntity("ICN", "인천국제공항", "Incheon International Airport", "SEL", "KR");

        FlightAirportResponse response = FlightAirportResponse.from(entity);

        assertThat(response.code()).isEqualTo("ICN");
        assertThat(response.korName()).isEqualTo("인천국제공항");
        assertThat(response.engName()).isEqualTo("Incheon International Airport");
        assertThat(response.cityCode()).isEqualTo("SEL");
        assertThat(response.countryCode()).isEqualTo("KR");
    }
}

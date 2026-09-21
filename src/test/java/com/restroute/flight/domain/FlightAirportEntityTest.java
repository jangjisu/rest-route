package com.restroute.flight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightAirportEntityTest {

    @Test
    @DisplayName("생성자로 전달한 필드를 그대로 노출한다")
    void constructor_exposesGivenFields() {
        FlightAirportEntity entity =
                new FlightAirportEntity("ICN", "인천국제공항", "Incheon International Airport", "SEL", "KR");

        assertThat(entity.getCode()).isEqualTo("ICN");
        assertThat(entity.getKorName()).isEqualTo("인천국제공항");
        assertThat(entity.getEngName()).isEqualTo("Incheon International Airport");
        assertThat(entity.getCityCode()).isEqualTo("SEL");
        assertThat(entity.getCountryCode()).isEqualTo("KR");
    }

    @Test
    @DisplayName("displayName은 한글명이 있으면 한글명을 돌려준다")
    void displayName_returnsKorNameWhenPresent() {
        FlightAirportEntity entity = new FlightAirportEntity("ICN", "인천국제공항", "Incheon", "SEL", "KR");

        assertThat(entity.displayName()).isEqualTo("인천국제공항");
    }

    @Test
    @DisplayName("displayName은 한글명이 없으면 영문명으로 대체한다")
    void displayName_fallsBackToEngNameWhenKorNameMissing() {
        FlightAirportEntity entity = new FlightAirportEntity("ICN", null, "Incheon", "SEL", "KR");

        assertThat(entity.displayName()).isEqualTo("Incheon");
    }
}

package com.restroute.flight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightAirlineEntityTest {

    @Test
    @DisplayName("생성자로 전달한 필드를 그대로 노출한다")
    void constructor_exposesGivenFields() {
        FlightAirlineEntity entity = new FlightAirlineEntity("7C", "제주항공", "Jeju Air", true);

        assertThat(entity.getCode()).isEqualTo("7C");
        assertThat(entity.getKorName()).isEqualTo("제주항공");
        assertThat(entity.getEngName()).isEqualTo("Jeju Air");
        assertThat(entity.isLowCost()).isTrue();
    }

    @Test
    @DisplayName("of는 전달한 값을 그대로 매핑한다")
    void of_mapsGivenValues() {
        FlightAirlineEntity entity = FlightAirlineEntity.of("KE", "대한항공", "Korean Air", false);

        assertThat(entity.getCode()).isEqualTo("KE");
        assertThat(entity.getKorName()).isEqualTo("대한항공");
        assertThat(entity.getEngName()).isEqualTo("Korean Air");
        assertThat(entity.isLowCost()).isFalse();
    }

    @Test
    @DisplayName("displayName은 한글명이 있으면 한글명을 돌려준다")
    void displayName_returnsKorNameWhenPresent() {
        FlightAirlineEntity entity = FlightAirlineEntity.of("7C", "제주항공", "Jeju Air", true);

        assertThat(entity.displayName()).isEqualTo("제주항공");
    }

    @Test
    @DisplayName("displayName은 한글명이 없으면 영문명으로 대체한다")
    void displayName_fallsBackToEngNameWhenKorNameMissing() {
        FlightAirlineEntity entity = FlightAirlineEntity.of("7C", null, "Jeju Air", true);

        assertThat(entity.displayName()).isEqualTo("Jeju Air");
    }
}

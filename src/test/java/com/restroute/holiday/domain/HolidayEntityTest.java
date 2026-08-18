package com.restroute.flight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightHolidayEntityTest {

    @Test
    @DisplayName("of로 전달한 필드를 그대로 노출한다")
    void of_exposesGivenFields() {
        FlightHolidayEntity entity = FlightHolidayEntity.of(LocalDate.of(2026, 9, 26), "대체공휴일");

        assertThat(entity.getHolidayDate()).isEqualTo(LocalDate.of(2026, 9, 26));
        assertThat(entity.getName()).isEqualTo("대체공휴일");
    }
}

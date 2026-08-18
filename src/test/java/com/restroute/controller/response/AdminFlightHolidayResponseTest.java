package com.restroute.controller.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.domain.FlightHolidayEntity;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AdminFlightHolidayResponseTest {

    @Test
    @DisplayName("엔티티 필드를 응답 DTO로 그대로 매핑하고 날짜는 yyyy-MM-dd 문자열로 포맷한다")
    void from_mapsEntityFieldsAndFormatsDate() {
        FlightHolidayEntity entity = FlightHolidayEntity.of(LocalDate.of(2026, 9, 26), "대체공휴일");
        ReflectionTestUtils.setField(entity, "id", 7L);

        AdminFlightHolidayResponse response = AdminFlightHolidayResponse.from(entity);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.date()).isEqualTo("2026-09-26");
        assertThat(response.name()).isEqualTo("대체공휴일");
    }
}

package com.restroute.holiday.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HolidayEntityTest {

    @Test
    @DisplayName("createdByAdmin은 adminOverridden을 true로 만든다 — 배치가 지우지 않아야 함을 표시한다")
    void createdByAdmin_setsAdminOverriddenTrue() {
        HolidayEntity entity = HolidayEntity.createdByAdmin(LocalDate.of(2026, 9, 26), "대체공휴일");

        assertThat(entity.getHolidayDate()).isEqualTo(LocalDate.of(2026, 9, 26));
        assertThat(entity.getName()).isEqualTo("대체공휴일");
        assertThat(entity.isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("syncedFromApi는 adminOverridden을 false로 만든다 — 배치가 지울 수 있음을 표시한다")
    void syncedFromApi_setsAdminOverriddenFalse() {
        HolidayEntity entity = HolidayEntity.syncedFromApi(LocalDate.of(2026, 8, 15), "광복절");

        assertThat(entity.getHolidayDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(entity.getName()).isEqualTo("광복절");
        assertThat(entity.isAdminOverridden()).isFalse();
    }

    @Test
    @DisplayName("isWeekend는 토요일/일요일만 true를 반환한다")
    void isWeekend_returnsTrueOnlyForSaturdayAndSunday() {
        assertThat(HolidayEntity.isWeekend(LocalDate.of(2026, 8, 15))).isTrue(); // 토요일
        assertThat(HolidayEntity.isWeekend(LocalDate.of(2026, 8, 16))).isTrue(); // 일요일
        assertThat(HolidayEntity.isWeekend(LocalDate.of(2026, 8, 17))).isFalse(); // 월요일
    }
}

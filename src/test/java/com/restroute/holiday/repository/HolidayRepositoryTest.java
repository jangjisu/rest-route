package com.restroute.holiday.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.holiday.domain.HolidayEntity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class HolidayRepositoryTest {

    @Autowired
    private HolidayRepository holidayRepository;

    @Test
    @DisplayName("날짜 오름차순으로 전체 공휴일을 반환한다")
    void findAllByOrderByHolidayDateAsc_returnsSortedByDate() {
        holidayRepository.saveAll(List.of(
                HolidayEntity.createdByAdmin(LocalDate.of(2026, 9, 26), "대체공휴일"),
                HolidayEntity.syncedFromApi(LocalDate.of(2026, 1, 1), "신정")));

        List<HolidayEntity> result = holidayRepository.findAllByOrderByHolidayDateAsc();

        assertThat(result).extracting(HolidayEntity::getName).containsExactly("신정", "대체공휴일");
    }

    @Test
    @DisplayName("이미 등록된 날짜면 true를 반환한다")
    void existsByHolidayDate_returnsTrueWhenAlreadyRegistered() {
        holidayRepository.save(HolidayEntity.syncedFromApi(LocalDate.of(2026, 1, 1), "신정"));

        assertThat(holidayRepository.existsByHolidayDate(LocalDate.of(2026, 1, 1)))
                .isTrue();
        assertThat(holidayRepository.existsByHolidayDate(LocalDate.of(2026, 1, 2)))
                .isFalse();
    }

    @Test
    @DisplayName("기간 안에 있어도 관리자가 직접 등록한 행은 제외하고, 배치가 넣은 행만 반환한다")
    void findAllByHolidayDateBetweenAndAdminOverriddenFalse_excludesAdminCreatedRows() {
        holidayRepository.saveAll(List.of(
                HolidayEntity.syncedFromApi(LocalDate.of(2026, 9, 25), "추석"),
                HolidayEntity.createdByAdmin(LocalDate.of(2026, 9, 26), "대체공휴일(관리자 등록)")));

        List<HolidayEntity> result = holidayRepository.findAllByHolidayDateBetweenAndAdminOverriddenFalse(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(result).extracting(HolidayEntity::getName).containsExactly("추석");
    }

    @Test
    @DisplayName("기간 안의 날짜만(관리자 등록 포함) 반환한다 — 신규 저장 후보를 거를 때 한 번에 쓴다")
    void findHolidayDatesBetween_returnsAllDatesRegardlessOfOverride() {
        holidayRepository.saveAll(List.of(
                HolidayEntity.syncedFromApi(LocalDate.of(2026, 9, 25), "추석"),
                HolidayEntity.createdByAdmin(LocalDate.of(2026, 9, 26), "대체공휴일(관리자 등록)"),
                HolidayEntity.syncedFromApi(LocalDate.of(2026, 1, 1), "신정")));

        List<LocalDate> result =
                holidayRepository.findHolidayDatesBetween(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(result).containsExactlyInAnyOrder(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26));
    }

    @Test
    @DisplayName("month가 주어진 목록에 속하는 공휴일만 연도 무관하게 날짜 오름차순으로 반환한다")
    void findAllByMonthInOrderByHolidayDateAsc_filtersByMonthRegardlessOfYear() {
        holidayRepository.saveAll(List.of(
                HolidayEntity.syncedFromApi(LocalDate.of(2026, 9, 25), "추석"),
                HolidayEntity.syncedFromApi(LocalDate.of(2027, 1, 1), "신정"),
                HolidayEntity.createdByAdmin(LocalDate.of(2026, 10, 3), "개천절")));

        List<HolidayEntity> result = holidayRepository.findAllByMonthInOrderByHolidayDateAsc(List.of(9, 10));

        assertThat(result).extracting(HolidayEntity::getName).containsExactly("추석", "개천절");
    }
}

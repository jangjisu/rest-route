package com.restroute.flight.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.domain.FlightHolidayEntity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class FlightHolidayRepositoryTest {

    @Autowired
    private FlightHolidayRepository flightHolidayRepository;

    @Test
    @DisplayName("날짜 오름차순으로 전체 공휴일을 반환한다")
    void findAllByOrderByHolidayDateAsc_returnsSortedByDate() {
        flightHolidayRepository.saveAll(List.of(
                FlightHolidayEntity.of(LocalDate.of(2026, 9, 26), "대체공휴일"),
                FlightHolidayEntity.of(LocalDate.of(2026, 1, 1), "신정")));

        List<FlightHolidayEntity> result = flightHolidayRepository.findAllByOrderByHolidayDateAsc();

        assertThat(result).extracting(FlightHolidayEntity::getName).containsExactly("신정", "대체공휴일");
    }

    @Test
    @DisplayName("이미 등록된 날짜면 true를 반환한다")
    void existsByHolidayDate_returnsTrueWhenAlreadyRegistered() {
        flightHolidayRepository.save(FlightHolidayEntity.of(LocalDate.of(2026, 1, 1), "신정"));

        assertThat(flightHolidayRepository.existsByHolidayDate(LocalDate.of(2026, 1, 1)))
                .isTrue();
        assertThat(flightHolidayRepository.existsByHolidayDate(LocalDate.of(2026, 1, 2)))
                .isFalse();
    }

    @Test
    @DisplayName("기간 안에 속한 공휴일만 반환한다")
    void findAllByHolidayDateBetween_returnsOnlyWithinRange() {
        flightHolidayRepository.saveAll(List.of(
                FlightHolidayEntity.of(LocalDate.of(2026, 9, 25), "추석"),
                FlightHolidayEntity.of(LocalDate.of(2026, 9, 26), "대체공휴일"),
                FlightHolidayEntity.of(LocalDate.of(2026, 10, 3), "개천절")));

        List<FlightHolidayEntity> result = flightHolidayRepository.findAllByHolidayDateBetween(
                LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 30));

        assertThat(result).extracting(FlightHolidayEntity::getName).containsExactlyInAnyOrder("추석", "대체공휴일");
    }
}

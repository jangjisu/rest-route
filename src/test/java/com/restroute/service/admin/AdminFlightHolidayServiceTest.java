package com.restroute.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.controller.request.AdminFlightHolidayRequest;
import com.restroute.controller.response.AdminFlightHolidayResponse;
import com.restroute.holiday.domain.HolidayEntity;
import com.restroute.holiday.repository.HolidayRepository;
import com.restroute.service.admin.exception.DuplicateFlightHolidayException;
import com.restroute.service.admin.exception.FlightHolidayNotFoundException;
import com.restroute.service.admin.exception.InvalidFlightHolidayRequestException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminFlightHolidayServiceTest {

    @Mock
    private HolidayRepository flightHolidayRepository;

    private AdminFlightHolidayService service;

    @BeforeEach
    void setUp() {
        service = new AdminFlightHolidayService(flightHolidayRepository);
    }

    private static HolidayEntity entityWithId(Long id, LocalDate date, String name) {
        HolidayEntity entity = HolidayEntity.of(date, name);
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    @Test
    @DisplayName("날짜 오름차순으로 전체 공휴일을 조회한다")
    void findAll_returnsHolidaysSortedByDate() {
        when(flightHolidayRepository.findAllByOrderByHolidayDateAsc())
                .thenReturn(List.of(entityWithId(1L, LocalDate.of(2026, 1, 1), "신정")));

        List<AdminFlightHolidayResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("신정");
        assertThat(result.get(0).date()).isEqualTo("2026-01-01");
    }

    @Test
    @DisplayName("새 날짜면 공휴일을 등록한다")
    void create_savesNewHoliday() {
        when(flightHolidayRepository.existsByHolidayDate(LocalDate.of(2026, 9, 26)))
                .thenReturn(false);
        when(flightHolidayRepository.save(any(HolidayEntity.class)))
                .thenReturn(entityWithId(5L, LocalDate.of(2026, 9, 26), "대체공휴일"));

        AdminFlightHolidayResponse result = service.create(new AdminFlightHolidayRequest("2026-09-26", "대체공휴일"));

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.date()).isEqualTo("2026-09-26");
        assertThat(result.name()).isEqualTo("대체공휴일");
    }

    @Test
    @DisplayName("이미 등록된 날짜면 중복 등록 예외를 던진다")
    void create_throwsWhenDateAlreadyRegistered() {
        when(flightHolidayRepository.existsByHolidayDate(LocalDate.of(2026, 9, 26)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(new AdminFlightHolidayRequest("2026-09-26", "대체공휴일")))
                .isInstanceOf(DuplicateFlightHolidayException.class);
    }

    @Test
    @DisplayName("date가 비어있으면 검증 예외를 던진다")
    void create_throwsWhenDateBlank() {
        assertThatThrownBy(() -> service.create(new AdminFlightHolidayRequest("", "대체공휴일")))
                .isInstanceOf(InvalidFlightHolidayRequestException.class);
    }

    @Test
    @DisplayName("date 형식이 이상하면 검증 예외를 던진다")
    void create_throwsWhenDateFormatInvalid() {
        assertThatThrownBy(() -> service.create(new AdminFlightHolidayRequest("2026/09/26", "대체공휴일")))
                .isInstanceOf(InvalidFlightHolidayRequestException.class);
    }

    @Test
    @DisplayName("name이 비어있으면 검증 예외를 던진다")
    void create_throwsWhenNameBlank() {
        assertThatThrownBy(() -> service.create(new AdminFlightHolidayRequest("2026-09-26", "  ")))
                .isInstanceOf(InvalidFlightHolidayRequestException.class);
    }

    @Test
    @DisplayName("존재하는 id면 공휴일을 삭제하고 삭제된 정보를 반환한다")
    void delete_removesHolidayAndReturnsIt() {
        HolidayEntity entity = entityWithId(5L, LocalDate.of(2026, 9, 26), "대체공휴일");
        when(flightHolidayRepository.findById(5L)).thenReturn(Optional.of(entity));

        AdminFlightHolidayResponse result = service.delete(5L);

        assertThat(result.date()).isEqualTo("2026-09-26");
        verify(flightHolidayRepository).delete(entity);
    }

    @Test
    @DisplayName("존재하지 않는 id면 찾을 수 없음 예외를 던진다")
    void delete_throwsWhenNotFound() {
        when(flightHolidayRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(FlightHolidayNotFoundException.class);
    }
}

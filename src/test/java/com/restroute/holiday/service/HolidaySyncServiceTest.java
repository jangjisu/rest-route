package com.restroute.holiday.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.holiday.client.SpecialDayClient;
import com.restroute.holiday.client.response.SpecialDayResponse;
import com.restroute.holiday.domain.HolidayEntity;
import com.restroute.holiday.repository.HolidayRepository;
import com.restroute.holiday.service.dto.HolidaySyncResult;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HolidaySyncServiceTest {

    @Mock
    private SpecialDayClient specialDayClient;

    @Mock
    private HolidayRepository holidayRepository;

    private HolidaySyncService service;

    @BeforeEach
    void setUp() {
        service = new HolidaySyncService(specialDayClient, holidayRepository);
    }

    private static void stubExistingHolidays(HolidayRepository repository, LocalDate... existingDates) {
        for (LocalDate date : existingDates) {
            when(repository.existsByHolidayDate(date)).thenReturn(true);
        }
    }

    @Test
    @DisplayName("아직 등록되지 않은 실제 공휴일만(평일 한정) syncedFromApi로 저장하고 저장 건수를 반환한다")
    void syncYear_savesOnlyNewActualHolidays() {
        SpecialDayResponse.Item substituteHoliday = new SpecialDayResponse.Item("20260817", "대체공휴일(광복절)", "Y");
        SpecialDayResponse.Item alreadyRegistered = new SpecialDayResponse.Item("20260101", "신정", "Y");
        SpecialDayResponse.Item notActuallyOff = new SpecialDayResponse.Item("20260706", "제헌절", "N");
        when(specialDayClient.restDaysOfYear(2026))
                .thenReturn(List.of(substituteHoliday, alreadyRegistered, notActuallyOff));
        when(holidayRepository.existsByHolidayDate(LocalDate.of(2026, 8, 17))).thenReturn(false);
        stubExistingHolidays(holidayRepository, LocalDate.of(2026, 1, 1));
        when(holidayRepository.findAllByHolidayDateBetweenAndAdminOverriddenFalse(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of());

        HolidaySyncResult result = service.syncYear(2026);

        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.deletedCount()).isEqualTo(0);
        ArgumentCaptor<HolidayEntity> captor = ArgumentCaptor.forClass(HolidayEntity.class);
        verify(holidayRepository).save(captor.capture());
        assertThat(captor.getValue().getHolidayDate()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(captor.getValue().getName()).isEqualTo("대체공휴일(광복절)");
        assertThat(captor.getValue().isAdminOverridden()).isFalse();
    }

    @Test
    @DisplayName("공공기관 휴일이 아닌(isHoliday=N) 항목은 저장하지 않는다")
    void syncYear_skipsNonHolidayItems() {
        when(specialDayClient.restDaysOfYear(2026))
                .thenReturn(List.of(new SpecialDayResponse.Item("20260706", "제헌절", "N")));
        when(holidayRepository.findAllByHolidayDateBetweenAndAdminOverriddenFalse(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of());

        HolidaySyncResult result = service.syncYear(2026);

        assertThat(result.savedCount()).isEqualTo(0);
        verify(holidayRepository, never()).save(any());
    }

    @Test
    @DisplayName("실제 공휴일(isHoliday=Y)이어도 주말이면 저장하지 않는다 — 주말은 이미 무조건 비근무일이다")
    void syncYear_skipsActualHolidaysThatFallOnWeekend() {
        SpecialDayResponse.Item liberationDaySaturday = new SpecialDayResponse.Item("20260815", "광복절", "Y");
        when(specialDayClient.restDaysOfYear(2026)).thenReturn(List.of(liberationDaySaturday));
        when(holidayRepository.findAllByHolidayDateBetweenAndAdminOverriddenFalse(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of());

        HolidaySyncResult result = service.syncYear(2026);

        assertThat(result.savedCount()).isEqualTo(0);
        verify(holidayRepository, never()).existsByHolidayDate(LocalDate.of(2026, 8, 15));
        verify(holidayRepository, never()).save(any());
    }

    @Test
    @DisplayName("배치가 예전에 넣은 행 중 오늘 응답에 더 이상 없는 날짜는 삭제하고 삭제 건수를 반환한다")
    void syncYear_deletesStaleSyncedHolidaysNoLongerInApiResponse() {
        HolidayEntity staleSyncedHoliday = HolidayEntity.syncedFromApi(LocalDate.of(2026, 7, 6), "제헌절");
        when(specialDayClient.restDaysOfYear(2026)).thenReturn(List.of());
        when(holidayRepository.findAllByHolidayDateBetweenAndAdminOverriddenFalse(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(staleSyncedHoliday));

        HolidaySyncResult result = service.syncYear(2026);

        assertThat(result.deletedCount()).isEqualTo(1);
        verify(holidayRepository).deleteAll(List.of(staleSyncedHoliday));
    }

    @Test
    @DisplayName("관리자가 직접 등록한 행은 오늘 응답에 없어도 삭제 대상에서 아예 제외된다")
    void syncYear_neverConsidersAdminCreatedHolidaysForDeletion() {
        when(specialDayClient.restDaysOfYear(2026)).thenReturn(List.of());
        when(holidayRepository.findAllByHolidayDateBetweenAndAdminOverriddenFalse(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of());

        HolidaySyncResult result = service.syncYear(2026);

        assertThat(result.deletedCount()).isEqualTo(0);
        verify(holidayRepository).deleteAll(List.of());
    }

    @Test
    @DisplayName("오늘 응답에 여전히 있는 평일 날짜는 배치가 넣은 행이어도 삭제하지 않는다")
    void syncYear_keepsSyncedHolidaysStillPresentInApiResponse() {
        HolidayEntity stillCurrentHoliday = HolidayEntity.syncedFromApi(LocalDate.of(2026, 8, 17), "대체공휴일(광복절)");
        when(specialDayClient.restDaysOfYear(2026))
                .thenReturn(List.of(new SpecialDayResponse.Item("20260817", "대체공휴일(광복절)", "Y")));
        stubExistingHolidays(holidayRepository, LocalDate.of(2026, 8, 17));
        when(holidayRepository.findAllByHolidayDateBetweenAndAdminOverriddenFalse(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(stillCurrentHoliday));

        HolidaySyncResult result = service.syncYear(2026);

        assertThat(result.deletedCount()).isEqualTo(0);
        verify(holidayRepository).deleteAll(List.of());
    }

    @Test
    @DisplayName("예전 코드가 주말에 잘못 넣어둔 행은, 그 날짜가 이제 후보에서 걸러지므로 정리(삭제)된다")
    void syncYear_cleansUpLegacyWeekendHolidaysViaReconciliation() {
        HolidayEntity legacyWeekendHoliday = HolidayEntity.syncedFromApi(LocalDate.of(2026, 8, 15), "광복절");
        when(specialDayClient.restDaysOfYear(2026))
                .thenReturn(List.of(new SpecialDayResponse.Item("20260815", "광복절", "Y")));
        when(holidayRepository.findAllByHolidayDateBetweenAndAdminOverriddenFalse(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(legacyWeekendHoliday));

        HolidaySyncResult result = service.syncYear(2026);

        assertThat(result.deletedCount()).isEqualTo(1);
        verify(holidayRepository).deleteAll(List.of(legacyWeekendHoliday));
    }
}

package com.restroute.holiday.service;

import com.restroute.holiday.client.SpecialDayClient;
import com.restroute.holiday.client.response.SpecialDayResponse;
import com.restroute.holiday.domain.HolidayEntity;
import com.restroute.holiday.repository.HolidayRepository;
import com.restroute.holiday.service.dto.HolidaySyncResult;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공공데이터포털 특일 정보(getRestDeInfo)에서 그 해의 실제 공휴일(대체공휴일 포함)을 가져와
 * flight_holiday를 최신 상태로 맞춘다 — API 응답에 있는데 우리 DB에 없는 날짜는 채워 넣고,
 * 예전에 이 동기화가 채워 넣었던 날짜인데 오늘 응답엔 더 이상 없으면(취소·정정) 지운다.
 *
 * <p>관리자가 admin 페이지에서 직접 등록한 행({@code adminOverridden=true})은 삭제 대상에서
 * 항상 제외한다 — 실제 값의 최종 권한은 관리자 화면에 있고, 이 동기화는 그걸 침범하지 않는다.
 *
 * <p>주말은 이미 무조건 비근무일로 판정되므로, API 응답에 있어도 저장하지 않는다(연차/공휴일
 * 배지 계산에 이 테이블을 조회 목적이 아니라 판정 목적으로만 쓰기 때문에 중복 저장할 이유가 없다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidaySyncService {

    private static final DateTimeFormatter LOCDATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final SpecialDayClient specialDayClient;
    private final HolidayRepository holidayRepository;

    @Transactional
    public HolidaySyncResult syncYear(int year) {
        List<SpecialDayResponse.Item> actualHolidayItems = specialDayClient.restDaysOfYear(year).stream()
                .filter(SpecialDayResponse.Item::isActualHoliday)
                .toList();

        if (actualHolidayItems.isEmpty()) {
            log.warn(
                    "Special day API returned no actual holidays for year={}; skipping sync to avoid mass delete.",
                    year);
            return HolidaySyncResult.of(0, 0);
        }

        List<HolidayCandidate> candidates = actualHolidayItems.stream()
                .map(item -> new HolidayCandidate(LocalDate.parse(item.locdate(), LOCDATE_FORMAT), item.dateName()))
                .filter(candidate -> !HolidayEntity.isWeekend(candidate.date()))
                .toList();
        Set<LocalDate> apiDates =
                candidates.stream().map(HolidayCandidate::date).collect(Collectors.toSet());
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        Set<LocalDate> existingDates = Set.copyOf(holidayRepository.findHolidayDatesBetween(yearStart, yearEnd));

        int deletedCount = deleteStaleSyncedHolidays(yearStart, yearEnd, apiDates);
        int savedCount = saveNewHolidays(candidates, existingDates);
        return HolidaySyncResult.of(savedCount, deletedCount);
    }

    private int deleteStaleSyncedHolidays(LocalDate yearStart, LocalDate yearEnd, Set<LocalDate> apiDates) {
        List<HolidayEntity> staleHolidays =
                holidayRepository.findAllByHolidayDateBetweenAndAdminOverriddenFalse(yearStart, yearEnd).stream()
                        .filter(holiday -> !apiDates.contains(holiday.getHolidayDate()))
                        .toList();
        holidayRepository.deleteAll(staleHolidays);
        return staleHolidays.size();
    }

    private int saveNewHolidays(List<HolidayCandidate> candidates, Set<LocalDate> existingDates) {
        int savedCount = 0;
        for (HolidayCandidate candidate : candidates) {
            if (existingDates.contains(candidate.date())) {
                continue;
            }
            holidayRepository.save(HolidayEntity.syncedFromApi(candidate.date(), candidate.name()));
            savedCount++;
        }
        return savedCount;
    }

    private record HolidayCandidate(LocalDate date, String name) {}
}

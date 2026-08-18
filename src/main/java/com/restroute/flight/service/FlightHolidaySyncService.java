package com.restroute.flight.service;

import com.restroute.flight.client.SpecialDayClient;
import com.restroute.flight.client.response.SpecialDayResponse;
import com.restroute.flight.domain.FlightHolidayEntity;
import com.restroute.flight.repository.FlightHolidayRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공공데이터포털 특일 정보(getRestDeInfo)에서 그 해의 실제 공휴일(대체공휴일 포함)을 가져와
 * flight_holiday를 채운다. 이미 등록된 날짜는(관리자가 직접 넣었든, 이전 동기화로 들어왔든)
 * 건드리지 않는다 — 이 동기화는 "빈 곳만 채우는" 역할이고, 실제 값의 최종 권한은 관리자 화면에
 * 있다(자동 배치가 놓치거나 늦게 반영해도 관리자가 직접 보정할 수 있어야 하기 때문).
 */
@Service
@RequiredArgsConstructor
public class FlightHolidaySyncService {

    private static final DateTimeFormatter LOCDATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final SpecialDayClient specialDayClient;
    private final FlightHolidayRepository flightHolidayRepository;

    @Transactional
    public int syncYear(int year) {
        int savedCount = 0;
        for (SpecialDayResponse.Item item : specialDayClient.restDaysOfYear(year)) {
            if (!item.isActualHoliday()) {
                continue;
            }
            LocalDate date = LocalDate.parse(item.locdate(), LOCDATE_FORMAT);
            if (flightHolidayRepository.existsByHolidayDate(date)) {
                continue;
            }
            flightHolidayRepository.save(FlightHolidayEntity.of(date, item.dateName()));
            savedCount++;
        }
        return savedCount;
    }
}

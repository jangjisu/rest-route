package com.restroute.flight.service;

import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.service.util.FlightDealResponses;
import com.restroute.holiday.domain.HolidayEntity;
import com.restroute.holiday.repository.HolidayRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 필터를 통과한 딜마다, 출발일~귀국일(양끝 포함) 사이에 걸리는 비근무일(공휴일·주말)을
 * {@link FlightDealResponse.HolidayDay} 목록으로 채운다. 공휴일이면 {@code name}이 채워지고,
 * 순수 주말(공휴일 테이블에 없는 토/일)이면 {@code name}이 {@code null}이다.
 *
 * <p>연차 사용일수 같은 파생값은 이 목록만 있으면 프론트에서 계산할 수 있어서 여기서는 만들지
 * 않는다.
 *
 * <p>딜 전체의 (출발일~귀국일) 범위를 모아 최소~최대로 공휴일을 한 번에 조회한다({@link
 * FlightDealPostFilter}와 같은 이유로 건별 조회를 피한다).
 */
@Component
@RequiredArgsConstructor
class FlightDealHolidayEnricher {

    private final HolidayRepository holidayRepository;

    List<FlightDealResponse> enrich(List<FlightDealResponse> items) {
        if (items.isEmpty()) {
            return items;
        }
        Map<LocalDate, String> holidayNameByDate = holidayNameByDateFor(items);
        return items.stream()
                .map(item -> FlightDealResponses.withHolidays(item, holidaysOf(item, holidayNameByDate)))
                .toList();
    }

    private Map<LocalDate, String> holidayNameByDateFor(List<FlightDealResponse> items) {
        List<LocalDate> departureDates =
                items.stream().map(FlightDealResponses::departureDateOf).toList();
        List<LocalDate> returnDates =
                items.stream().map(FlightDealResponses::returnDateOf).toList();
        LocalDate from = Collections.min(departureDates);
        LocalDate to = Collections.max(returnDates);

        Map<LocalDate, String> holidayNameByDate = new HashMap<>();
        for (HolidayEntity holiday : holidayRepository.findAllByHolidayDateBetween(from, to)) {
            holidayNameByDate.put(holiday.getHolidayDate(), holiday.getName());
        }
        return holidayNameByDate;
    }

    private static List<FlightDealResponse.HolidayDay> holidaysOf(
            FlightDealResponse item, Map<LocalDate, String> holidayNameByDate) {
        LocalDate departureDate = FlightDealResponses.departureDateOf(item);
        LocalDate returnDate = FlightDealResponses.returnDateOf(item);

        List<FlightDealResponse.HolidayDay> holidays = new ArrayList<>();
        for (LocalDate date = departureDate; !date.isAfter(returnDate); date = date.plusDays(1)) {
            String holidayName = holidayNameByDate.get(date);
            if (holidayName != null) {
                holidays.add(new FlightDealResponse.HolidayDay(date.toString(), holidayName));
                continue;
            }
            if (HolidayEntity.isWeekend(date)) {
                holidays.add(new FlightDealResponse.HolidayDay(date.toString(), null));
            }
        }
        return holidays;
    }
}

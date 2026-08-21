package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.service.util.FlightDealResponses;
import com.restroute.holiday.domain.HolidayEntity;
import com.restroute.holiday.repository.HolidayRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link FlightDealResponseMapper}가 만든 목록에서 항목을 제외(exclude)만 한다 — 값을 채우는
 * 건({@link FlightDealHolidayEnricher}) 이 클래스의 일이 아니다. 전체 최저가 표시({@link
 * FlightDealResponses#markLowestInRange})도 여기서 하지 않는다 — 필터로 최저가 항목이 빠질 수
 * 있으므로, 이 필터를 다 거친 다음에 표시해야 한다.
 *
 * <p>두 종류의 제외 기준이 섞여 있다: 실제 요청한 dateFrom~dateTo 밖 항목 제외는 사용자 조건과
 * 무관한 데이터 보정(RANGE 조회가 달 단위로 응답을 줘서 생기는 여분을 자름 — FIXED는 애초에
 * 정확한 날짜만 오므로 이 필터가 항상 무해하게 통과한다)이고, 나머지 셋(includeWeekend/
 * includeHoliday/includeTransfer)은 사용자가 요청 파라미터로 켜고 끄는 조건이다. 주말·공휴일
 * 판정은 출발일(departure leg의 날짜) 기준이다 — 실제 인벤토리가 그 조합밖에 안 줘서 귀국일은
 * 별도로 보지 않는다.
 */
@Component
@RequiredArgsConstructor
class FlightDealPostFilter {

    private final HolidayRepository holidayRepository;

    List<FlightDealResponse> apply(List<FlightDealResponse> items, FlightSearchRequestDto request) {
        List<FlightDealResponse> filtered =
                withinRequestedDateRange(items, request.parsedDateFrom(), request.parsedDateTo());
        if (!request.isIncludeTransfer()) {
            filtered = withoutTransfers(filtered);
        }
        if (!request.isIncludeWeekend()) {
            filtered = withoutWeekendDepartures(filtered);
        }
        if (!request.isIncludeHoliday()) {
            filtered = withoutHolidayDepartures(filtered);
        }
        return filtered;
    }

    private static List<FlightDealResponse> withinRequestedDateRange(
            List<FlightDealResponse> items, LocalDate dateFrom, LocalDate dateTo) {
        return items.stream()
                .filter(item -> {
                    LocalDate departureDate = FlightDealResponses.departureDateOf(item);
                    return !departureDate.isBefore(dateFrom) && !departureDate.isAfter(dateTo);
                })
                .toList();
    }

    private static List<FlightDealResponse> withoutTransfers(List<FlightDealResponse> items) {
        return items.stream()
                .filter(item ->
                        item.departure().transferCount() == 0 && item.arrival().transferCount() == 0)
                .toList();
    }

    private static List<FlightDealResponse> withoutWeekendDepartures(List<FlightDealResponse> items) {
        return items.stream()
                .filter(item -> !HolidayEntity.isWeekend(FlightDealResponses.departureDateOf(item)))
                .toList();
    }

    /** 후보 항목들의 출발일 범위만큼만 한 번에 조회해서 걸러낸다 — 건별 조회(N+1)를 피한다. */
    private List<FlightDealResponse> withoutHolidayDepartures(List<FlightDealResponse> items) {
        if (items.isEmpty()) {
            return items;
        }
        List<LocalDate> departureDates =
                items.stream().map(FlightDealResponses::departureDateOf).toList();
        Set<LocalDate> holidayDates = new HashSet<>(holidayRepository.findHolidayDatesBetween(
                Collections.min(departureDates), Collections.max(departureDates)));
        return items.stream()
                .filter(item -> !holidayDates.contains(FlightDealResponses.departureDateOf(item)))
                .toList();
    }
}

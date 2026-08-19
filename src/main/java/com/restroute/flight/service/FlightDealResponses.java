package com.restroute.flight.service;

import com.restroute.flight.controller.response.FlightDealResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

/** mock/실 연동 양쪽에서 공통으로 쓰는 {@link FlightDealResponse} 조립·후처리. */
final class FlightDealResponses {

    private static final DateTimeFormatter LEG_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /** mock은 실제 공휴일 달력 연동이 없어 항상 빈 목록이다. */
    static final List<FlightDealResponse.HolidayDay> NO_HOLIDAYS = List.of();

    private FlightDealResponses() {}

    /** id는 세션 토큰 + 순번(1부터, 4자리)으로 매긴다 — mock/실 연동 공통 형식. */
    static String idOf(String sessionToken, int index) {
        return "%s_%04d".formatted(sessionToken, index + 1);
    }

    /** departAt/arriveAt을 표준 포맷으로 찍어 Leg를 만든다 — arriveAt 계산 방식은 호출부마다 다를 수 있어 인자로 받는다. */
    static FlightDealResponse.Leg legOf(
            OffsetDateTime departAt, OffsetDateTime arriveAt, int durationMinutes, int transferCount) {
        return new FlightDealResponse.Leg(
                LEG_TIME_FORMAT.format(departAt), LEG_TIME_FORMAT.format(arriveAt), durationMinutes, transferCount);
    }

    /** {@link FlightDealPostFilter}/{@link FlightDealHolidayEnricher}가 공통으로 쓰는 출발일 파싱. */
    static LocalDate departureDateOf(FlightDealResponse item) {
        return OffsetDateTime.parse(item.departure().departAt()).toLocalDate();
    }

    /** 귀국편이 출발하는 날짜(=여행에서 돌아오는 날) 파싱. */
    static LocalDate returnDateOf(FlightDealResponse item) {
        return OffsetDateTime.parse(item.arrival().departAt()).toLocalDate();
    }

    /** 가격 기준 전체 최저가 한 건에만 isLowestInRange를 true로 표시한다(동가면 첫 항목 하나만). */
    static List<FlightDealResponse> markLowestInRange(List<FlightDealResponse> items) {
        if (items.isEmpty()) {
            return items;
        }
        int lowestIndex = 0;
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).price().amount() < items.get(lowestIndex).price().amount()) {
                lowestIndex = i;
            }
        }
        int finalLowestIndex = lowestIndex;
        return IntStream.range(0, items.size())
                .mapToObj(i -> i == finalLowestIndex ? withLowestInRange(items.get(i)) : items.get(i))
                .toList();
    }

    private static FlightDealResponse withLowestInRange(FlightDealResponse deal) {
        return new FlightDealResponse(
                deal.id(),
                deal.destination(),
                deal.departure(),
                deal.arrival(),
                deal.nights(),
                deal.holidays(),
                deal.airline(),
                deal.price(),
                true,
                deal.gateName(),
                deal.bookingLink(),
                deal.seatsLeft());
    }

    /** {@link FlightDealHolidayEnricher}가 holidays 필드만 실제 값으로 바꿔 끼울 때 쓴다. */
    static FlightDealResponse withHolidays(FlightDealResponse deal, List<FlightDealResponse.HolidayDay> holidays) {
        return new FlightDealResponse(
                deal.id(),
                deal.destination(),
                deal.departure(),
                deal.arrival(),
                deal.nights(),
                holidays,
                deal.airline(),
                deal.price(),
                deal.isLowestInRange(),
                deal.gateName(),
                deal.bookingLink(),
                deal.seatsLeft());
    }
}

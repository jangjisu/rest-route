package com.restroute.flight.service.util;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.holiday.domain.HolidayEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import org.springframework.util.StringUtils;

/**
 * 프론트엔드 개발용 고정 모킹 데이터 생성기. Travelpayouts grouped_prices가 실제로 줄 수 있는
 * 필드만으로 결정적(deterministic) 가짜 데이터를 만든다.
 *
 * <p>id는 세션 토큰을 아직 몰라서 여기서는 채우지 않는다 — 세션별 저장/조회, cursor lookup을
 * 담당하는 세션 스토어가 최종 저장 직전에 부여한다. 여기는 순수하게 "이 세션의 몇 번째 항목이
 * 어떤 값인지"만 계산한다.
 *
 * <p>includeWeekend는 실 경로({@link com.restroute.flight.service.FlightDealPostFilter})와
 * 동일하게 적용한다. includeHoliday는 적용하지 않는다 — 실제 공휴일 이름은 DB(HolidayRepository)에서
 * 와야 하는데, 이 클래스는 의도적으로 DB 의존 없는 순수 static 유틸이라 값을 모른다. holidays
 * 필드는 주말만 채운다({@code name=null}) — 실 응답의 holidays 구조와 모양은 맞춘다.
 */
public final class FlightSearchMockFixture {

    private static final int BASE_PRICE = 89000;
    private static final int PRICE_STEP = 7300;
    private static final int PRICE_CYCLE = 15;
    private static final int BASE_DURATION_MINUTES = 90;
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    private static final List<Destination> DESTINATIONS = List.of(
            new Destination("FUK", "후쿠오카", ZoneOffset.ofHours(9)),
            new Destination("KIX", "오사카", ZoneOffset.ofHours(9)),
            new Destination("OKA", "오키나와", ZoneOffset.ofHours(9)),
            new Destination("NGO", "나고야", ZoneOffset.ofHours(9)),
            new Destination("TYO", "도쿄", ZoneOffset.ofHours(9)),
            new Destination("BKK", "방콕", ZoneOffset.ofHours(7)),
            new Destination("DAD", "다낭", ZoneOffset.ofHours(7)),
            new Destination("SGN", "호치민", ZoneOffset.ofHours(7)),
            new Destination("PQC", "푸꾸옥", ZoneOffset.ofHours(7)),
            new Destination("GUM", "괌", ZoneOffset.ofHours(10)));

    private static final List<Airline> AIRLINES = List.of(
            new Airline("LJ", "진에어"),
            new Airline("7C", "제주항공"),
            new Airline("TW", "티웨이항공"),
            new Airline("WE", "타이스마일항공"));

    /** 예약처는 지금 Aviasales 하나뿐이다 — mock도 실제와 동일하게 고정값을 쓴다. */
    private static final String GATE_NAME = "Aviasales";

    private static final String PENDING_ID = "";

    private FlightSearchMockFixture() {}

    public static List<FlightDealResponse> generateAll(FlightSearchRequestDto request, int totalSize) {
        List<FlightDealResponse> items = IntStream.range(0, totalSize)
                .mapToObj(index -> dealAt(index, request))
                .filter(deal -> request.isIncludeWeekend() || !isWeekendDeparture(deal))
                .toList();
        return FlightDealResponses.markLowestInRange(items);
    }

    private static boolean isWeekendDeparture(FlightDealResponse deal) {
        return HolidayEntity.isWeekend(FlightDealResponses.departureDateOf(deal));
    }

    private static FlightDealResponse dealAt(int index, FlightSearchRequestDto request) {
        Destination destination = destinationAt(index, request);
        int nights = nightsAt(index, request);
        LocalDate departureDate = departureDateAt(index, request);
        LocalDate returnDate = departureDate.plusDays(nights);
        Airline airline = AIRLINES.get(index % AIRLINES.size());
        int amount = BASE_PRICE + (index % PRICE_CYCLE) * PRICE_STEP;
        int departureDuration = BASE_DURATION_MINUTES + (index % 5) * 10;
        int arrivalDuration = BASE_DURATION_MINUTES + (index % 4) * 10;
        int departureTransferCount = request.isIncludeTransfer() && index % 3 == 0 ? 1 : 0;
        int arrivalTransferCount = request.isIncludeTransfer() && index % 4 == 0 ? 1 : 0;

        FlightDealResponse.Leg departure = legAt(
                departureDate.atTime(9, 20).atOffset(KST),
                destination.offset(),
                departureDuration,
                departureTransferCount);
        FlightDealResponse.Leg arrival = legAt(
                returnDate.atTime(13, 10).atOffset(destination.offset()), KST, arrivalDuration, arrivalTransferCount);

        return new FlightDealResponse(
                PENDING_ID,
                new FlightDealResponse.Destination(destination.code(), destination.name()),
                departure,
                arrival,
                nights,
                weekendHolidaysOf(departureDate, returnDate),
                new FlightDealResponse.Airline(airline.code(), airline.name(), true),
                new FlightDealResponse.Price(amount, "KRW"),
                false,
                GATE_NAME,
                "https://www.aviasales.com/search/mock-" + index,
                null);
    }

    /** 출발일~귀국일 사이의 주말만 채운다(name=null) — 실제 공휴일 이름은 DB 없이는 알 수 없다. */
    private static List<FlightDealResponse.HolidayDay> weekendHolidaysOf(
            LocalDate departureDate, LocalDate returnDate) {
        List<FlightDealResponse.HolidayDay> holidays = new ArrayList<>();
        for (LocalDate date = departureDate; !date.isAfter(returnDate); date = date.plusDays(1)) {
            if (HolidayEntity.isWeekend(date)) {
                holidays.add(new FlightDealResponse.HolidayDay(date.toString(), null));
            }
        }
        return holidays;
    }

    private static FlightDealResponse.Leg legAt(
            OffsetDateTime departAt, ZoneOffset arrivalOffset, int duration, int transferCount) {
        OffsetDateTime arriveAt = departAt.plusMinutes(duration).withOffsetSameInstant(arrivalOffset);
        return FlightDealResponses.legOf(departAt, arriveAt, duration, transferCount);
    }

    private static Destination destinationAt(int index, FlightSearchRequestDto request) {
        if (StringUtils.hasText(request.destination())) {
            String code = request.destination().toUpperCase(Locale.ROOT);
            return DESTINATIONS.stream()
                    .filter(destination -> destination.code().equals(code))
                    .findFirst()
                    .orElseGet(() -> new Destination(code, code, KST));
        }
        return DESTINATIONS.get(index % DESTINATIONS.size());
    }

    private static int nightsAt(int index, FlightSearchRequestDto request) {
        List<Integer> nights = request.parsedNights();
        return nights.get(index % nights.size());
    }

    private static LocalDate departureDateAt(int index, FlightSearchRequestDto request) {
        LocalDate dateFrom = request.parsedDateFrom();
        long rangeDays = ChronoUnit.DAYS.between(dateFrom, request.parsedDateTo()) + 1;
        long offset = index % rangeDays;
        return dateFrom.plusDays(offset);
    }

    private record Destination(String code, String name, ZoneOffset offset) {}

    private record Airline(String code, String name) {}
}

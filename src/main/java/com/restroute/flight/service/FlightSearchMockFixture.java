package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import org.springframework.util.StringUtils;

/**
 * 프론트엔드 개발용 고정 모킹 데이터 생성기. Travelpayouts grouped_prices가 실제로 줄 수 있는
 * 필드만으로 결정적(deterministic) 가짜 데이터를 만든다.
 *
 * <p>id는 세션 토큰(4자리) + 순번(예: "aB3x_0004")으로 구성된다. 세션별 저장/조회, cursor
 * lookup은 이 클래스가 아니라 {@link FlightDealSessionStore}가 담당한다 — 여기는 순수하게
 * "이 세션의 몇 번째 항목이 어떤 값인지"만 계산한다.
 */
final class FlightSearchMockFixture {

    private static final int BASE_PRICE = 89000;
    private static final int PRICE_STEP = 7300;
    private static final int PRICE_CYCLE = 15;
    private static final int BASE_DURATION_MINUTES = 90;
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);
    private static final DateTimeFormatter LEG_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

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

    /** 실제 공휴일 달력 연동 전까지는 항상 0/빈 값이다 — 연휴 배지 계산은 별도 작업. */
    private static final FlightDealResponse.Holiday NO_HOLIDAY = new FlightDealResponse.Holiday(0, List.of(), 0);

    private FlightSearchMockFixture() {}

    static List<FlightDealResponse> generateAll(FlightSearchRequestDto request, String sessionToken, int totalSize) {
        List<FlightDealResponse> items = IntStream.range(0, totalSize)
                .mapToObj(index -> dealAt(index, request, sessionToken))
                .toList();
        return markLowestInRange(items);
    }

    /** 가격 기준 전체 최저가 한 건에만 isLowestInRange를 true로 표시한다(동가면 첫 항목 하나만). */
    private static List<FlightDealResponse> markLowestInRange(List<FlightDealResponse> items) {
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
                deal.holiday(),
                deal.airline(),
                deal.price(),
                true,
                deal.gateName(),
                deal.bookingLink(),
                deal.seatsLeft());
    }

    private static FlightDealResponse dealAt(int index, FlightSearchRequestDto request, String sessionToken) {
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
        String id = idOf(sessionToken, index);

        FlightDealResponse.Leg departure = legAt(
                departureDate.atTime(9, 20).atOffset(KST),
                destination.offset(),
                departureDuration,
                departureTransferCount);
        FlightDealResponse.Leg arrival = legAt(
                returnDate.atTime(13, 10).atOffset(destination.offset()), KST, arrivalDuration, arrivalTransferCount);

        return new FlightDealResponse(
                id,
                new FlightDealResponse.Destination(destination.code(), destination.name()),
                departure,
                arrival,
                nights,
                NO_HOLIDAY,
                new FlightDealResponse.Airline(airline.code(), airline.name(), true),
                new FlightDealResponse.Price(amount, "KRW"),
                false,
                GATE_NAME,
                "https://www.aviasales.com/search/mock-" + id,
                null);
    }

    private static FlightDealResponse.Leg legAt(
            OffsetDateTime departAt, ZoneOffset arrivalOffset, int duration, int transferCount) {
        OffsetDateTime arriveAt = departAt.plusMinutes(duration).withOffsetSameInstant(arrivalOffset);
        return new FlightDealResponse.Leg(
                LEG_TIME_FORMAT.format(departAt), LEG_TIME_FORMAT.format(arriveAt), duration, transferCount);
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

    private static String idOf(String sessionToken, int index) {
        return "%s_%04d".formatted(sessionToken, index + 1);
    }

    private record Destination(String code, String name, ZoneOffset offset) {}

    private record Airline(String code, String name) {}
}

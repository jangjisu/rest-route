package com.restroute.flight.controller;

import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchMeta;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.util.StringUtils;

/**
 * 프론트엔드 개발용 고정 모킹 데이터. Travelpayouts grouped_prices가 실제로 줄 수 있는
 * 필드만으로 결정적(deterministic) 가짜 데이터를 생성한다.
 *
 * <p>id는 인덱스를 base64로 인코딩해서 만든다 — 커서를 받으면 그 문자열을 디코드해서 위치를
 * 역산하지 않고, 생성된 목록에서 실제로 찾아서(lookup) 위치를 구한다. 나중에 진짜 백엔드 id로
 * 바뀌어도 이 lookup 방식은 그대로 통한다.
 */
final class FlightSearchMockFixture {

    private static final int TOTAL_COUNT = 342;
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

    private static final List<String> GATES = List.of("Trip.com", "Kupi.com", "Mytrip.com");

    private FlightSearchMockFixture() {}

    static FlightDealSearchResponse page(
            FlightSearchRequestValidator.ValidatedRequest request, String cursor, int size) {
        List<FlightDealResponse> all = generateAll(request);
        Map<String, Integer> indexById = buildIndex(all);

        int startIndex = startIndexOf(cursor, indexById);
        int endIndex = Math.min(startIndex + size, all.size());
        List<FlightDealResponse> items = all.subList(startIndex, endIndex);
        boolean hasNext = endIndex < all.size();
        String nextCursor = hasNext ? items.get(items.size() - 1).id() : null;

        return new FlightDealSearchResponse(items, new FlightDealSearchMeta(nextCursor, hasNext, all.size()));
    }

    private static List<FlightDealResponse> generateAll(FlightSearchRequestValidator.ValidatedRequest request) {
        return IntStream.range(0, TOTAL_COUNT)
                .mapToObj(index -> dealAt(index, request))
                .toList();
    }

    private static Map<String, Integer> buildIndex(List<FlightDealResponse> all) {
        Map<String, Integer> indexById = new LinkedHashMap<>();
        for (int i = 0; i < all.size(); i++) {
            indexById.put(all.get(i).id(), i);
        }
        return indexById;
    }

    private static int startIndexOf(String cursor, Map<String, Integer> indexById) {
        if (cursor == null) {
            return 0;
        }

        Integer index = indexById.get(cursor);
        if (index == null) {
            throw new FlightDealNotFoundException(cursor);
        }
        return index + 1;
    }

    private static FlightDealResponse dealAt(int index, FlightSearchRequestValidator.ValidatedRequest request) {
        Destination destination = destinationAt(index, request);
        int nights = nightsAt(index, request);
        LocalDate departureDate = departureDateAt(index, request);
        LocalDate returnDate = departureDate.plusDays(nights);
        Airline airline = AIRLINES.get(index % AIRLINES.size());
        int amount = BASE_PRICE + (index % PRICE_CYCLE) * PRICE_STEP;
        int departureDuration = BASE_DURATION_MINUTES + (index % 5) * 10;
        int arrivalDuration = BASE_DURATION_MINUTES + (index % 4) * 10;
        int departureTransferCount = request.includeTransfer() && index % 3 == 0 ? 1 : 0;
        int arrivalTransferCount = request.includeTransfer() && index % 4 == 0 ? 1 : 0;
        String id = idOf(index);

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
                new FlightDealResponse.Airline(airline.code(), airline.name()),
                new FlightDealResponse.Price(amount, "KRW"),
                GATES.get(index % GATES.size()),
                "https://www.aviasales.com/search/mock-" + id);
    }

    private static FlightDealResponse.Leg legAt(
            OffsetDateTime departureFrom, ZoneOffset arrivalOffset, int duration, int transferCount) {
        OffsetDateTime departTo = departureFrom.plusMinutes(duration).withOffsetSameInstant(arrivalOffset);
        return new FlightDealResponse.Leg(
                LEG_TIME_FORMAT.format(departureFrom), LEG_TIME_FORMAT.format(departTo), duration, transferCount);
    }

    private static Destination destinationAt(int index, FlightSearchRequestValidator.ValidatedRequest request) {
        if (StringUtils.hasText(request.destination())) {
            String code = request.destination().toUpperCase(Locale.ROOT);
            return DESTINATIONS.stream()
                    .filter(destination -> destination.code().equals(code))
                    .findFirst()
                    .orElseGet(() -> new Destination(code, code, KST));
        }
        return DESTINATIONS.get(index % DESTINATIONS.size());
    }

    private static int nightsAt(int index, FlightSearchRequestValidator.ValidatedRequest request) {
        List<Integer> nights = request.nights();
        return nights.get(index % nights.size());
    }

    private static LocalDate departureDateAt(int index, FlightSearchRequestValidator.ValidatedRequest request) {
        long rangeDays = ChronoUnit.DAYS.between(request.dateFrom(), request.dateTo()) + 1;
        long offset = index % rangeDays;
        return request.dateFrom().plusDays(offset);
    }

    private static String idOf(int index) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(String.valueOf(index).getBytes(StandardCharsets.UTF_8));
    }

    private record Destination(String code, String name, ZoneOffset offset) {}

    private record Airline(String code, String name) {}
}

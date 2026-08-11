package com.restroute.flight.controller;

import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchMeta;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
 * <p>id는 인덱스를 그대로 노출하는 대신(예: deal_00000) 커서가 정말 불투명한 값이 되도록
 * 섞어서 만들고, 커서를 받으면 그 문자열을 파싱해서 위치를 역산하지 않고 생성된 목록에서
 * 실제로 찾아서 위치를 구한다 — 나중에 진짜 백엔드 id로 바뀌어도 이 방식은 그대로 통한다.
 */
final class FlightSearchMockFixture {

    private static final int TOTAL_COUNT = 342;
    private static final int BASE_PRICE = 89000;
    private static final int PRICE_STEP = 7300;
    private static final int PRICE_CYCLE = 15;
    private static final int BASE_DURATION_MINUTES = 90;
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);
    private static final DateTimeFormatter DEPARTURE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final List<Destination> DESTINATIONS = List.of(
            new Destination("FUK", "Fukuoka"),
            new Destination("KIX", "Osaka"),
            new Destination("OKA", "Okinawa"),
            new Destination("NGO", "Nagoya"),
            new Destination("TYO", "Tokyo"),
            new Destination("BKK", "Bangkok"),
            new Destination("DAD", "Da Nang"),
            new Destination("SGN", "Ho Chi Minh City"),
            new Destination("PQC", "Phu Quoc"),
            new Destination("GUM", "Guam"));

    private static final List<String> AIRLINES = List.of("LJ", "7C", "TW", "WE");
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
        String airline = AIRLINES.get(index % AIRLINES.size());
        int amount = BASE_PRICE + (index % PRICE_CYCLE) * PRICE_STEP;
        int durationMinutes = BASE_DURATION_MINUTES + (index % 5) * 10;
        int returnDurationMinutes = BASE_DURATION_MINUTES + (index % 4) * 10;

        OffsetDateTime departureAt = departureDate.atTime(9, 20).atOffset(KST);
        OffsetDateTime returnAt = returnDate.atTime(11, 20).atOffset(KST);
        String id = idOf(index);

        return new FlightDealResponse(
                id,
                new FlightDealResponse.Destination(destination.iata(), destination.city()),
                DEPARTURE_FORMAT.format(departureAt),
                DEPARTURE_FORMAT.format(returnAt),
                nights,
                index % 3 == 0 ? 1 : 0,
                index % 4 == 0 ? 1 : 0,
                airline,
                airline + (100 + index % 900),
                durationMinutes,
                returnDurationMinutes,
                new FlightDealResponse.Price(amount, "KRW"),
                GATES.get(index % GATES.size()),
                "https://www.aviasales.com/search/mock-" + id);
    }

    private static Destination destinationAt(int index, FlightSearchRequestValidator.ValidatedRequest request) {
        if (StringUtils.hasText(request.destination())) {
            String iata = request.destination().toUpperCase(Locale.ROOT);
            return new Destination(iata, iata);
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
        long mixed = mix(index);
        String token = Long.toUnsignedString(mixed, 36).toUpperCase(Locale.ROOT);
        String padded = token.length() >= 8 ? token.substring(0, 8) : "0".repeat(8 - token.length()) + token;
        return "deal_" + padded;
    }

    private static long mix(int index) {
        long x = index + 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    private record Destination(String iata, String city) {}
}

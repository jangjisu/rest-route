package com.restroute.flight.controller;

import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchMeta;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 프론트엔드 개발용 고정 모킹 데이터.
 * 실제 검색/정렬 비즈니스 로직은 아직 확정되지 않아, 커서 기반 페이지네이션을
 * 검증할 수 있을 만큼 충분히 많은(TOTAL_COUNT) 결정적(deterministic) 가짜 데이터만 생성한다.
 */
final class FlightSearchMockFixture {

    private static final int TOTAL_COUNT = 342;
    private static final int BASE_PRICE = 89000;
    private static final int PRICE_STEP = 7300;
    private static final int PRICE_CYCLE = 15;
    private static final OffsetDateTime BASE_DEPARTURE =
            OffsetDateTime.of(2026, 9, 1, 9, 20, 0, 0, ZoneOffset.ofHours(9));
    private static final DateTimeFormatter DEPARTURE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final List<Destination> DESTINATIONS = List.of(
            new Destination("FUK", "후쿠오카"),
            new Destination("KIX", "오사카"),
            new Destination("OKA", "오키나와"),
            new Destination("NGO", "나고야"),
            new Destination("TYO", "도쿄"),
            new Destination("BKK", "방콕"),
            new Destination("DAD", "다낭"),
            new Destination("SGN", "호치민"),
            new Destination("PQC", "푸꾸옥"),
            new Destination("GUM", "괌"));

    private FlightSearchMockFixture() {}

    static FlightDealSearchResponse page(String cursor, int size) {
        int startIndex = startIndexOf(cursor);
        int endIndex = Math.min(startIndex + size, TOTAL_COUNT);

        List<FlightDealResponse> items = generateRange(startIndex, endIndex);
        boolean hasNext = endIndex < TOTAL_COUNT;
        String nextCursor = hasNext ? items.get(items.size() - 1).id() : null;

        return new FlightDealSearchResponse(items, new FlightDealSearchMeta(nextCursor, hasNext, TOTAL_COUNT));
    }

    private static int startIndexOf(String cursor) {
        if (cursor == null) {
            return 0;
        }

        int cursorIndex = indexFromId(cursor);
        return cursorIndex >= 0 ? cursorIndex + 1 : 0;
    }

    private static List<FlightDealResponse> generateRange(int startIndex, int endIndex) {
        return java.util.stream.IntStream.range(startIndex, endIndex)
                .mapToObj(FlightSearchMockFixture::dealAt)
                .toList();
    }

    private static FlightDealResponse dealAt(int index) {
        Destination destination = DESTINATIONS.get(index % DESTINATIONS.size());
        int amount = BASE_PRICE + (index % PRICE_CYCLE) * PRICE_STEP;
        OffsetDateTime departureAt = BASE_DEPARTURE.plusDays(index);

        return new FlightDealResponse(
                idOf(index),
                new FlightDealResponse.Destination(destination.iata(), destination.city()),
                DEPARTURE_FORMAT.format(departureAt),
                new FlightDealResponse.Price(amount, "KRW"));
    }

    private static String idOf(int index) {
        return "deal_%05d".formatted(index);
    }

    private static int indexFromId(String id) {
        if (!id.startsWith("deal_")) {
            return -1;
        }
        try {
            return Integer.parseInt(id.substring("deal_".length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private record Destination(String iata, String city) {}
}

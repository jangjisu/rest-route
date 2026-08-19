package com.restroute.flight.controller.response;

import java.util.List;

/**
 * Travelpayouts grouped_prices가 실제로 줄 수 있는 필드만으로 구성한 항공권 한 건.
 * 왕복 전체에 항공사가 하나뿐이라 가는편 기준으로만 알 수 있고,
 * 경유는 횟수만 있고 경유 공항/대기시간·수하물·운임 규정은 이 API로는 알 수 없다(그래서 없음).
 */
public record FlightDealResponse(
        String id,
        Destination destination,
        Leg departure,
        Leg arrival,
        int nights,
        List<HolidayDay> holidays,
        Airline airline,
        Price price,
        boolean isLowestInRange,
        String gateName,
        String bookingLink,
        Integer seatsLeft) {

    public record Destination(String code, String name) {}

    public record Leg(String departAt, String arriveAt, int duration, int transferCount) {}

    /**
     * 출발일~귀국일 사이에 걸리는 비근무일(공휴일·주말) 한 건. 공휴일이면 {@code name}이 채워지고,
     * 순수 주말(공휴일 테이블에 없는 토/일)이면 {@code name}이 {@code null}이다. 연차 사용일수
     * 같은 파생값은 프론트에서 이 목록으로 직접 계산한다 — 여기서는 원 데이터만 내려준다.
     */
    public record HolidayDay(String date, String name) {}

    public record Airline(String code, String name, boolean isLowCost) {}

    public record Price(int amount, String currency) {}
}

package com.restroute.flight.controller.response;

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
        Airline airline,
        Price price,
        String gateName,
        String bookingLink) {

    public record Destination(String code, String name) {}

    public record Leg(String departureFrom, String departTo, int duration, int transferCount) {}

    public record Airline(String code, String name) {}

    public record Price(int amount, String currency) {}
}

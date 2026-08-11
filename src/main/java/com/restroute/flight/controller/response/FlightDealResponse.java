package com.restroute.flight.controller.response;

/**
 * Travelpayouts grouped_prices가 실제로 줄 수 있는 필드만으로 구성한 항공권 한 건.
 * 왕복 전체에 항공사/편명이 하나뿐이라 가는편/오는편을 따로 못 나누고,
 * 경유는 횟수만 있고 경유 공항/대기시간·수하물·운임 규정은 이 API로는 알 수 없다(그래서 없음).
 */
public record FlightDealResponse(
        String id,
        Destination destination,
        String departureAt,
        String returnAt,
        int nights,
        int transferCount,
        int returnTransferCount,
        String airline,
        String flightNumber,
        int durationMinutes,
        int returnDurationMinutes,
        Price price,
        String gateName,
        String bookingLink) {

    public record Destination(String iata, String city) {}

    public record Price(int amount, String currency) {}
}

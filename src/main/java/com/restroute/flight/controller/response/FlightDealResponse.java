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
        Holiday holiday,
        Airline airline,
        Price price,
        boolean isLowestInRange,
        String gateName,
        String bookingLink,
        Integer seatsLeft) {

    public record Destination(String code, String name) {}

    public record Leg(String departAt, String arriveAt, int duration, int transferCount) {}

    /**
     * 연휴 배지 계산 결과. 지금은 실제 공휴일 달력 연동이 없어 mock에서는 항상 0/빈 값으로
     * 채운다 — 실제 계산 로직(공휴일 판정, 연차 일수 산정)은 별도 작업이 필요하다.
     */
    public record Holiday(int count, List<String> names, int annualLeaveDays) {}

    public record Airline(String code, String name, boolean isLowCost) {}

    public record Price(int amount, String currency) {}
}

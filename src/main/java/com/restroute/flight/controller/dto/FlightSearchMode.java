package com.restroute.flight.controller.dto;

/**
 * 날짜를 고르는 방식. 지정날짜(FIXED)와 범위(RANGE)는 nights 허용 여부와 결과 의미가 완전히
 * 다르므로("같은 날짜 두 개"도 방식에 따라 전혀 다른 검색이 된다), searchMode는 항상 명시적으로
 * 받고 절대 추측하지 않는다.
 */
public enum FlightSearchMode {
    FIXED,
    RANGE;

    static FlightSearchMode fromWireValue(String raw) {
        return switch (raw) {
            case "fixed" -> FIXED;
            case "range" -> RANGE;
            default -> null;
        };
    }

    public static boolean isRange(FlightSearchMode mode) {
        return mode == RANGE;
    }
}

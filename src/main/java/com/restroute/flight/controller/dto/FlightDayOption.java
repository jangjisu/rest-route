package com.restroute.flight.controller.dto;

import lombok.Getter;

/**
 * 날짜 조건 필터. 복수 선택 시 AND 조건으로 함께 적용된다.
 */
@Getter
public enum FlightDayOption {
    INCLUDE_WEEKEND("주말 2일 포함"),
    EXCLUDE_WEEKEND_INCLUDE_HOLIDAY("주말 제외 공휴일 포함");

    private final String description;

    FlightDayOption(String description) {
        this.description = description;
    }
}

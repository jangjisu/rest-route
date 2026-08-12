package com.restroute.flight.controller;

import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 컨트롤러가 쿼리 파라미터로부터 그대로 바인딩받는 검색 조건 DTO. 생성자가
 * {@link FlightSearchRequestValidator}를 호출해서 바로 검증하고, 실패하면 그 자리에서
 * {@link InvalidFlightSearchException}을 던진다 — 이 객체가 존재한다는 것 자체가 이미
 * 검증을 통과했다는 뜻이다. required 여부와 실제 값 체크는 전부 validator 쪽 책임이고, 이
 * DTO는 원본 값 보관과 파싱된 값 접근자만 담당한다.
 *
 * <p>{@code equals}/{@code hashCode}는 cursor/size를 제외하고 비교하도록 직접 오버라이드했다
 * — 둘 다 페이지마다 값이 달라지는 게 정상이라, {@link FlightDealSessionStore}가 "검색 조건이
 * 같은 요청인지" 판단할 때 이 두 필드 때문에 매번 다른 요청으로 오인하면 안 되기 때문이다.
 */
record FlightSearchRequestDto(
        String origin,
        String destination,
        String dateFrom,
        String dateTo,
        List<String> nights,
        List<String> filter,
        List<String> dayOption,
        String includeTransfer,
        String cursor,
        String size) {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;

    FlightSearchRequestDto {
        FlightSearchRequestValidator.validate(
                origin, destination, dateFrom, dateTo, nights, filter, dayOption, includeTransfer);
    }

    LocalDate parsedDateFrom() {
        return LocalDate.parse(dateFrom);
    }

    LocalDate parsedDateTo() {
        return LocalDate.parse(dateTo);
    }

    List<Integer> parsedNights() {
        return nights.stream().map(Integer::parseInt).toList();
    }

    boolean isIncludeTransfer() {
        return "true".equals(includeTransfer);
    }

    /** cursor가 없으면 첫 요청(새로 조회해야 함), 있으면 이전 페이지에 이어가는 요청이다. */
    boolean isFirstRequest() {
        return cursor == null;
    }

    /** size가 없거나 숫자가 아니면 기본값(20)으로, 범위를 벗어나면 [1,50]으로 조용히 잘라낸다. */
    int boundedSize() {
        return Math.min(Math.max(parseSizeOrDefault(size), MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }

    private static int parseSizeOrDefault(String raw) {
        if (raw == null) {
            return DEFAULT_PAGE_SIZE;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return DEFAULT_PAGE_SIZE;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlightSearchRequestDto other)) {
            return false;
        }
        return Objects.equals(origin, other.origin)
                && Objects.equals(destination, other.destination)
                && Objects.equals(dateFrom, other.dateFrom)
                && Objects.equals(dateTo, other.dateTo)
                && Objects.equals(nights, other.nights)
                && Objects.equals(filter, other.filter)
                && Objects.equals(dayOption, other.dayOption)
                && Objects.equals(includeTransfer, other.includeTransfer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, destination, dateFrom, dateTo, nights, filter, dayOption, includeTransfer);
    }
}

package com.restroute.flight.controller;

import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.util.CollectionUtils;

/**
 * 항공권 검색 요청 파라미터 검증. 컨트롤러 메서드를 짧게 유지하기 위해 분리했다.
 */
final class FlightSearchRequestValidator {

    private static final int MAX_DATE_RANGE_MONTHS = 3;

    private FlightSearchRequestValidator() {}

    static void validate(LocalDate dateFrom, LocalDate dateTo, List<Integer> nights, List<String> regions) {
        validateDateRange(dateFrom, dateTo);
        validateNights(nights);
        validateRegions(regions);
    }

    private static void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateTo.isBefore(dateFrom)) {
            throw InvalidFlightSearchException.forReversedDateRange(dateFrom, dateTo);
        }
        if (dateTo.isAfter(dateFrom.plusMonths(MAX_DATE_RANGE_MONTHS))) {
            throw InvalidFlightSearchException.forDateRangeTooWide(dateFrom, dateTo, MAX_DATE_RANGE_MONTHS);
        }
    }

    private static void validateNights(List<Integer> nights) {
        if (CollectionUtils.isEmpty(nights)) {
            throw InvalidFlightSearchException.forEmptyNights();
        }
    }

    private static void validateRegions(List<String> regions) {
        if (CollectionUtils.isEmpty(regions)) {
            return;
        }

        for (String region : regions) {
            if (!isKnownRegion(region)) {
                throw InvalidFlightSearchException.forUnknownRegion(region);
            }
        }
    }

    private static boolean isKnownRegion(String region) {
        try {
            FlightRegion.valueOf(region);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

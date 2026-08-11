package com.restroute.flight.controller;

import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import com.restroute.flight.controller.response.FlightApiError;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 항공권 검색 요청을 grouped_prices 호출에 필요한 형태로 파싱/검증한다.
 * 개별 필드에서 걸리는 대로 바로 던지지 않고 전부 모아서 한 번에 알려준다.
 */
final class FlightSearchRequestValidator {

    private static final int MAX_DATE_RANGE_MONTHS = 3;
    private static final Pattern IATA_CODE = Pattern.compile("^[A-Za-z]{3}$");

    private FlightSearchRequestValidator() {}

    static ValidatedRequest validate(
            String origin,
            String destination,
            String dateFromRaw,
            String dateToRaw,
            List<String> nightsRaw,
            List<String> regions) {
        List<FlightApiError.Detail> details = new ArrayList<>();

        validateOrigin(origin, details);
        validateDestination(destination, details);
        LocalDate dateFrom = validateDateFrom(dateFromRaw, details);
        LocalDate dateTo = validateRequiredDate("dateTo", dateToRaw, details);
        validateDateRange(dateFrom, dateTo, details);
        List<Integer> nights = validateNights(nightsRaw, details);
        validateRegions(regions, details);

        if (!details.isEmpty()) {
            throw new InvalidFlightSearchException(details);
        }

        return new ValidatedRequest(origin, destination, dateFrom, dateTo, nights, regions);
    }

    private static void validateOrigin(String origin, List<FlightApiError.Detail> details) {
        if (!StringUtils.hasText(origin)) {
            details.add(new FlightApiError.Detail("origin", "REQUIRED"));
            return;
        }
        if (!IATA_CODE.matcher(origin).matches()) {
            details.add(new FlightApiError.Detail("origin", "INVALID_IATA_CODE"));
        }
    }

    private static void validateDestination(String destination, List<FlightApiError.Detail> details) {
        if (StringUtils.hasText(destination) && !IATA_CODE.matcher(destination).matches()) {
            details.add(new FlightApiError.Detail("destination", "INVALID_IATA_CODE"));
        }
    }

    private static LocalDate validateDateFrom(String raw, List<FlightApiError.Detail> details) {
        LocalDate parsed = validateRequiredDate("dateFrom", raw, details);
        if (parsed != null && parsed.isBefore(LocalDate.now())) {
            details.add(new FlightApiError.Detail("dateFrom", "PAST_DATE_NOT_ALLOWED"));
        }
        return parsed;
    }

    private static LocalDate validateRequiredDate(String field, String raw, List<FlightApiError.Detail> details) {
        if (!StringUtils.hasText(raw)) {
            details.add(new FlightApiError.Detail(field, "REQUIRED"));
            return null;
        }
        return parseDate(field, raw, details);
    }

    private static LocalDate parseDate(String field, String raw, List<FlightApiError.Detail> details) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            details.add(new FlightApiError.Detail(field, "INVALID_DATE_FORMAT"));
            return null;
        }
    }

    private static void validateDateRange(LocalDate dateFrom, LocalDate dateTo, List<FlightApiError.Detail> details) {
        if (dateFrom == null || dateTo == null) {
            return;
        }
        if (dateTo.isBefore(dateFrom)) {
            details.add(new FlightApiError.Detail("dateTo", "BEFORE_DATE_FROM"));
            return;
        }
        if (dateTo.isAfter(dateFrom.plusMonths(MAX_DATE_RANGE_MONTHS))) {
            details.add(new FlightApiError.Detail("dateTo", "DATE_RANGE_TOO_WIDE"));
        }
    }

    private static List<Integer> validateNights(List<String> nightsRaw, List<FlightApiError.Detail> details) {
        if (CollectionUtils.isEmpty(nightsRaw)) {
            details.add(new FlightApiError.Detail("nights", "REQUIRED"));
            return List.of();
        }

        List<Integer> nights = new ArrayList<>();
        for (String raw : nightsRaw) {
            try {
                nights.add(Integer.parseInt(raw));
            } catch (NumberFormatException e) {
                details.add(new FlightApiError.Detail("nights", "INVALID_NIGHTS_VALUE"));
            }
        }
        return nights;
    }

    private static void validateRegions(List<String> regions, List<FlightApiError.Detail> details) {
        if (CollectionUtils.isEmpty(regions)) {
            return;
        }
        for (String region : regions) {
            if (!isKnownRegion(region)) {
                details.add(new FlightApiError.Detail("regions", "INVALID_REGION"));
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

    record ValidatedRequest(
            String origin,
            String destination,
            LocalDate dateFrom,
            LocalDate dateTo,
            List<Integer> nights,
            List<String> regions) {}
}

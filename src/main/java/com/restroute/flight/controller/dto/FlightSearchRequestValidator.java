package com.restroute.flight.controller.dto;

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
 * 항공권 검색 조건의 required 여부와 실제 값 형식을 검증한다. {@link FlightSearchRequestDto}의
 * 생성자가 이 클래스를 호출해서 유효성을 확인한다 — 개별 필드에서 걸리는 대로 바로 던지지 않고
 * 전부 모아서 한 번에 알려준다.
 */
final class FlightSearchRequestValidator {

    private static final int MAX_DATE_RANGE_MONTHS = 3;
    private static final int MIN_NIGHTS = 1;
    private static final int MAX_NIGHTS = 90;
    private static final Pattern IATA_CODE = Pattern.compile("^[A-Za-z]{3}$");

    private FlightSearchRequestValidator() {}

    static void validate(
            String origin,
            String destination,
            String dateFrom,
            String dateTo,
            List<String> nights,
            List<String> filter,
            List<String> dayOption,
            String includeTransfer) {
        List<FlightApiError.Detail> details = new ArrayList<>();

        validateOrigin(origin, details);
        validateDestination(destination, details);
        LocalDate parsedDateFrom = validateDateFrom(dateFrom, details);
        LocalDate parsedDateTo = validateRequiredDate("dateTo", dateTo, details);
        validateDateRange(parsedDateFrom, parsedDateTo, details);
        validateNights(nights, details);
        validateFilter(filter, details);
        validateDayOption(dayOption, details);
        validateIncludeTransfer(includeTransfer, details);

        if (!details.isEmpty()) {
            throw new InvalidFlightSearchException(details);
        }
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

    private static void validateNights(List<String> nightsRaw, List<FlightApiError.Detail> details) {
        if (CollectionUtils.isEmpty(nightsRaw)) {
            details.add(new FlightApiError.Detail("nights", "REQUIRED"));
            return;
        }
        for (String raw : nightsRaw) {
            Integer value = parseNights(raw);
            if (value == null || value < MIN_NIGHTS || value > MAX_NIGHTS) {
                details.add(new FlightApiError.Detail("nights", "INVALID_NIGHTS_VALUE"));
            }
        }
    }

    private static Integer parseNights(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void validateFilter(List<String> filter, List<FlightApiError.Detail> details) {
        if (CollectionUtils.isEmpty(filter)) {
            return;
        }
        for (String region : filter) {
            if (!isKnownRegion(region)) {
                details.add(new FlightApiError.Detail("filter", "INVALID_FILTER"));
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

    private static void validateDayOption(List<String> dayOption, List<FlightApiError.Detail> details) {
        if (CollectionUtils.isEmpty(dayOption)) {
            return;
        }
        for (String option : dayOption) {
            if (!isKnownDayOption(option)) {
                details.add(new FlightApiError.Detail("dayOption", "INVALID_DAY_OPTION"));
            }
        }
    }

    private static boolean isKnownDayOption(String option) {
        try {
            FlightDayOption.valueOf(option);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void validateIncludeTransfer(String raw, List<FlightApiError.Detail> details) {
        if (!StringUtils.hasText(raw) || "true".equals(raw) || "false".equals(raw)) {
            return;
        }
        details.add(new FlightApiError.Detail("includeTransfer", "INVALID_INCLUDE_TRANSFER"));
    }
}

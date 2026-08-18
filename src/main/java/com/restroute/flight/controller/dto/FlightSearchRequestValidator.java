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

    private static final int MIN_NIGHTS = 1;
    private static final int MAX_NIGHTS = 90;
    private static final String SUPPORTED_CURRENCY = "krw";
    private static final Pattern IATA_CODE = Pattern.compile("^[A-Za-z]{3}$");

    private FlightSearchRequestValidator() {}

    static void validate(
            String origin,
            String searchMode,
            String dateFrom,
            String dateTo,
            String destination,
            List<String> nights,
            List<String> sector,
            String includeWeekend,
            String includeHoliday,
            String includeTransfer,
            String adults,
            String children,
            String infants,
            String sort,
            String currency) {
        List<FlightApiError.Detail> details = new ArrayList<>();

        validateOrigin(origin, details);
        FlightSearchMode parsedMode = validateSearchMode(searchMode, details);
        LocalDate parsedDateFrom = validateDateFrom(dateFrom, details);
        LocalDate parsedDateTo = validateRequiredDate("dateTo", dateTo, details);
        validateDateRange(parsedDateFrom, parsedDateTo, details);
        validateDestination(destination, details);
        validateNights(nights, parsedMode, details);
        validateSector(sector, destination, details);
        validateIncludeWeekend(includeWeekend, details);
        validateIncludeHoliday(includeHoliday, details);
        validateIncludeTransfer(includeTransfer, details);
        validateAdults(adults, details);
        validateChildren(children, details);
        validateInfants(infants, details);
        validateSort(sort, details);
        validateCurrency(currency, details);

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

    private static FlightSearchMode validateSearchMode(String raw, List<FlightApiError.Detail> details) {
        if (!StringUtils.hasText(raw)) {
            details.add(new FlightApiError.Detail("searchMode", "REQUIRED"));
            return null;
        }
        FlightSearchMode mode = FlightSearchMode.fromWireValue(raw);
        if (mode == null) {
            details.add(new FlightApiError.Detail("searchMode", "INVALID_SEARCH_MODE"));
        }
        return mode;
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

    /** 범위 검색은 달·연도를 넘나들어도 되므로 상한을 두지 않는다 — dateTo가 dateFrom보다 이전인지만 본다. */
    private static void validateDateRange(LocalDate dateFrom, LocalDate dateTo, List<FlightApiError.Detail> details) {
        if (dateFrom == null || dateTo == null) {
            return;
        }
        if (dateTo.isBefore(dateFrom)) {
            details.add(new FlightApiError.Detail("dateTo", "BEFORE_DATE_FROM"));
        }
    }

    private static void validateNights(
            List<String> nightsRaw, FlightSearchMode mode, List<FlightApiError.Detail> details) {
        if (CollectionUtils.isEmpty(nightsRaw)) {
            return;
        }
        if (mode == FlightSearchMode.FIXED) {
            details.add(new FlightApiError.Detail("nights", "NIGHTS_NOT_ALLOWED_IN_FIXED_MODE"));
            return;
        }
        for (String raw : nightsRaw) {
            Integer value = parseIntOrNull(raw);
            if (value == null || value < MIN_NIGHTS || value > MAX_NIGHTS) {
                details.add(new FlightApiError.Detail("nights", "INVALID_NIGHTS_VALUE"));
            }
        }
    }

    private static void validateSector(List<String> sector, String destination, List<FlightApiError.Detail> details) {
        if (CollectionUtils.isEmpty(sector)) {
            return;
        }
        if (StringUtils.hasText(destination)) {
            details.add(new FlightApiError.Detail("sector", "SECTOR_DESTINATION_CONFLICT"));
            return;
        }
        for (String region : sector) {
            if (!isKnownRegion(region)) {
                details.add(new FlightApiError.Detail("sector", "INVALID_SECTOR"));
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

    private static void validateIncludeWeekend(String raw, List<FlightApiError.Detail> details) {
        if (!StringUtils.hasText(raw) || "true".equals(raw) || "false".equals(raw)) {
            return;
        }
        details.add(new FlightApiError.Detail("includeWeekend", "INVALID_INCLUDE_WEEKEND"));
    }

    private static void validateIncludeHoliday(String raw, List<FlightApiError.Detail> details) {
        if (!StringUtils.hasText(raw) || "true".equals(raw) || "false".equals(raw)) {
            return;
        }
        details.add(new FlightApiError.Detail("includeHoliday", "INVALID_INCLUDE_HOLIDAY"));
    }

    private static void validateIncludeTransfer(String raw, List<FlightApiError.Detail> details) {
        if (!StringUtils.hasText(raw) || "true".equals(raw) || "false".equals(raw)) {
            return;
        }
        details.add(new FlightApiError.Detail("includeTransfer", "INVALID_INCLUDE_TRANSFER"));
    }

    private static void validateAdults(String raw, List<FlightApiError.Detail> details) {
        validateNonNegativeInt("adults", raw, "INVALID_ADULTS", details);
    }

    private static void validateChildren(String raw, List<FlightApiError.Detail> details) {
        validateNonNegativeInt("children", raw, "INVALID_CHILDREN", details);
    }

    private static void validateInfants(String raw, List<FlightApiError.Detail> details) {
        validateNonNegativeInt("infants", raw, "INVALID_INFANTS", details);
    }

    private static void validateNonNegativeInt(
            String field, String raw, String errorCode, List<FlightApiError.Detail> details) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        Integer value = parseIntOrNull(raw);
        if (value == null || value < 0) {
            details.add(new FlightApiError.Detail(field, errorCode));
        }
    }

    private static Integer parseIntOrNull(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void validateSort(String sort, List<FlightApiError.Detail> details) {
        if (!StringUtils.hasText(sort) || isKnownSort(sort)) {
            return;
        }
        details.add(new FlightApiError.Detail("sort", "INVALID_SORT"));
    }

    private static boolean isKnownSort(String sort) {
        try {
            FlightDealSort.valueOf(sort);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** 지금은 krw만 받는다 — 대소문자는 구분하지 않고, 그 외 값은 전부 오류다. */
    private static void validateCurrency(String currency, List<FlightApiError.Detail> details) {
        if (!StringUtils.hasText(currency) || SUPPORTED_CURRENCY.equalsIgnoreCase(currency)) {
            return;
        }
        details.add(new FlightApiError.Detail("currency", "INVALID_CURRENCY"));
    }
}

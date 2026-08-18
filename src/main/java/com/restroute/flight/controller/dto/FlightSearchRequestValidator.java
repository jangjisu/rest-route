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
 *
 * <p>검증 1회당 인스턴스를 하나 만들어 쓴다 — 원본 필드와 누적 중인 {@link #details}를 인스턴스
 * 필드로 들고 있어서, validateXxx() 메소드마다 details를 인자로 넘길 필요가 없다. 바깥에서 부르는
 * 진입점({@link #validate})만 static으로 남겨서 호출부({@link FlightSearchRequestDto})는 그대로다.
 */
final class FlightSearchRequestValidator {

    private static final int MIN_NIGHTS = 1;
    private static final int MAX_NIGHTS = 90;
    private static final String SUPPORTED_CURRENCY = "krw";
    private static final Pattern IATA_CODE = Pattern.compile("^[A-Za-z]{3}$");

    private final List<FlightApiError.Detail> details = new ArrayList<>();

    private final String origin;
    private final String searchMode;
    private final String dateFrom;
    private final String dateTo;
    private final String destination;
    private final List<String> nights;
    private final List<String> sector;
    private final String includeWeekend;
    private final String includeHoliday;
    private final String includeTransfer;
    private final String adults;
    private final String children;
    private final String infants;
    private final String sort;
    private final String currency;

    private FlightSearchRequestValidator(
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
        this.origin = origin;
        this.searchMode = searchMode;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.destination = destination;
        this.nights = nights;
        this.sector = sector;
        this.includeWeekend = includeWeekend;
        this.includeHoliday = includeHoliday;
        this.includeTransfer = includeTransfer;
        this.adults = adults;
        this.children = children;
        this.infants = infants;
        this.sort = sort;
        this.currency = currency;
    }

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
        new FlightSearchRequestValidator(
                        origin,
                        searchMode,
                        dateFrom,
                        dateTo,
                        destination,
                        nights,
                        sector,
                        includeWeekend,
                        includeHoliday,
                        includeTransfer,
                        adults,
                        children,
                        infants,
                        sort,
                        currency)
                .run();
    }

    private void run() {
        validateOrigin();
        FlightSearchMode parsedMode = validateSearchMode();
        LocalDate parsedDateFrom = validateDateFrom();
        LocalDate parsedDateTo = validateRequiredDate("dateTo", dateTo);
        validateDateRange(parsedDateFrom, parsedDateTo);
        validateDestination();
        validateNights(parsedMode);
        validateSector();
        validateIncludeWeekend();
        validateIncludeHoliday();
        validateIncludeTransfer();
        validateAdults();
        validateChildren();
        validateInfants();
        validateSort();
        validateCurrency();

        if (!details.isEmpty()) {
            throw new InvalidFlightSearchException(details);
        }
    }

    private void validateOrigin() {
        if (!StringUtils.hasText(origin)) {
            details.add(new FlightApiError.Detail("origin", "REQUIRED"));
            return;
        }
        if (!IATA_CODE.matcher(origin).matches()) {
            details.add(new FlightApiError.Detail("origin", "INVALID_IATA_CODE"));
        }
    }

    private FlightSearchMode validateSearchMode() {
        if (!StringUtils.hasText(searchMode)) {
            details.add(new FlightApiError.Detail("searchMode", "REQUIRED"));
            return null;
        }
        FlightSearchMode mode = FlightSearchMode.fromWireValue(searchMode);
        if (mode == null) {
            details.add(new FlightApiError.Detail("searchMode", "INVALID_SEARCH_MODE"));
        }
        return mode;
    }

    private void validateDestination() {
        if (StringUtils.hasText(destination) && !IATA_CODE.matcher(destination).matches()) {
            details.add(new FlightApiError.Detail("destination", "INVALID_IATA_CODE"));
        }
    }

    private LocalDate validateDateFrom() {
        LocalDate parsed = validateRequiredDate("dateFrom", dateFrom);
        if (parsed != null && parsed.isBefore(LocalDate.now())) {
            details.add(new FlightApiError.Detail("dateFrom", "PAST_DATE_NOT_ALLOWED"));
        }
        return parsed;
    }

    private LocalDate validateRequiredDate(String field, String raw) {
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
    private void validateDateRange(LocalDate parsedDateFrom, LocalDate parsedDateTo) {
        if (parsedDateFrom == null || parsedDateTo == null) {
            return;
        }
        if (parsedDateTo.isBefore(parsedDateFrom)) {
            details.add(new FlightApiError.Detail("dateTo", "BEFORE_DATE_FROM"));
        }
    }

    private void validateNights(FlightSearchMode mode) {
        if (CollectionUtils.isEmpty(nights)) {
            return;
        }
        if (mode == FlightSearchMode.FIXED) {
            details.add(new FlightApiError.Detail("nights", "NIGHTS_NOT_ALLOWED_IN_FIXED_MODE"));
            return;
        }
        for (String raw : nights) {
            Integer value = parseIntOrNull(raw);
            if (value == null || value < MIN_NIGHTS || value > MAX_NIGHTS) {
                details.add(new FlightApiError.Detail("nights", "INVALID_NIGHTS_VALUE"));
            }
        }
    }

    private void validateSector() {
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

    private void validateIncludeWeekend() {
        if (!StringUtils.hasText(includeWeekend) || "true".equals(includeWeekend) || "false".equals(includeWeekend)) {
            return;
        }
        details.add(new FlightApiError.Detail("includeWeekend", "INVALID_INCLUDE_WEEKEND"));
    }

    private void validateIncludeHoliday() {
        if (!StringUtils.hasText(includeHoliday) || "true".equals(includeHoliday) || "false".equals(includeHoliday)) {
            return;
        }
        details.add(new FlightApiError.Detail("includeHoliday", "INVALID_INCLUDE_HOLIDAY"));
    }

    private void validateIncludeTransfer() {
        if (!StringUtils.hasText(includeTransfer)
                || "true".equals(includeTransfer)
                || "false".equals(includeTransfer)) {
            return;
        }
        details.add(new FlightApiError.Detail("includeTransfer", "INVALID_INCLUDE_TRANSFER"));
    }

    private void validateAdults() {
        validateNonNegativeInt("adults", adults, "INVALID_ADULTS");
    }

    private void validateChildren() {
        validateNonNegativeInt("children", children, "INVALID_CHILDREN");
    }

    private void validateInfants() {
        validateNonNegativeInt("infants", infants, "INVALID_INFANTS");
    }

    private void validateNonNegativeInt(String field, String raw, String errorCode) {
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

    private void validateSort() {
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
    private void validateCurrency() {
        if (!StringUtils.hasText(currency) || SUPPORTED_CURRENCY.equalsIgnoreCase(currency)) {
            return;
        }
        details.add(new FlightApiError.Detail("currency", "INVALID_CURRENCY"));
    }
}

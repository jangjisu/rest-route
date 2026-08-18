package com.restroute.flight.controller.dto;

import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 컨트롤러가 쿼리 파라미터로부터 그대로 바인딩받는 검색 조건 DTO. 생성자가
 * {@link FlightSearchRequestValidator}를 호출해서 바로 검증하고, 실패하면 그 자리에서
 * {@link InvalidFlightSearchException}을 던진다 — 이 객체가 존재한다는 것 자체가 이미
 * 검증을 통과했다는 뜻이다. required 여부와 실제 값 체크는 전부 validator 쪽 책임이고, 이
 * DTO는 원본 값 보관과 파싱된 값 접근자만 담당한다.
 *
 * <p>{@code equals}/{@code hashCode}는 cursor/limit을 제외하고 비교하도록 직접 오버라이드했다
 * — 둘 다 페이지마다 값이 달라지는 게 정상이라, {@code FlightDealSessionStore}가 "검색 조건이
 * 같은 요청인지" 판단할 때 이 두 필드 때문에 매번 다른 요청으로 오인하면 안 되기 때문이다.
 */
public record FlightSearchRequestDto(
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
        String cursor,
        String limit,
        String locale,
        String currency) {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MIN_NIGHTS = 1;
    private static final int DEFAULT_ADULTS = 1;
    private static final int DEFAULT_CHILDREN = 0;
    private static final int DEFAULT_INFANTS = 0;
    private static final String DEFAULT_LOCALE = "ko";
    private static final String DEFAULT_CURRENCY = "krw";

    public FlightSearchRequestDto {
        FlightSearchRequestValidator.validate(
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
                currency);
    }

    public FlightSearchMode parsedSearchMode() {
        return FlightSearchMode.fromWireValue(searchMode);
    }

    public LocalDate parsedDateFrom() {
        return LocalDate.parse(dateFrom);
    }

    public LocalDate parsedDateTo() {
        return LocalDate.parse(dateTo);
    }

    /**
     * nights가 없으면 dateFrom~dateTo 기간 전체를 1박부터 최대박까지 조회 대상으로 본다
     * (예: 기간이 8일이면 1~8박). nights는 범위(RANGE) 검색 전용이라 지정날짜(FIXED)에서는
     * validator가 이미 걸러낸다.
     */
    public List<Integer> parsedNights() {
        if (CollectionUtils.isEmpty(nights)) {
            long maxNights = Math.max(MIN_NIGHTS, ChronoUnit.DAYS.between(parsedDateFrom(), parsedDateTo()));
            return IntStream.rangeClosed(MIN_NIGHTS, (int) maxNights).boxed().toList();
        }
        return nights.stream().map(Integer::parseInt).toList();
    }

    public boolean isIncludeWeekend() {
        return "true".equals(includeWeekend);
    }

    public boolean isIncludeHoliday() {
        return "true".equals(includeHoliday);
    }

    /** includeTransfer는 생략하면 켜짐(경유 포함)이 기본값이다 — "false"일 때만 직항만 본다. */
    public boolean isIncludeTransfer() {
        return !"false".equals(includeTransfer);
    }

    public int parsedAdults() {
        return StringUtils.hasText(adults) ? Integer.parseInt(adults) : DEFAULT_ADULTS;
    }

    public int parsedChildren() {
        return StringUtils.hasText(children) ? Integer.parseInt(children) : DEFAULT_CHILDREN;
    }

    public int parsedInfants() {
        return StringUtils.hasText(infants) ? Integer.parseInt(infants) : DEFAULT_INFANTS;
    }

    /** sort가 없으면 PRICE(최저가순)가 기본값이다. */
    public FlightDealSort parsedSort() {
        if (!StringUtils.hasText(sort)) {
            return FlightDealSort.PRICE;
        }
        return FlightDealSort.valueOf(sort);
    }

    /** cursor가 없으면 첫 요청(새로 조회해야 함), 있으면 이전 페이지에 이어가는 요청이다. */
    public boolean isFirstRequest() {
        return cursor == null;
    }

    /** limit이 없거나 숫자가 아니면 기본값(20)으로, 범위를 벗어나면 [1,50]으로 조용히 잘라낸다. */
    public int boundedLimit() {
        return Math.min(Math.max(parseLimitOrDefault(limit), MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }

    private static int parseLimitOrDefault(String raw) {
        if (raw == null) {
            return DEFAULT_PAGE_SIZE;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return DEFAULT_PAGE_SIZE;
        }
    }

    public String parsedLocale() {
        return StringUtils.hasText(locale) ? locale : DEFAULT_LOCALE;
    }

    /** currency가 없으면 krw가 기본값이다 — validator가 이미 krw 외 값을 걸러내므로 대소문자만 통일한다. */
    public String parsedCurrency() {
        return StringUtils.hasText(currency) ? currency.toLowerCase(Locale.ROOT) : DEFAULT_CURRENCY;
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
                && Objects.equals(searchMode, other.searchMode)
                && Objects.equals(dateFrom, other.dateFrom)
                && Objects.equals(dateTo, other.dateTo)
                && Objects.equals(destination, other.destination)
                && Objects.equals(nights, other.nights)
                && Objects.equals(sector, other.sector)
                && Objects.equals(includeWeekend, other.includeWeekend)
                && Objects.equals(includeHoliday, other.includeHoliday)
                && Objects.equals(includeTransfer, other.includeTransfer)
                && Objects.equals(adults, other.adults)
                && Objects.equals(children, other.children)
                && Objects.equals(infants, other.infants)
                && Objects.equals(sort, other.sort)
                && Objects.equals(locale, other.locale)
                && Objects.equals(currency, other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
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
                locale,
                currency);
    }
}

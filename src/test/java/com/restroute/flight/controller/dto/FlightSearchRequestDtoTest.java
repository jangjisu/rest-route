package com.restroute.flight.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightSearchRequestDtoTest {

    private static String futureDate(int daysFromToday) {
        return LocalDate.now().plusDays(daysFromToday).toString();
    }

    @Test
    @DisplayName("유효한 값이면 생성되고, 파싱된 접근자도 올바른 값을 반환한다")
    void constructor_succeeds_andExposesParsedAccessors() {
        FlightSearchRequestDto result = new FlightSearchRequestDto(
                "ICN",
                "range",
                futureDate(10),
                futureDate(41),
                "OSA",
                List.of("3", "4"),
                null,
                "true",
                "true",
                "true",
                "2",
                "1",
                "1",
                "DATE",
                null,
                "20",
                "en",
                "KRW");

        assertThat(result.origin()).isEqualTo("ICN");
        assertThat(result.parsedSearchMode()).isEqualTo(FlightSearchMode.RANGE);
        assertThat(result.destination()).isEqualTo("OSA");
        assertThat(result.parsedDateFrom()).isEqualTo(LocalDate.now().plusDays(10));
        assertThat(result.parsedDateTo()).isEqualTo(LocalDate.now().plusDays(41));
        assertThat(result.parsedNights()).containsExactly(3, 4);
        assertThat(result.isIncludeWeekend()).isTrue();
        assertThat(result.isIncludeHoliday()).isTrue();
        assertThat(result.isIncludeTransfer()).isTrue();
        assertThat(result.parsedAdults()).isEqualTo(2);
        assertThat(result.parsedChildren()).isEqualTo(1);
        assertThat(result.parsedInfants()).isEqualTo(1);
        assertThat(result.parsedSort()).isEqualTo(FlightDealSort.DATE);
        assertThat(result.parsedLocale()).isEqualTo("en");
        assertThat(result.parsedCurrency()).isEqualTo("krw");
    }

    @Test
    @DisplayName("sector에 목적지 그룹만 지정하면(destination 없이) 정상 생성된다")
    void constructor_succeeds_whenSectorGivenWithoutDestination() {
        FlightSearchRequestDto result = fixedRequest(builder -> builder.sector(List.of("JAPAN")));

        assertThat(result.sector()).containsExactly("JAPAN");
    }

    @Test
    @DisplayName("nights가 없으면(null) parsedNights()는 dateFrom~dateTo 기간만큼 1박부터 전체를 반환한다")
    void parsedNights_defaultsToDateRange_whenNightsIsNull() {
        FlightSearchRequestDto result = rangeRequest(futureDate(10), futureDate(18), null);

        assertThat(result.parsedNights()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    @DisplayName("nights가 빈 리스트여도 parsedNights()는 dateFrom~dateTo 기간만큼 반환한다")
    void parsedNights_defaultsToDateRange_whenNightsIsEmpty() {
        FlightSearchRequestDto result = rangeRequest(futureDate(10), futureDate(23), List.of());

        assertThat(result.parsedNights()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
    }

    @Test
    @DisplayName("dateFrom과 dateTo가 같은 날이어도 parsedNights()는 최소 1박은 반환한다")
    void parsedNights_defaultsToAtLeastOneNight_whenSameDayRange() {
        FlightSearchRequestDto result = rangeRequest(futureDate(10), futureDate(10), null);

        assertThat(result.parsedNights()).containsExactly(1);
    }

    @Test
    @DisplayName("dateTo가 오늘로부터 3개월을 넘으면 생성에 실패한다")
    void constructor_throws_whenDateRangeExceedsThreeMonths() {
        assertThatThrownBy(() -> rangeRequest(
                        futureDate(10),
                        LocalDate.now().plusMonths(3).plusDays(1).toString(),
                        List.of("3")))
                .isInstanceOf(InvalidFlightSearchException.class);
    }

    @Test
    @DisplayName("sort가 없으면 parsedSort()는 PRICE(최저가순)를 반환한다")
    void parsedSort_defaultsToPrice_whenSortIsMissing() {
        FlightSearchRequestDto result = rangeRequest(futureDate(10), futureDate(41), List.of("3"));

        assertThat(result.parsedSort()).isEqualTo(FlightDealSort.PRICE);
    }

    @Test
    @DisplayName("includeTransfer가 없으면 경유 포함이 기본값이다")
    void isIncludeTransfer_defaultsToTrue_whenMissing() {
        FlightSearchRequestDto result = rangeRequest(futureDate(10), futureDate(41), List.of("3"));

        assertThat(result.isIncludeTransfer()).isTrue();
    }

    @Test
    @DisplayName("includeTransfer=false면 직항만 본다")
    void isIncludeTransfer_isFalse_whenExplicitlyFalse() {
        FlightSearchRequestDto result = fixedRequest(builder -> builder.includeTransfer("false"));

        assertThat(result.isIncludeTransfer()).isFalse();
    }

    @Test
    @DisplayName("includeWeekend/includeHoliday가 없으면 둘 다 꺼짐이 기본값이다")
    void dayFilters_defaultToFalse_whenMissing() {
        FlightSearchRequestDto result = rangeRequest(futureDate(10), futureDate(41), List.of("3"));

        assertThat(result.isIncludeWeekend()).isFalse();
        assertThat(result.isIncludeHoliday()).isFalse();
    }

    @Test
    @DisplayName("adults/children/infants가 없으면 어른 1명, 나머지 0명이 기본값이다")
    void paxCounts_defaultWhenMissing() {
        FlightSearchRequestDto result = rangeRequest(futureDate(10), futureDate(41), List.of("3"));

        assertThat(result.parsedAdults()).isEqualTo(1);
        assertThat(result.parsedChildren()).isEqualTo(0);
        assertThat(result.parsedInfants()).isEqualTo(0);
    }

    @Test
    @DisplayName("locale/currency가 없으면 각각 ko, krw가 기본값이다")
    void localeAndCurrency_defaultWhenMissing() {
        FlightSearchRequestDto result = rangeRequest(futureDate(10), futureDate(41), List.of("3"));

        assertThat(result.parsedLocale()).isEqualTo("ko");
        assertThat(result.parsedCurrency()).isEqualTo("krw");
    }

    @Test
    @DisplayName("생성자는 검증을 FlightSearchRequestValidator에 위임한다 — 실패하면 그대로 전파된다")
    void constructor_delegatesValidationAndPropagatesFailure() {
        assertThatThrownBy(() -> new FlightSearchRequestDto(
                        null,
                        null,
                        futureDate(10),
                        futureDate(41),
                        null,
                        List.of("3"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))
                .isInstanceOf(InvalidFlightSearchException.class);
    }

    @Test
    @DisplayName("searchMode가 없으면 REQUIRED로 실패한다")
    void constructor_throws_whenSearchModeMissing() {
        assertThatThrownBy(() -> new RequestBuilder().searchMode(null).build())
                .isInstanceOf(InvalidFlightSearchException.class);
    }

    @Test
    @DisplayName("searchMode가 fixed/range가 아니면 INVALID_SEARCH_MODE로 실패한다")
    void constructor_throws_whenSearchModeUnknown() {
        assertThatThrownBy(() -> new RequestBuilder().searchMode("FIXED").build())
                .isInstanceOf(InvalidFlightSearchException.class);
    }

    @Test
    @DisplayName("지정날짜(fixed)에 nights를 보내면 실패한다")
    void constructor_throws_whenNightsSentInFixedMode() {
        assertThatThrownBy(() -> fixedRequest(builder -> builder.nights(List.of("3"))))
                .isInstanceOf(InvalidFlightSearchException.class);
    }

    @Test
    @DisplayName("destination과 sector를 함께 보내면 실패한다")
    void constructor_throws_whenSectorCombinedWithDestination() {
        assertThatThrownBy(
                        () -> fixedRequest(builder -> builder.destination("OSA").sector(List.of("JAPAN"))))
                .isInstanceOf(InvalidFlightSearchException.class);
    }

    @Test
    @DisplayName("currency가 krw가 아니면 실패한다")
    void constructor_throws_whenCurrencyNotKrw() {
        assertThatThrownBy(() -> fixedRequest(builder -> builder.currency("usd")))
                .isInstanceOf(InvalidFlightSearchException.class);
    }

    @Test
    @DisplayName("adults가 음수면 실패한다")
    void constructor_throws_whenAdultsNegative() {
        assertThatThrownBy(() -> fixedRequest(builder -> builder.adults("-1")))
                .isInstanceOf(InvalidFlightSearchException.class);
    }

    @Test
    @DisplayName("equals/hashCode는 cursor/limit이 달라도 같은 검색 조건이면 같다고 본다")
    void equalsAndHashCode_ignoreCursorAndLimit() {
        FlightSearchRequestDto a = rangeRequestWithPaging("tok_0001", "10");
        FlightSearchRequestDto b = rangeRequestWithPaging("tok_0002", "20");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("equals는 cursor/limit 외의 필드가 다르면 다르다고 본다")
    void equals_stillDiffersOnOtherFields() {
        FlightSearchRequestDto a = rangeRequest(futureDate(10), futureDate(41), List.of("3"));
        FlightSearchRequestDto b = rangeRequest(futureDate(10), futureDate(41), List.of("4"));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("cursor가 없으면 첫 요청이고, 있으면 첫 요청이 아니다")
    void isFirstRequest_reflectsCursorPresence() {
        FlightSearchRequestDto withoutCursor = rangeRequest(futureDate(10), futureDate(41), List.of("3"));
        FlightSearchRequestDto withCursor = rangeRequestWithPaging("tok_0001", null);

        assertThat(withoutCursor.isFirstRequest()).isTrue();
        assertThat(withCursor.isFirstRequest()).isFalse();
    }

    @Test
    @DisplayName("limit이 없으면 boundedLimit()는 기본값 20을 반환한다")
    void boundedLimit_defaultsWhenMissing() {
        FlightSearchRequestDto result = rangeRequest(futureDate(10), futureDate(41), List.of("3"));

        assertThat(result.boundedLimit()).isEqualTo(20);
    }

    @Test
    @DisplayName("limit이 범위를 벗어나거나 숫자가 아니면 boundedLimit()가 조용히 잘라내거나 기본값을 쓴다")
    void boundedLimit_clampsOrDefaults() {
        FlightSearchRequestDto tooSmall = fixedRequest(builder -> builder.limit("0"));
        FlightSearchRequestDto tooBig = fixedRequest(builder -> builder.limit("999"));
        FlightSearchRequestDto notNumeric = fixedRequest(builder -> builder.limit("abc"));

        assertThat(tooSmall.boundedLimit()).isEqualTo(1);
        assertThat(tooBig.boundedLimit()).isEqualTo(50);
        assertThat(notNumeric.boundedLimit()).isEqualTo(20);
    }

    private static FlightSearchRequestDto rangeRequest(String dateFrom, String dateTo, List<String> nights) {
        return new RequestBuilder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .nights(nights)
                .build();
    }

    private static FlightSearchRequestDto rangeRequestWithPaging(String cursor, String limit) {
        return new RequestBuilder()
                .dateFrom(futureDate(10))
                .dateTo(futureDate(41))
                .nights(List.of("3"))
                .cursor(cursor)
                .limit(limit)
                .build();
    }

    private static FlightSearchRequestDto fixedRequest(java.util.function.Consumer<RequestBuilder> customize) {
        RequestBuilder builder = new RequestBuilder().searchMode("fixed");
        customize.accept(builder);
        return builder.build();
    }

    /** 18개 필드를 매번 전부 나열하지 않도록 테스트 전용으로 둔 최소 빌더. */
    private static final class RequestBuilder {
        private String origin = "ICN";
        private String searchMode = "range";
        private String dateFrom = futureDate(10);
        private String dateTo = futureDate(41);
        private String destination;
        private List<String> nights;
        private List<String> sector;
        private String includeWeekend;
        private String includeHoliday;
        private String includeTransfer;
        private String adults;
        private String children;
        private String infants;
        private String sort;
        private String cursor;
        private String limit;
        private String locale;
        private String currency;

        RequestBuilder searchMode(String value) {
            this.searchMode = value;
            return this;
        }

        RequestBuilder dateFrom(String value) {
            this.dateFrom = value;
            return this;
        }

        RequestBuilder dateTo(String value) {
            this.dateTo = value;
            return this;
        }

        RequestBuilder destination(String value) {
            this.destination = value;
            return this;
        }

        RequestBuilder nights(List<String> value) {
            this.nights = value;
            return this;
        }

        RequestBuilder sector(List<String> value) {
            this.sector = value;
            return this;
        }

        RequestBuilder includeTransfer(String value) {
            this.includeTransfer = value;
            return this;
        }

        RequestBuilder adults(String value) {
            this.adults = value;
            return this;
        }

        RequestBuilder currency(String value) {
            this.currency = value;
            return this;
        }

        RequestBuilder cursor(String value) {
            this.cursor = value;
            return this;
        }

        RequestBuilder limit(String value) {
            this.limit = value;
            return this;
        }

        FlightSearchRequestDto build() {
            return new FlightSearchRequestDto(
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
                    cursor,
                    limit,
                    locale,
                    currency);
        }
    }
}

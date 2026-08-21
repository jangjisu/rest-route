package com.restroute.flight.controller.dto;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import com.restroute.flight.controller.response.FlightApiError;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightSearchRequestValidatorTest {

    private static String futureDate(int daysFromToday) {
        return LocalDate.now().plusDays(daysFromToday).toString();
    }

    @Test
    @DisplayName("전부 유효하면 예외를 던지지 않는다")
    void validate_doesNotThrow_whenAllValid() {
        assertThatCode(() -> FlightSearchRequestValidator.validate(
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
                        "DATE",
                        "KRW"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("destination/nights/sector/includeWeekend/includeHoliday/includeTransfer/sort/currency 없이도(옵션) 통과한다")
    void validate_passesWithoutOptionalFields() {
        assertThatCode(() -> FlightSearchRequestValidator.validate(
                        "ICN", "range", futureDate(10), futureDate(41), null, null, null, null, null, null, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("origin이 없으면 REQUIRED를 반환한다")
    void validate_flagsMissingOrigin() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.origin(null)))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("origin", "required")));
    }

    @Test
    @DisplayName("origin이 IATA 코드 형식이 아니면 INVALID_IATA_CODE를 반환한다")
    void validate_flagsInvalidOriginFormat() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.origin("seoul")))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("origin", "invalid_iata_code")));
    }

    @Test
    @DisplayName("searchMode가 없으면 REQUIRED를 반환한다")
    void validate_flagsMissingSearchMode() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.searchMode(null)))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("searchMode", "required")));
    }

    @Test
    @DisplayName("searchMode가 fixed/range가 아니면 INVALID_SEARCH_MODE를 반환한다")
    void validate_flagsUnknownSearchMode() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.searchMode("FIXED")))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("searchMode", "invalid_search_mode")));
    }

    @Test
    @DisplayName("destination이 있는데 IATA 코드 형식이 아니면 INVALID_IATA_CODE를 반환한다")
    void validate_flagsInvalidDestinationFormat() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.destination("osaka")))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("destination", "invalid_iata_code")));
    }

    @Test
    @DisplayName("dateFrom이 없으면 REQUIRED를 반환한다")
    void validate_flagsMissingDateFrom() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.dateFrom(null)))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("dateFrom", "required")));
    }

    @Test
    @DisplayName("dateFrom 형식이 이상하면 INVALID_DATE_FORMAT을 반환한다")
    void validate_flagsInvalidDateFromFormat() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.dateFrom("2099/01/10")))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("dateFrom", "invalid_date_format")));
    }

    @Test
    @DisplayName("dateFrom이 과거면 PAST_DATE_NOT_ALLOWED를 반환한다")
    void validate_flagsPastDateFrom() {
        assertThatThrownBy(() -> validMinimalExcept(
                        builder -> builder.dateFrom("2000-01-01").dateTo("2000-01-10")))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("dateFrom", "past_date_not_allowed")));
    }

    @Test
    @DisplayName("dateTo가 dateFrom보다 빠르면 BEFORE_DATE_FROM을 반환한다")
    void validate_flagsReversedDateRange() {
        assertThatThrownBy(() -> validMinimalExcept(
                        builder -> builder.dateFrom(futureDate(20)).dateTo(futureDate(10))))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("dateTo", "before_date_from")));
    }

    @Test
    @DisplayName("dateTo가 오늘로부터 3개월을 넘으면 DATE_RANGE_TOO_WIDE를 반환한다")
    void validate_flagsDateRangeExceedingThreeMonths() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.dateFrom(futureDate(10))
                        .dateTo(LocalDate.now().plusMonths(3).plusDays(1).toString())))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("dateTo", "date_range_too_wide")));
    }

    @Test
    @DisplayName("dateTo가 정확히 오늘로부터 3개월이면(경계값) 통과한다")
    void validate_passesWhenDateRangeIsExactlyThreeMonths() {
        assertThatCode(() -> validMinimalExcept(builder -> builder.dateFrom(futureDate(1))
                        .dateTo(LocalDate.now().plusMonths(3).toString())))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nights가 비어있어도(옵션) 통과한다")
    void validate_passesWithoutNights() {
        assertThatCode(() -> validMinimalExcept(builder -> builder.nights(List.of())))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nights에 숫자가 아닌 값이 있으면 INVALID_NIGHTS_VALUE를 반환한다")
    void validate_flagsInvalidNightsValue() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.nights(List.of("three"))))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("nights", "invalid_nights_value")));
    }

    @Test
    @DisplayName("nights가 1 미만이거나 90 초과면 INVALID_NIGHTS_VALUE를 반환한다")
    void validate_flagsOutOfRangeNightsValue() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.nights(List.of("0", "91"))))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(
                        new FlightApiError.Detail("nights", "invalid_nights_value"),
                        new FlightApiError.Detail("nights", "invalid_nights_value")));
    }

    @Test
    @DisplayName("지정날짜(fixed)에 nights를 보내면 NIGHTS_NOT_ALLOWED_IN_FIXED_MODE를 반환한다")
    void validate_flagsNightsInFixedMode() {
        assertThatThrownBy(() -> validMinimalExcept(
                        builder -> builder.searchMode("fixed").nights(List.of("3"))))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("nights", "nights_not_allowed_in_fixed_mode")));
    }

    @Test
    @DisplayName("sector에 알 수 없는 값이 있으면 INVALID_SECTOR를 반환한다")
    void validate_flagsUnknownSector() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.sector(List.of("EUROPE"))))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("sector", "invalid_sector")));
    }

    @Test
    @DisplayName("sector가 알려진 지역권(일본/동남아/중화권/괌사이판)이면 통과한다")
    void validate_passesKnownSectorValues() {
        assertThatCode(() -> validMinimalExcept(builder -> builder.sector(List.of("JAPAN"))))
                .doesNotThrowAnyException();
        assertThatCode(() -> validMinimalExcept(builder -> builder.sector(List.of("SOUTHEAST_ASIA"))))
                .doesNotThrowAnyException();
        assertThatCode(() -> validMinimalExcept(builder -> builder.sector(List.of("GREATER_CHINA"))))
                .doesNotThrowAnyException();
        assertThatCode(() -> validMinimalExcept(builder -> builder.sector(List.of("GUAM_SAIPAN"))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("destination과 sector를 함께 보내면 SECTOR_DESTINATION_CONFLICT를 반환한다")
    void validate_flagsSectorCombinedWithDestination() {
        assertThatThrownBy(() ->
                        validMinimalExcept(builder -> builder.destination("OSA").sector(List.of("JAPAN"))))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("sector", "sector_destination_conflict")));
    }

    @Test
    @DisplayName("includeWeekend가 true/false가 아니면 INVALID_INCLUDE_WEEKEND를 반환한다")
    void validate_flagsInvalidIncludeWeekend() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.includeWeekend("yes")))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("includeWeekend", "invalid_include_weekend")));
    }

    @Test
    @DisplayName("includeHoliday가 true/false가 아니면 INVALID_INCLUDE_HOLIDAY를 반환한다")
    void validate_flagsInvalidIncludeHoliday() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.includeHoliday("yes")))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("includeHoliday", "invalid_include_holiday")));
    }

    @Test
    @DisplayName("includeTransfer가 true/false가 아니면 INVALID_INCLUDE_TRANSFER를 반환한다")
    void validate_flagsInvalidIncludeTransfer() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.includeTransfer("yes")))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("includeTransfer", "invalid_include_transfer")));
    }

    @Test
    @DisplayName("sort가 PRICE/DATE가 아니면 INVALID_SORT를 반환한다")
    void validate_flagsInvalidSort() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.sort("CHEAPEST")))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("sort", "invalid_sort")));
    }

    @Test
    @DisplayName("sort가 PRICE/DATE면 통과한다")
    void validate_passesKnownSortValues() {
        assertThatCode(() -> validMinimalExcept(builder -> builder.sort("PRICE")))
                .doesNotThrowAnyException();
        assertThatCode(() -> validMinimalExcept(builder -> builder.sort("DATE")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("currency가 krw가 아니면(대소문자 무관) INVALID_CURRENCY를 반환한다")
    void validate_flagsInvalidCurrency() {
        assertThatThrownBy(() -> validMinimalExcept(builder -> builder.currency("usd")))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("currency", "invalid_currency")));
    }

    @Test
    @DisplayName("currency가 krw면(대소문자 무관) 통과한다")
    void validate_passesKrwCurrencyCaseInsensitively() {
        assertThatCode(() -> validMinimalExcept(builder -> builder.currency("KRW")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("여러 필드가 동시에 잘못되면 전부 모아서 반환한다")
    void validate_collectsMultipleDetailsAtOnce() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        null, null, null, null, null, null, null, null, null, null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(
                        new FlightApiError.Detail("origin", "required"),
                        new FlightApiError.Detail("searchMode", "required"),
                        new FlightApiError.Detail("dateFrom", "required"),
                        new FlightApiError.Detail("dateTo", "required")));
    }

    private static void validMinimalExcept(java.util.function.Consumer<ValidateArgs> customize) {
        ValidateArgs args = new ValidateArgs();
        customize.accept(args);
        args.invoke();
    }

    /** validate()의 12개 인자를 매번 전부 나열하지 않도록 테스트 전용으로 둔 최소 빌더. */
    private static final class ValidateArgs {
        private String origin = "ICN";
        private String searchMode = "range";
        private String dateFrom = futureDate(10);
        private String dateTo = futureDate(41);
        private String destination;
        private List<String> nights = List.of("3");
        private List<String> sector;
        private String includeWeekend;
        private String includeHoliday;
        private String includeTransfer;
        private String sort;
        private String currency;

        ValidateArgs origin(String value) {
            this.origin = value;
            return this;
        }

        ValidateArgs searchMode(String value) {
            this.searchMode = value;
            return this;
        }

        ValidateArgs dateFrom(String value) {
            this.dateFrom = value;
            return this;
        }

        ValidateArgs dateTo(String value) {
            this.dateTo = value;
            return this;
        }

        ValidateArgs destination(String value) {
            this.destination = value;
            return this;
        }

        ValidateArgs nights(List<String> value) {
            this.nights = value;
            return this;
        }

        ValidateArgs sector(List<String> value) {
            this.sector = value;
            return this;
        }

        ValidateArgs includeWeekend(String value) {
            this.includeWeekend = value;
            return this;
        }

        ValidateArgs includeHoliday(String value) {
            this.includeHoliday = value;
            return this;
        }

        ValidateArgs includeTransfer(String value) {
            this.includeTransfer = value;
            return this;
        }

        ValidateArgs sort(String value) {
            this.sort = value;
            return this;
        }

        ValidateArgs currency(String value) {
            this.currency = value;
            return this;
        }

        void invoke() {
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
                    sort,
                    currency);
        }
    }
}

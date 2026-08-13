package com.restroute.flight.controller.dto;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import com.restroute.flight.controller.response.FlightApiError;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightSearchRequestValidatorTest {

    @Test
    @DisplayName("전부 유효하면 예외를 던지지 않는다")
    void validate_doesNotThrow_whenAllValid() {
        assertThatCode(() -> FlightSearchRequestValidator.validate(
                        "ICN",
                        "OSA",
                        "2099-01-10",
                        "2099-02-10",
                        List.of("3", "4"),
                        List.of("JAPAN"),
                        List.of("INCLUDE_WEEKEND"),
                        "true"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("destination/filter/dayOption/includeTransfer 없이도(옵션) 통과한다")
    void validate_passesWithoutOptionalFields() {
        assertThatCode(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("origin이 없으면 REQUIRED를 반환한다")
    void validate_flagsMissingOrigin() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        null, null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("origin", "REQUIRED")));
    }

    @Test
    @DisplayName("origin이 IATA 코드 형식이 아니면 INVALID_IATA_CODE를 반환한다")
    void validate_flagsInvalidOriginFormat() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "seoul", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("origin", "INVALID_IATA_CODE")));
    }

    @Test
    @DisplayName("destination이 있는데 IATA 코드 형식이 아니면 INVALID_IATA_CODE를 반환한다")
    void validate_flagsInvalidDestinationFormat() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "ICN", "osaka", "2099-01-10", "2099-02-10", List.of("3"), null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("destination", "INVALID_IATA_CODE")));
    }

    @Test
    @DisplayName("dateFrom이 없으면 REQUIRED를 반환한다")
    void validate_flagsMissingDateFrom() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, null, "2099-02-10", List.of("3"), null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("dateFrom", "REQUIRED")));
    }

    @Test
    @DisplayName("dateFrom 형식이 이상하면 INVALID_DATE_FORMAT을 반환한다")
    void validate_flagsInvalidDateFromFormat() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, "2099/01/10", "2099-02-10", List.of("3"), null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("dateFrom", "INVALID_DATE_FORMAT")));
    }

    @Test
    @DisplayName("dateFrom이 과거면 PAST_DATE_NOT_ALLOWED를 반환한다")
    void validate_flagsPastDateFrom() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, "2000-01-01", "2000-01-10", List.of("3"), null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("dateFrom", "PAST_DATE_NOT_ALLOWED")));
    }

    @Test
    @DisplayName("dateTo가 dateFrom보다 빠르면 BEFORE_DATE_FROM을 반환한다")
    void validate_flagsReversedDateRange() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, "2099-02-10", "2099-01-10", List.of("3"), null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("dateTo", "BEFORE_DATE_FROM")));
    }

    @Test
    @DisplayName("날짜 범위가 3개월을 넘으면 DATE_RANGE_TOO_WIDE를 반환한다")
    void validate_flagsDateRangeTooWide() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, "2099-01-01", "2099-06-01", List.of("3"), null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("dateTo", "DATE_RANGE_TOO_WIDE")));
    }

    @Test
    @DisplayName("nights가 비어있어도(옵션) 통과한다")
    void validate_passesWithoutNights() {
        assertThatCode(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, "2099-01-10", "2099-02-10", List.of(), null, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nights에 숫자가 아닌 값이 있으면 INVALID_NIGHTS_VALUE를 반환한다")
    void validate_flagsInvalidNightsValue() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, "2099-01-10", "2099-02-10", List.of("three"), null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("nights", "INVALID_NIGHTS_VALUE")));
    }

    @Test
    @DisplayName("nights가 1 미만이거나 90 초과면 INVALID_NIGHTS_VALUE를 반환한다")
    void validate_flagsOutOfRangeNightsValue() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, "2099-01-10", "2099-02-10", List.of("0", "91"), null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(
                        new FlightApiError.Detail("nights", "INVALID_NIGHTS_VALUE"),
                        new FlightApiError.Detail("nights", "INVALID_NIGHTS_VALUE")));
    }

    @Test
    @DisplayName("filter에 알 수 없는 값이 있으면 INVALID_FILTER를 반환한다")
    void validate_flagsUnknownFilter() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), List.of("EUROPE"), null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("filter", "INVALID_FILTER")));
    }

    @Test
    @DisplayName("dayOption에 알 수 없는 값이 있으면 INVALID_DAY_OPTION을 반환한다")
    void validate_flagsUnknownDayOption() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, List.of("HOLIDAY_ONLY"), null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("dayOption", "INVALID_DAY_OPTION")));
    }

    @Test
    @DisplayName("includeTransfer가 true/false가 아니면 INVALID_INCLUDE_TRANSFER를 반환한다")
    void validate_flagsInvalidIncludeTransfer() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(
                        "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, "yes"))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(new FlightApiError.Detail("includeTransfer", "INVALID_INCLUDE_TRANSFER")));
    }

    @Test
    @DisplayName("여러 필드가 동시에 잘못되면 전부 모아서 반환한다")
    void validate_collectsMultipleDetailsAtOnce() {
        assertThatThrownBy(() ->
                        FlightSearchRequestValidator.validate(null, null, null, null, List.of(), null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .extracting(e -> ((InvalidFlightSearchException) e).details())
                .isEqualTo(List.of(
                        new FlightApiError.Detail("origin", "REQUIRED"),
                        new FlightApiError.Detail("dateFrom", "REQUIRED"),
                        new FlightApiError.Detail("dateTo", "REQUIRED")));
    }
}

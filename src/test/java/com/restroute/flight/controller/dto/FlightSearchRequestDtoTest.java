package com.restroute.flight.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightSearchRequestDtoTest {

    @Test
    @DisplayName("유효한 값이면 생성되고, 파싱된 접근자도 올바른 값을 반환한다")
    void constructor_succeeds_andExposesParsedAccessors() {
        FlightSearchRequestDto result = new FlightSearchRequestDto(
                "ICN",
                "OSA",
                "2099-01-10",
                "2099-02-10",
                List.of("3", "4"),
                List.of("JAPAN"),
                List.of("INCLUDE_WEEKEND"),
                "true",
                "DATE",
                null,
                "20");

        assertThat(result.origin()).isEqualTo("ICN");
        assertThat(result.destination()).isEqualTo("OSA");
        assertThat(result.parsedDateFrom()).isEqualTo(LocalDate.of(2099, 1, 10));
        assertThat(result.parsedDateTo()).isEqualTo(LocalDate.of(2099, 2, 10));
        assertThat(result.parsedNights()).containsExactly(3, 4);
        assertThat(result.isIncludeTransfer()).isTrue();
        assertThat(result.parsedSort()).isEqualTo(FlightDealSort.DATE);
    }

    @Test
    @DisplayName("nights가 없으면(null) parsedNights()는 dateFrom~dateTo 기간만큼 1박부터 전체를 반환한다")
    void parsedNights_defaultsToDateRange_whenNightsIsNull() {
        FlightSearchRequestDto result = new FlightSearchRequestDto(
                "ICN", "OSA", "2099-01-10", "2099-01-18", null, null, null, null, null, null, null);

        assertThat(result.parsedNights()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    @DisplayName("nights가 빈 리스트여도 parsedNights()는 dateFrom~dateTo 기간만큼 반환한다")
    void parsedNights_defaultsToDateRange_whenNightsIsEmpty() {
        FlightSearchRequestDto result = new FlightSearchRequestDto(
                "ICN", "OSA", "2099-01-10", "2099-01-23", List.of(), null, null, null, null, null, null);

        assertThat(result.parsedNights()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
    }

    @Test
    @DisplayName("dateFrom과 dateTo가 같은 날이어도 parsedNights()는 최소 1박은 반환한다")
    void parsedNights_defaultsToAtLeastOneNight_whenSameDayRange() {
        FlightSearchRequestDto result = new FlightSearchRequestDto(
                "ICN", "OSA", "2099-01-10", "2099-01-10", null, null, null, null, null, null, null);

        assertThat(result.parsedNights()).containsExactly(1);
    }

    @Test
    @DisplayName("sort가 없으면 parsedSort()는 PRICE(최저가순)를 반환한다")
    void parsedSort_defaultsToPrice_whenSortIsMissing() {
        FlightSearchRequestDto result = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, null, null, null);

        assertThat(result.parsedSort()).isEqualTo(FlightDealSort.PRICE);
    }

    @Test
    @DisplayName("sort=DATE면 parsedSort()는 DATE를 반환한다")
    void parsedSort_returnsDate_whenSortIsDate() {
        FlightSearchRequestDto result = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, "DATE", null, null);

        assertThat(result.parsedSort()).isEqualTo(FlightDealSort.DATE);
    }

    @Test
    @DisplayName("생성자는 검증을 FlightSearchRequestValidator에 위임한다 — 실패하면 그대로 전파된다")
    void constructor_delegatesValidationAndPropagatesFailure() {
        assertThatThrownBy(() -> new FlightSearchRequestDto(
                        null, null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, null, null, null))
                .isInstanceOf(InvalidFlightSearchException.class);
    }

    @Test
    @DisplayName("equals/hashCode는 cursor/size가 달라도 같은 검색 조건이면 같다고 본다")
    void equalsAndHashCode_ignoreCursorAndSize() {
        FlightSearchRequestDto a = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, null, "tok_0001", "10");
        FlightSearchRequestDto b = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, null, "tok_0002", "20");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("equals는 cursor/size 외의 필드가 다르면 다르다고 본다")
    void equals_stillDiffersOnOtherFields() {
        FlightSearchRequestDto a = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, null, null, null);
        FlightSearchRequestDto b = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("4"), null, null, null, null, null, null);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("equals는 sort가 다르면 다르다고 본다")
    void equals_differsWhenSortDiffers() {
        FlightSearchRequestDto a = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, "PRICE", null, null);
        FlightSearchRequestDto b = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, "DATE", null, null);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("cursor가 없으면 첫 요청이고, 있으면 첫 요청이 아니다")
    void isFirstRequest_reflectsCursorPresence() {
        FlightSearchRequestDto withoutCursor = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, null, null, null);
        FlightSearchRequestDto withCursor = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, null, "tok_0001", null);

        assertThat(withoutCursor.isFirstRequest()).isTrue();
        assertThat(withCursor.isFirstRequest()).isFalse();
    }

    @Test
    @DisplayName("size가 없으면 boundedSize()는 기본값 20을 반환한다")
    void boundedSize_defaultsWhenMissing() {
        FlightSearchRequestDto result = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, null, null, null);

        assertThat(result.boundedSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("size가 범위를 벗어나거나 숫자가 아니면 boundedSize()가 조용히 잘라내거나 기본값을 쓴다")
    void boundedSize_clampsOrDefaults() {
        FlightSearchRequestDto tooSmall = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, null, null, "0");
        FlightSearchRequestDto tooBig = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, null, null, "999");
        FlightSearchRequestDto notNumeric = new FlightSearchRequestDto(
                "ICN", null, "2099-01-10", "2099-02-10", List.of("3"), null, null, null, null, null, "abc");

        assertThat(tooSmall.boundedSize()).isEqualTo(1);
        assertThat(tooBig.boundedSize()).isEqualTo(50);
        assertThat(notNumeric.boundedSize()).isEqualTo(20);
    }
}

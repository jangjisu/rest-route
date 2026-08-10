package com.restroute.flight.controller;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightSearchRequestValidatorTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);
    private static final LocalDate TO = LocalDate.of(2026, 7, 31);

    @Test
    @DisplayName("유효한 값이면 예외를 던지지 않는다")
    void validate_passesForValidRequest() {
        assertThatCode(() -> FlightSearchRequestValidator.validate(FROM, TO, List.of(3, 4), List.of("JAPAN")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("지역권/박수가 없어도(옵션) 통과한다")
    void validate_passesWithoutOptionalValues() {
        assertThatCode(() -> FlightSearchRequestValidator.validate(FROM, TO, List.of(3), null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠르면 한글 메시지로 예외를 던진다")
    void validate_throwsForReversedDateRange() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(TO, FROM, List.of(3), null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .hasMessageContaining("시작일")
                .hasMessageContaining("빠를 수 없습니다");
    }

    @Test
    @DisplayName("날짜 범위가 3개월을 넘으면 한글 메시지로 예외를 던진다")
    void validate_throwsForDateRangeTooWide() {
        LocalDate tooFar = FROM.plusMonths(3).plusDays(1);

        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(FROM, tooFar, List.of(3), null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .hasMessageContaining("최대 3개월");
    }

    @Test
    @DisplayName("nights가 비어있으면 한글 메시지로 예외를 던진다")
    void validate_throwsForEmptyNights() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(FROM, TO, List.of(), null))
                .isInstanceOf(InvalidFlightSearchException.class)
                .hasMessageContaining("최소 1개 이상 선택");
    }

    @Test
    @DisplayName("알 수 없는 지역권이면 한글 메시지로 예외를 던진다")
    void validate_throwsForUnknownRegion() {
        assertThatThrownBy(() -> FlightSearchRequestValidator.validate(FROM, TO, List.of(3), List.of("EUROPE")))
                .isInstanceOf(InvalidFlightSearchException.class)
                .hasMessageContaining("알 수 없는 지역권");
    }
}

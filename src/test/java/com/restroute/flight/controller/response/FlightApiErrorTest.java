package com.restroute.flight.controller.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightApiErrorTest {

    @Test
    @DisplayName("of는 details 없이 code/message만 채운다")
    void of_setsCodeAndMessageWithoutDetails() {
        FlightApiError error = FlightApiError.of("DEAL_NOT_FOUND", "Deal not found or already expired");

        assertThat(error.code()).isEqualTo("DEAL_NOT_FOUND");
        assertThat(error.message()).isEqualTo("Deal not found or already expired");
        assertThat(error.details()).isNull();
    }

    @Test
    @DisplayName("details는 field/code 쌍을 그대로 담는다")
    void details_holdsFieldAndCodePairs() {
        FlightApiError.Detail detail = new FlightApiError.Detail("origin", "INVALID_IATA_CODE");
        FlightApiError error = new FlightApiError("VALIDATION_FAILED", "2 fields are invalid", List.of(detail));

        assertThat(error.details()).containsExactly(detail);
        assertThat(error.details().get(0).field()).isEqualTo("origin");
        assertThat(error.details().get(0).code()).isEqualTo("INVALID_IATA_CODE");
    }
}

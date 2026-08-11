package com.restroute.flight.controller.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightApiResponseTest {

    @Test
    @DisplayName("success는 data를 채우고 error는 null이다")
    void success_setsDataAndNullError() {
        FlightApiResponse<String> response = FlightApiResponse.success("ok");

        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.error()).isNull();
    }

    @Test
    @DisplayName("error는 data가 null이고 전달한 error를 그대로 담는다")
    void error_setsNullDataAndGivenError() {
        FlightApiError error = FlightApiError.of("DEAL_NOT_FOUND", "Deal not found or already expired");

        FlightApiResponse<String> response = FlightApiResponse.error(error);

        assertThat(response.data()).isNull();
        assertThat(response.error()).isSameAs(error);
    }
}

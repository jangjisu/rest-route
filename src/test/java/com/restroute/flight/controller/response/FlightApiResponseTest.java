package com.restroute.flight.controller.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightApiResponseTest {

    @Test
    @DisplayName("success는 data/meta를 채우고 error는 null이다")
    void success_setsDataAndMetaAndNullError() {
        FlightDealSearchMeta meta = FlightDealSearchMeta.of(null, false, 0, "ko", "krw", "2026-08-14T14:03:00+09:00");

        FlightApiResponse<String> response = FlightApiResponse.success("ok", meta);

        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.meta()).isSameAs(meta);
        assertThat(response.error()).isNull();
    }

    @Test
    @DisplayName("error는 data/meta가 null이고 전달한 error를 그대로 담는다")
    void error_setsNullDataAndMetaAndGivenError() {
        FlightApiError error = FlightApiError.of("deal_not_found", "Deal not found or already expired");

        FlightApiResponse<String> response = FlightApiResponse.error(error);

        assertThat(response.data()).isNull();
        assertThat(response.meta()).isNull();
        assertThat(response.error()).isSameAs(error);
    }
}

package com.restroute.flight.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import com.restroute.flight.controller.response.FlightApiError;
import com.restroute.flight.controller.response.FlightApiResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class FlightExceptionHandlerTest {

    private final FlightExceptionHandler handler = new FlightExceptionHandler();

    @Test
    @DisplayName("커서를 찾을 수 없으면 404와 DEAL_NOT_FOUND 에러를 반환한다")
    void handleDealNotFound_returnsNotFoundError() {
        ResponseEntity<FlightApiResponse<Void>> response =
                handler.handleDealNotFound(new FlightDealNotFoundException("bogus"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().error().code()).isEqualTo("DEAL_NOT_FOUND");
        assertThat(response.getBody().error().message()).isEqualTo("Deal not found or already expired");
    }

    @Test
    @DisplayName("검증 실패는 400과 VALIDATION_FAILED, 필드별 details를 반환한다")
    void handleInvalidSearch_returnsValidationFailedError() {
        List<FlightApiError.Detail> details = List.of(
                new FlightApiError.Detail("origin", "INVALID_IATA_CODE"),
                new FlightApiError.Detail("dateFrom", "PAST_DATE_NOT_ALLOWED"));

        ResponseEntity<FlightApiResponse<Void>> response =
                handler.handleInvalidSearch(new InvalidFlightSearchException(details));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().error().message()).isEqualTo("2 fields are invalid");
        assertThat(response.getBody().error().details()).isEqualTo(details);
    }
}

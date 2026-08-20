package com.restroute.flight.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.client.exception.TravelpayoutsApiException;
import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import com.restroute.flight.controller.response.FlightApiError;
import com.restroute.flight.controller.response.FlightApiResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

class FlightExceptionHandlerTest {

    private final FlightExceptionHandler handler = new FlightExceptionHandler();

    @Test
    @DisplayName("커서를 찾을 수 없으면 404와 deal_not_found 에러를 반환한다")
    void handleDealNotFound_returnsNotFoundError() {
        ResponseEntity<FlightApiResponse<Void>> response =
                handler.handleDealNotFound(new FlightDealNotFoundException("bogus"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().meta()).isNull();
        assertThat(response.getBody().error().code()).isEqualTo("deal_not_found");
        assertThat(response.getBody().error().message()).isEqualTo("Deal not found or already expired");
    }

    @Test
    @DisplayName("검증 실패는 400과 validation_failed, 필드별 details를 반환한다")
    void handleInvalidSearch_returnsValidationFailedError() {
        List<FlightApiError.Detail> details = List.of(
                new FlightApiError.Detail("origin", "invalid_iata_code"),
                new FlightApiError.Detail("dateFrom", "past_date_not_allowed"));

        ResponseEntity<FlightApiResponse<Void>> response =
                handler.handleInvalidSearch(new InvalidFlightSearchException(details));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().meta()).isNull();
        assertThat(response.getBody().error().code()).isEqualTo("validation_failed");
        assertThat(response.getBody().error().message()).isEqualTo("입력값을 확인해 주세요");
        assertThat(response.getBody().error().details()).isEqualTo(details);
    }

    @Test
    @DisplayName("외부 API 호출 실패는 flight 전용 봉투로 external_api_unavailable을 반환한다")
    void handleExternalApiFailure_returnsFlightEnvelope() {
        ResponseEntity<FlightApiResponse<Void>> response =
                handler.handleExternalApiFailure(new TravelpayoutsApiException("grouped prices", "success=false"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().meta()).isNull();
        assertThat(response.getBody().error().code()).isEqualTo("external_api_unavailable");
    }

    @Test
    @DisplayName("필수 파라미터가 아예 없으면 400과 validation_failed, 그 필드 하나만 담긴 details를 반환한다")
    void handleMissingRequestParameter_returnsValidationFailedError() {
        ResponseEntity<FlightApiResponse<Void>> response =
                handler.handleMissingRequestParameter(new MissingServletRequestParameterException("origin", "String"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().meta()).isNull();
        assertThat(response.getBody().error().code()).isEqualTo("validation_failed");
        assertThat(response.getBody().error().details())
                .isEqualTo(List.of(new FlightApiError.Detail("origin", "required")));
    }
}

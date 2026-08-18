package com.restroute.flight.controller;

import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.exception.InvalidFlightSearchException;
import com.restroute.flight.controller.response.FlightApiError;
import com.restroute.flight.controller.response.FlightApiResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * flight 컨트롤러 전용 예외 처리기. 공용 {@code GlobalExceptionHandler}(ApiResponse 형태)와
 * 별개로, flight 패키지 안에서만 {@code FlightApiResponse}({data, error}) 형태로 응답한다.
 *
 * <p>{@code @Order}로 공용 GlobalExceptionHandler보다 먼저 평가되게 한다 — 안 하면
 * basePackages로 좁혀놨어도 순서상 공용 핸들러의 Exception.class catch-all이 먼저 걸려서
 * flight 전용 형태 대신 공용 500 응답이 나가버린다(실제로 그렇게 나는 것 확인함).
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.restroute.flight.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FlightExceptionHandler {

    private static final String VALIDATION_FAILED_MESSAGE = "입력값을 확인해 주세요";

    @ExceptionHandler(FlightDealNotFoundException.class)
    public ResponseEntity<FlightApiResponse<Void>> handleDealNotFound(FlightDealNotFoundException e) {
        log.warn("Flight deal not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(FlightApiResponse.error(
                        FlightApiError.of("deal_not_found", "Deal not found or already expired")));
    }

    @ExceptionHandler(InvalidFlightSearchException.class)
    public ResponseEntity<FlightApiResponse<Void>> handleInvalidSearch(InvalidFlightSearchException e) {
        log.warn("Invalid flight search request: {}", e.details());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(FlightApiResponse.error(
                        new FlightApiError("validation_failed", VALIDATION_FAILED_MESSAGE, e.details())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<FlightApiResponse<Void>> handleMissingRequestParameter(
            MissingServletRequestParameterException e) {
        log.warn("Missing required flight search parameter: {}", e.getParameterName());
        List<FlightApiError.Detail> details = List.of(new FlightApiError.Detail(e.getParameterName(), "required"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(FlightApiResponse.error(
                        new FlightApiError("validation_failed", VALIDATION_FAILED_MESSAGE, details)));
    }
}

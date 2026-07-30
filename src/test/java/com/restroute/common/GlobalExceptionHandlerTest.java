package com.restroute.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.client.exception.ExApiException;
import com.restroute.client.exception.KakaoApiException;
import com.restroute.service.image.InvalidRestStopImageException;
import com.restroute.service.image.RestStopNotFoundException;
import com.restroute.service.salesranking.SalesRankingUploadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("타입 불일치 예외는 INVALID_PARAMETER 응답으로 변환한다")
    void handleTypeMismatch_returnsInvalidParameter() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", null, new IllegalArgumentException("bad request"));

        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.INVALID_PARAMETER.name());
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseCode.INVALID_PARAMETER.getDefaultMessage());
    }

    @Test
    @DisplayName("일반 예외는 INTERNAL_ERROR 응답으로 변환한다")
    void handleException_returnsInternalError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.INTERNAL_ERROR.name());
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseCode.INTERNAL_ERROR.getDefaultMessage());
    }

    @Test
    @DisplayName("업로드 용량 초과는 INVALID_PARAMETER 응답으로 변환한다")
    void handleMaxUploadSizeExceeded_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(20_000_000));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.INVALID_PARAMETER.name());
    }

    @Test
    @DisplayName("BusinessException 하나로 없는 휴게소 예외를 NOT_FOUND 응답으로 변환하고 메시지를 그대로 노출한다")
    void handleBusinessException_returnsNotFoundForRestStopNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusinessException(RestStopNotFoundException.forServiceAreaCode("UNKNOWN"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.NOT_FOUND.name());
        assertThat(response.getBody().getMessage()).isEqualTo("Rest stop not found: UNKNOWN");
    }

    @Test
    @DisplayName("BusinessException 하나로 잘못된 이미지 예외를 INVALID_PARAMETER 응답으로 변환한다")
    void handleBusinessException_returnsInvalidParameterForInvalidRestStopImage() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusinessException(new InvalidRestStopImageException("invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.INVALID_PARAMETER.name());
        assertThat(response.getBody().getMessage()).isEqualTo("invalid");
    }

    @Test
    @DisplayName("판매 순위 업로드 예외도 BusinessException으로 INVALID_PARAMETER 응답이 된다(이전엔 500으로 새던 버그)")
    void handleBusinessException_returnsInvalidParameterForSalesRankingUpload() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusinessException(SalesRankingUploadException.of("CSV 형식이 올바르지 않습니다."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.INVALID_PARAMETER.name());
        assertThat(response.getBody().getMessage()).isEqualTo("CSV 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("카카오 API 실패는 상세 메시지 대신 기본 메시지로 응답한다(내부 정보 노출 방지)")
    void handleExternalApiException_returnsGenericMessageForKakaoApiException() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleExternalApiException(new KakaoApiException("directions", "timeout"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.EXTERNAL_API_UNAVAILABLE.name());
        assertThat(response.getBody().getMessage())
                .isEqualTo(ResponseCode.EXTERNAL_API_UNAVAILABLE.getDefaultMessage());
    }

    @Test
    @DisplayName("공공 API(ExApi) 실패도 ExternalApiException으로 처리되어 200/기본 메시지로 응답한다(이전엔 500으로 새던 버그)")
    void handleExternalApiException_returnsGenericMessageForExApiException() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleExternalApiException(new ExApiException("https://example.com", "timeout"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.EXTERNAL_API_UNAVAILABLE.name());
        assertThat(response.getBody().getMessage())
                .isEqualTo(ResponseCode.EXTERNAL_API_UNAVAILABLE.getDefaultMessage());
    }
}

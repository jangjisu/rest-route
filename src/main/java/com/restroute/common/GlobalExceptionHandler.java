package com.restroute.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Request parameter type mismatch: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.INVALID_PARAMETER.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestParameter(MissingServletRequestParameterException e) {
        log.warn("Missing required request parameter: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.INVALID_PARAMETER.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("Rest stop image upload exceeds the maximum size: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.INVALID_PARAMETER.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalApiException(ExternalApiException e) {
        log.error("External API call failed", e);
        ResponseCode code = e.responseCode();
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ResponseCode code = e.responseCode();
        log.warn("{}: {}", code, e.getMessage());
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ResponseCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INTERNAL_ERROR));
    }
}

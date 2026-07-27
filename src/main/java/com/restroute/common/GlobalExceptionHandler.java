package com.restroute.common;

import com.restroute.client.exception.KakaoApiException;
import com.restroute.service.admin.InvalidRestFoodEditException;
import com.restroute.service.admin.InvalidRestStopEditException;
import com.restroute.service.admin.RestOilNotFoundException;
import com.restroute.service.image.InvalidRestStopImageException;
import com.restroute.service.image.RestFoodNotFoundException;
import com.restroute.service.image.RestStopNotFoundException;
import com.restroute.service.route.RouteRestStopNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(RouteRestStopNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRouteRestStopNotFound(RouteRestStopNotFoundException e) {
        log.warn("Route rest stops not found: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(RestStopNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRestStopNotFound(RestStopNotFoundException e) {
        log.warn("Rest stop not found: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(InvalidRestStopImageException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRestStopImage(InvalidRestStopImageException e) {
        log.warn("Invalid rest stop image: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.INVALID_PARAMETER.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_PARAMETER, e.getMessage()));
    }

    @ExceptionHandler(InvalidRestStopEditException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRestStopEdit(InvalidRestStopEditException e) {
        log.warn("Invalid rest stop edit: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.INVALID_PARAMETER.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_PARAMETER, e.getMessage()));
    }

    @ExceptionHandler(RestFoodNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRestFoodNotFound(RestFoodNotFoundException e) {
        log.warn("Rest food not found: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(InvalidRestFoodEditException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRestFoodEdit(InvalidRestFoodEditException e) {
        log.warn("Invalid rest food edit: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.INVALID_PARAMETER.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_PARAMETER, e.getMessage()));
    }

    @ExceptionHandler(RestOilNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRestOilNotFound(RestOilNotFoundException e) {
        log.warn("Rest oil not found: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("Rest stop image upload exceeds the maximum size: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.INVALID_PARAMETER.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(KakaoApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleKakaoApiException(KakaoApiException e) {
        log.error("Kakao API call failed", e);
        return ResponseEntity.status(ResponseCode.EXTERNAL_API_UNAVAILABLE.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.EXTERNAL_API_UNAVAILABLE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ResponseCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INTERNAL_ERROR));
    }
}

package com.restroute.common;

public abstract class ExternalApiException extends BusinessException {

    protected ExternalApiException(String message) {
        super(ResponseCode.EXTERNAL_API_UNAVAILABLE, message);
    }

    protected ExternalApiException(String message, Throwable cause) {
        super(ResponseCode.EXTERNAL_API_UNAVAILABLE, message, cause);
    }
}

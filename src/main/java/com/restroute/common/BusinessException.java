package com.restroute.common;

public abstract class BusinessException extends RuntimeException {

    private final ResponseCode responseCode;

    protected BusinessException(ResponseCode responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    protected BusinessException(ResponseCode responseCode, String message, Throwable cause) {
        super(message, cause);
        this.responseCode = responseCode;
    }

    public ResponseCode responseCode() {
        return responseCode;
    }
}

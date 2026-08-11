package com.restroute.flight.client.exception;

import com.restroute.common.ExternalApiException;

public class TravelpayoutsApiException extends ExternalApiException {

    public TravelpayoutsApiException(String requestDescription, String message) {
        super(buildMessage(requestDescription, message));
    }

    public TravelpayoutsApiException(String requestDescription, String message, Throwable cause) {
        super(buildMessage(requestDescription, message), cause);
    }

    private static String buildMessage(String requestDescription, String message) {
        return "Travelpayouts API 호출에 실패했습니다. 요청=" + requestDescription + ", 메시지=" + message;
    }
}

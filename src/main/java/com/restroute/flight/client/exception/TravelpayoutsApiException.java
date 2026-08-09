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
        return "Failed to call Travelpayouts API. request=" + requestDescription + ", message=" + message;
    }
}

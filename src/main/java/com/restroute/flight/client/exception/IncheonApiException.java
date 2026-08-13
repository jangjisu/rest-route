package com.restroute.flight.client.exception;

import com.restroute.common.ExternalApiException;

public class IncheonApiException extends ExternalApiException {

    public IncheonApiException(String requestDescription, String message) {
        super(buildMessage(requestDescription, message));
    }

    public IncheonApiException(String requestDescription, String message, Throwable cause) {
        super(buildMessage(requestDescription, message), cause);
    }

    private static String buildMessage(String requestDescription, String message) {
        return "인천공항 취항 항공사 API 호출에 실패했습니다. 요청=" + requestDescription + ", 메시지=" + message;
    }
}

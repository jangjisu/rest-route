package com.restroute.holiday.client.exception;

import com.restroute.common.ExternalApiException;

public class SpecialDayApiException extends ExternalApiException {

    public SpecialDayApiException(String requestDescription, String message) {
        super(buildMessage(requestDescription, message));
    }

    public SpecialDayApiException(String requestDescription, String message, Throwable cause) {
        super(buildMessage(requestDescription, message), cause);
    }

    private static String buildMessage(String requestDescription, String message) {
        return "특일 정보 API 호출에 실패했습니다. 요청=" + requestDescription + ", 메시지=" + message;
    }
}

package com.restroute.common.client.exception;

import com.restroute.common.ExternalApiException;

public class KakaoApiException extends ExternalApiException {

    public KakaoApiException(String requestDescription, String message) {
        super(buildMessage(requestDescription, message));
    }

    public KakaoApiException(String requestDescription, String message, Throwable cause) {
        super(buildMessage(requestDescription, message), cause);
    }

    private static String buildMessage(String requestDescription, String message) {
        return "Failed to call Kakao API. request=" + requestDescription + ", message=" + message;
    }
}

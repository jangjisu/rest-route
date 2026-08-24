package com.restroute.common.client.exception;

import com.restroute.common.ExternalApiException;
import com.restroute.common.client.ExternalApiRequestLog;

public class ExApiException extends ExternalApiException {

    public ExApiException(String requestUrl, String message) {
        super(buildMessage(requestUrl, message));
    }

    public ExApiException(String requestUrl, String message, Throwable cause) {
        super(buildMessage(requestUrl, message), cause);
    }

    private static String buildMessage(String requestUrl, String message) {
        return "Failed to fetch API. requestUrl=" + ExternalApiRequestLog.sanitizeUrl(requestUrl) + ", message="
                + message;
    }
}

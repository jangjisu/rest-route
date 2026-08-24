package com.restroute.common.client;

import java.util.Set;

public final class ExternalApiRequestLog {

    private static final Set<String> SENSITIVE_PARAMETERS = Set.of("key", "code", "serviceKey");

    private ExternalApiRequestLog() {}

    public static String sanitizeUrl(String requestUrl) {
        if (requestUrl == null || requestUrl.isBlank()) {
            return requestUrl;
        }

        String sanitized = requestUrl;
        for (String parameter : SENSITIVE_PARAMETERS) {
            sanitized = sanitized.replaceAll("([?&])" + parameter + "=[^&]*", "$1" + parameter + "=<redacted>");
        }
        return sanitized.replaceAll("(?i)(serviceKey|key|code)=[^&\\s]*", "$1=<redacted>");
    }
}

package com.restroute.route.service.util;

import java.util.Optional;
import org.springframework.util.StringUtils;

public final class RouteRestStopNumberParser {

    private RouteRestStopNumberParser() {}

    public static Optional<Integer> parsePrice(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        String digits = value.replaceAll("\\D", "");
        if (!StringUtils.hasText(digits)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(digits));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static int parseCount(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        String digits = value.replaceAll("\\D", "");
        if (!StringUtils.hasText(digits)) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

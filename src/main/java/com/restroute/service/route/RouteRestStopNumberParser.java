package com.restroute.service.route;

import java.util.Optional;
import org.springframework.util.StringUtils;

final class RouteRestStopNumberParser {

    private RouteRestStopNumberParser() {}

    static Optional<Integer> parsePrice(String value) {
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

    static int parseCount(String value) {
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

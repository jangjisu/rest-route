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

    /**
     * price가 averagePrice보다 쌀 때만 true. 둘 중 하나라도 숫자로 못 바꾸면 false —
     * "평균보다 싸다"고 확신할 근거가 없다는 뜻이라 안 싼 것과 같게 취급한다.
     */
    public static boolean isBelowAverage(String price, String averagePrice) {
        Optional<Integer> parsedPrice = parsePrice(price);
        Optional<Integer> parsedAverage = parsePrice(averagePrice);
        if (parsedPrice.isEmpty() || parsedAverage.isEmpty()) {
            return false;
        }
        return parsedPrice.get() < parsedAverage.get();
    }
}

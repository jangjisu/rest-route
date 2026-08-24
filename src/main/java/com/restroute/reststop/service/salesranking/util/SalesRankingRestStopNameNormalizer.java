package com.restroute.reststop.service.salesranking.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class SalesRankingRestStopNameNormalizer {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^0-9A-Za-z가-힣]");

    private SalesRankingRestStopNameNormalizer() {}

    public static String normalize(String value) {
        return NON_ALPHANUMERIC
                .matcher(value == null ? "" : value)
                .replaceAll("")
                .toLowerCase(Locale.ROOT);
    }
}

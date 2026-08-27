package com.restroute.reststop.service.salesranking.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class SalesRankingRestStopNameNormalizer {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^0-9A-Za-z가-힣]");
    private static final String REST_STOP_SUFFIX = "휴게소";

    private SalesRankingRestStopNameNormalizer() {}

    public static String normalize(String value) {
        // 원천 CSV마다 "휴게소" 표기 유무/위치가 제각각이다(예: "죽전(서울)" vs "죽전(서울)휴게소",
        // "망향휴게소(부산)" vs "망향(부산)휴게소") — 양쪽에서 "휴게소"를 통째로 제거하고 비교하면
        // 표기 위치와 무관하게 같은 휴게소로 매칭된다.
        String withoutRestStopSuffix = (value == null ? "" : value).replace(REST_STOP_SUFFIX, "");
        return NON_ALPHANUMERIC.matcher(withoutRestStopSuffix).replaceAll("").toLowerCase(Locale.ROOT);
    }
}

package com.restroute.route.service.util;

/** 좌표 문자열 파싱/직렬화만 담당하는 순수 헬퍼. 상태나 의존성이 없어 Spring 빈으로 등록하지 않는다. */
public final class RouteCoordinateFormat {

    private RouteCoordinateFormat() {}

    public static String toParam(double longitude, double latitude) {
        return longitude + "," + latitude;
    }

    public static Double parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

package com.restroute.controller.response;

import java.util.Optional;
import java.util.function.Function;

/**
 * 응답 DTO들이 반복하던 "Optional에서 텍스트를 꺼내 비어 있으면 null로, 아니면 trim해서 반환한다"
 * 패턴을 하나로 묶는다.
 */
final class ResponseTextUtils {

    private ResponseTextUtils() {}

    static <T> String textOf(Optional<T> source, Function<T, String> getter) {
        return source.map(getter)
                .filter(ResponseTextUtils::hasText)
                .map(String::trim)
                .orElse(null);
    }

    static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

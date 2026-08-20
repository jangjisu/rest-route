package com.restroute.flight.client;

import feign.Client;
import feign.Request;
import feign.Response;
import java.io.IOException;
import java.util.Collection;
import lombok.RequiredArgsConstructor;

/**
 * 실제 호출 전에 {@link TravelpayoutsRateLimiter#acquire()}로 게이트를 거치고, 응답을 받으면
 * x-rate-limit-remaining/x-rate-limit-reset 헤더로 재보정한다. Feign의 Client는 디코딩 이전의
 * 원본 HTTP 응답을 다루므로 4xx/5xx 응답의 헤더도 그대로 잡을 수 있다.
 */
@RequiredArgsConstructor
class TravelpayoutsRateLimitingFeignClient implements Client {

    private static final String REMAINING_HEADER = "x-rate-limit-remaining";
    private static final String RESET_HEADER = "x-rate-limit-reset";

    private final TravelpayoutsRateLimiter rateLimiter;
    private final Client delegate;

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        rateLimiter.acquire();
        Response response = delegate.execute(request, options);
        rateLimiter.recalibrate(headerAsInt(response, REMAINING_HEADER), headerAsInt(response, RESET_HEADER));
        return response;
    }

    private static Integer headerAsInt(Response response, String name) {
        Collection<String> values = response.headers().get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(values.iterator().next());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

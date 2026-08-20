package com.restroute.flight.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.Client;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TravelpayoutsRateLimitingFeignClientTest {

    @Mock
    private TravelpayoutsRateLimiter rateLimiter;

    @Mock
    private Client delegate;

    private TravelpayoutsRateLimitingFeignClient client;
    private Request request;
    private Request.Options options;

    @BeforeEach
    void setUp() {
        client = new TravelpayoutsRateLimitingFeignClient(rateLimiter, delegate);
        request = Request.create(
                Request.HttpMethod.GET, "http://example.com", Map.of(), null, StandardCharsets.UTF_8, null);
        options = new Request.Options();
    }

    @Test
    @DisplayName("실행 전 acquire, 실행 후 응답 헤더로 recalibrate 순서로 호출한다")
    void execute_acquiresBeforeAndRecalibratesAfter() throws Exception {
        Response response = responseWithHeaders(
                Map.of("x-rate-limit-remaining", List.of("599"), "x-rate-limit-reset", List.of("42")));
        when(delegate.execute(request, options)).thenReturn(response);

        Response result = client.execute(request, options);

        assertThat(result).isSameAs(response);
        InOrder order = inOrder(rateLimiter, delegate);
        order.verify(rateLimiter).acquire();
        order.verify(delegate).execute(request, options);
        order.verify(rateLimiter).recalibrate(599, 42);
    }

    @Test
    @DisplayName("헤더가 없으면 null로 recalibrate를 호출한다")
    void execute_passesNullWhenHeadersMissing() throws Exception {
        when(delegate.execute(request, options)).thenReturn(responseWithHeaders(Map.of()));

        client.execute(request, options);

        verify(rateLimiter).recalibrate(null, null);
    }

    @Test
    @DisplayName("헤더 값이 숫자가 아니면 null로 취급한다")
    void execute_treatsNonNumericHeaderAsNull() throws Exception {
        when(delegate.execute(request, options))
                .thenReturn(responseWithHeaders(Map.of("x-rate-limit-remaining", List.of("not-a-number"))));

        client.execute(request, options);

        verify(rateLimiter).recalibrate(null, null);
    }

    private Response responseWithHeaders(Map<String, Collection<String>> headers) {
        return Response.builder()
                .status(200)
                .reason("OK")
                .request(request)
                .headers(headers)
                .build();
    }
}

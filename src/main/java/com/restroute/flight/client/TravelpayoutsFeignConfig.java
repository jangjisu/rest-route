package com.restroute.flight.client;

import feign.Client;
import org.springframework.context.annotation.Bean;

/**
 * {@code @FeignClient(configuration = ...)} 전용 설정. 의도적으로 {@code @Configuration}을
 * 붙이지 않는다 — 붙이면 메인 컴포넌트 스캔에도 걸려서 이 Client 빈이 전역으로 등록되고,
 * travelpayouts 외 다른 Feign 클라이언트에도 rate limiter가 적용돼버린다.
 */
class TravelpayoutsFeignConfig {

    @Bean
    Client travelpayoutsClient(TravelpayoutsRateLimiter rateLimiter) {
        return new TravelpayoutsRateLimitingFeignClient(rateLimiter, new Client.Default(null, null));
    }
}

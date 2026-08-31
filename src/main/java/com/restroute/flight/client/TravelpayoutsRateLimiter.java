package com.restroute.flight.client;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Travelpayouts API 호출 전체가 공유하는 요청 한도 게이트. 서버가 분당 고정 창이 아니라
 * rolling window로 한도를 관리하므로, 클라이언트가 창을 스스로 계산하지 않고 매 응답의
 * x-rate-limit-remaining/x-rate-limit-reset 헤더로 남은 예산을 계속 재보정한다. acquire()는 예산이 바닥이면 재보정될 때까지 블로킹한다 — 가상 스레드라
 * 블로킹 비용이 낮다.
 */
@Slf4j
@Component
public class TravelpayoutsRateLimiter {

    private static final int DEFAULT_LIMIT = 600;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);
    private static final Duration PROBE_INTERVAL = Duration.ofSeconds(2);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);

    private final Clock clock;
    private final AtomicReference<Budget> budget;

    public TravelpayoutsRateLimiter() {
        this(Clock.systemUTC(), DEFAULT_LIMIT, DEFAULT_WINDOW);
    }

    TravelpayoutsRateLimiter(Clock clock, int limit, Duration window) {
        this.clock = clock;
        this.budget = new AtomicReference<>(Budget.initial(clock, limit, window));
    }

    /**
     * 호출 전 반드시 거쳐야 하는 게이트. 예산이 없으면 재보정될 때까지 블로킹한다. 대기는
     * {@link #POLL_INTERVAL} 단위로 짧게 쪼개서, 대기 중에 recalibrate()로 예산이 갱신되면
     * 원래 대기 시간을 다 채우지 않고도 곧바로 반영되게 한다.
     */
    public void acquire() {
        while (true) {
            Budget current = budget.get();
            long waitMillis = current.waitMillis(clock.instant());
            if (waitMillis > 0) {
                sleep(Math.min(waitMillis, POLL_INTERVAL.toMillis()));
                continue;
            }
            if (budget.compareAndSet(current, current.consumeOne(clock.instant()))) {
                return;
            }
        }
    }

    /** 응답 헤더로 남은 예산을 서버 기준으로 재보정한다. 헤더가 없으면 아무것도 하지 않는다. */
    public void recalibrate(Integer remaining, Integer resetAfterSeconds) {
        if (remaining == null || resetAfterSeconds == null) {
            return;
        }
        Instant resetAt = clock.instant().plusSeconds(resetAfterSeconds);
        budget.set(new Budget(remaining, resetAt));
        log.debug("Travelpayouts rate limit recalibrated. remaining={}, resetAt={}", remaining, resetAt);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Travelpayouts 요청 대기 중 인터럽트됐습니다.", e);
        }
    }

    private record Budget(int remaining, Instant resetAt) {

        static Budget initial(Clock clock, int limit, Duration window) {
            return new Budget(limit, clock.instant().plus(window));
        }

        long waitMillis(Instant now) {
            if (remaining > 0) {
                return 0;
            }
            return Math.max(0, Duration.between(now, resetAt).toMillis());
        }

        Budget consumeOne(Instant now) {
            if (remaining > 0) {
                return new Budget(remaining - 1, resetAt);
            }
            // resetAt은 지났는데 아직 재보정을 못 받은 경우 — 한 요청만 흘려보내고
            // 나머지는 짧게 다시 대기시켜서 재보정 전에 한꺼번에 몰리지 않게 한다.
            return new Budget(0, now.plus(PROBE_INTERVAL));
        }
    }
}

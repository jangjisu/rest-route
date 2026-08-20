package com.restroute.flight.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class TravelpayoutsRateLimiterTest {

    @Test
    @DisplayName("예산이 남아있으면 즉시 통과한다")
    @Timeout(1)
    void acquire_passesImmediately_whenBudgetRemains() {
        TravelpayoutsRateLimiter limiter = new TravelpayoutsRateLimiter(Clock.systemUTC(), 2, Duration.ofMinutes(1));

        limiter.acquire();
        limiter.acquire();
    }

    @Test
    @DisplayName("예산이 바닥나면 블로킹하다가, 재보정으로 예산이 생기면 곧바로 풀린다")
    @Timeout(2)
    void acquire_blocksUntilRecalibrated() throws Exception {
        TravelpayoutsRateLimiter limiter = new TravelpayoutsRateLimiter(Clock.systemUTC(), 1, Duration.ofSeconds(10));
        limiter.acquire();

        CompletableFuture<Void> blocked = CompletableFuture.runAsync(limiter::acquire);
        Thread.sleep(300);
        assertThat(blocked).isNotDone();

        limiter.recalibrate(5, 1);

        blocked.get(1, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("recalibrate에 null 헤더 값이 오면 예산을 건드리지 않는다")
    @Timeout(2)
    void recalibrate_ignoresNullValues() throws Exception {
        TravelpayoutsRateLimiter limiter = new TravelpayoutsRateLimiter(Clock.systemUTC(), 1, Duration.ofSeconds(10));
        limiter.acquire();

        limiter.recalibrate(null, null);
        limiter.recalibrate(5, null);
        limiter.recalibrate(null, 1);

        CompletableFuture<Void> stillBlocked = CompletableFuture.runAsync(limiter::acquire);
        Thread.sleep(300);
        assertThat(stillBlocked).isNotDone();

        limiter.recalibrate(1, 1);
        stillBlocked.get(1, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("동시에 여러 스레드가 acquire해도 정확히 한도만큼만 통과한다")
    @Timeout(3)
    void acquire_isConcurrencySafe() throws Exception {
        int limit = 100;
        TravelpayoutsRateLimiter limiter =
                new TravelpayoutsRateLimiter(Clock.systemUTC(), limit, Duration.ofSeconds(10));
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        try {
            AtomicInteger completed = new AtomicInteger();
            CompletableFuture<?>[] withinBudget = IntStream.range(0, limit)
                    .mapToObj(i -> CompletableFuture.runAsync(
                            () -> {
                                limiter.acquire();
                                completed.incrementAndGet();
                            },
                            pool))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(withinBudget).get(1, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(completed).hasValue(limit);

            CompletableFuture<Void> overBudget = CompletableFuture.runAsync(limiter::acquire, pool);
            Thread.sleep(300);
            assertThat(overBudget).isNotDone();

            limiter.recalibrate(1, 1);
            overBudget.get(1, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }
}

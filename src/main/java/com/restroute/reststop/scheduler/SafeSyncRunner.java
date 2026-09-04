package com.restroute.reststop.scheduler;

import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link RestStopScheduler}/{@link RestStopStartupInitializer}가 각자 반복하던
 * try/catch/log 뼈대를 하나로 모은다 — 동기화 하나가 실패해도 다른 동기화를 막지 않는다는
 * 정책(CONTEXT.md의 "관리자 재정의"·"백필" 절 참고)은 그대로 지키고, 어떤 동기화인지와
 * 결과를 어떻게 얻는지만 호출부가 정한다.
 */
@Slf4j
final class SafeSyncRunner {

    private SafeSyncRunner() {}

    static void runScheduled(String label, IntSupplier task) {
        try {
            int savedCount = task.getAsInt();
            log.info("Scheduled {} sync completed. savedCount={}", label, savedCount);
        } catch (RuntimeException e) {
            log.error("Scheduled {} sync failed. cause={}", label, e.getMessage(), e);
        }
    }

    static void runInitial(String label, String tableName, IntSupplier task) {
        try {
            int savedCount = task.getAsInt();
            if (savedCount > 0) {
                log.info("Initial {} sync completed. savedCount={}", label, savedCount);
                return;
            }
            log.info("Initial {} sync skipped because {} table already has data.", label, tableName);
        } catch (RuntimeException e) {
            log.error("Initial {} sync failed. cause={}", label, e.getMessage(), e);
        }
    }

    static void runBackfill(String logPrefix, Supplier<Map<String, Integer>> task) {
        try {
            Map<String, Integer> result = task.get();
            log.info("{} completed. result={}", logPrefix, result);
        } catch (RuntimeException e) {
            log.error("{} failed. cause={}", logPrefix, e.getMessage(), e);
        }
    }
}

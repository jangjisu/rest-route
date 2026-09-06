package com.restroute.flight.scheduler;

import java.util.function.Supplier;

/**
 * 국가/도시/공항/항공사 4종 참조 데이터 재시딩이 SQL 경로·repository 콜백·캐시·로그 라벨만
 * 다르고 나머지 흐름(재시딩 → 성공/실패 로그 → 캐시 refresh)은 동일해서, 그 흐름을
 * {@link FlightReferenceDataStartupInitializer} 하나로 두고 도메인별로 다른 부분만 이 값
 * 타입으로 표현한다.
 */
record ReferenceDataSyncSpec(
        String label,
        String seedSqlPath,
        Runnable clearExisting,
        Supplier<Long> currentCount,
        Runnable refreshCache,
        String enabledPropertyKey) {

    static ReferenceDataSyncSpec of(
            String label,
            String seedSqlPath,
            Runnable clearExisting,
            Supplier<Long> currentCount,
            Runnable refreshCache,
            String enabledPropertyKey) {
        return new ReferenceDataSyncSpec(
                label, seedSqlPath, clearExisting, currentCount, refreshCache, enabledPropertyKey);
    }
}

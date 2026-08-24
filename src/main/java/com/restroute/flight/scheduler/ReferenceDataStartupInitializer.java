package com.restroute.flight.scheduler;

import com.restroute.flight.service.FlightReferenceDataSeeder;
import java.util.function.Supplier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 국가/도시/공항/항공사 4종 참조 데이터의 시작 시 재시딩 + 캐시 채우기 골격. reseed 호출,
 * 실패해도 앱 시작은 막지 않는 처리, 그 뒤 캐시 refresh 순서는 4종 모두 동일하고 SQL
 * 경로·repository 콜백·로그 문구·캐시만 다르다. 로거는 실제 클래스를 가리켜야 해서 로깅
 * 자체는 서브클래스에 남겨두고 여기서는 흐름만 공유한다.
 */
abstract class ReferenceDataStartupInitializer implements ApplicationRunner {

    private final FlightReferenceDataSeeder flightReferenceDataSeeder;
    private final String seedSqlPath;
    private final Runnable clearExisting;
    private final Supplier<Long> currentCount;

    protected ReferenceDataStartupInitializer(
            FlightReferenceDataSeeder flightReferenceDataSeeder,
            String seedSqlPath,
            Runnable clearExisting,
            Supplier<Long> currentCount) {
        this.flightReferenceDataSeeder = flightReferenceDataSeeder;
        this.seedSqlPath = seedSqlPath;
        this.clearExisting = clearExisting;
        this.currentCount = currentCount;
    }

    @Override
    public final void run(ApplicationArguments args) {
        try {
            int savedCount = flightReferenceDataSeeder.reseed(seedSqlPath, clearExisting, currentCount);
            onSeedSuccess(savedCount);
        } catch (RuntimeException e) {
            onSeedFailure(e);
        }
        refreshCache();
    }

    protected abstract void onSeedSuccess(int savedCount);

    protected abstract void onSeedFailure(RuntimeException e);

    protected abstract void refreshCache();
}

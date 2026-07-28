package com.restroute.scheduler;

import com.restroute.service.HighwayServiceAreaInfoSyncService;
import com.restroute.service.RestEventSyncService;
import com.restroute.service.RestFoodSyncService;
import com.restroute.service.RestOilPriceSyncService;
import com.restroute.service.RestOilSyncService;
import com.restroute.service.RestStopDetailSyncService;
import com.restroute.service.RestStopServiceAreaCodeBackfillService;
import com.restroute.service.RestStopSyncService;
import com.restroute.service.RestThemeSyncService;
import com.restroute.service.evcharger.EvChargerSyncResult;
import com.restroute.service.evcharger.EvChargerSyncService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestStopScheduler {

    private final RestStopSyncService restStopSyncService;
    private final RestStopDetailSyncService restStopDetailSyncService;
    private final HighwayServiceAreaInfoSyncService highwayServiceAreaInfoSyncService;
    private final RestOilSyncService restOilSyncService;
    private final RestOilPriceSyncService restOilPriceSyncService;
    private final RestFoodSyncService restFoodSyncService;
    private final RestThemeSyncService restThemeSyncService;
    private final RestEventSyncService restEventSyncService;
    private final RestStopServiceAreaCodeBackfillService restStopServiceAreaCodeBackfillService;
    private final EvChargerSyncService evChargerSyncService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void syncRestStopsDaily() {
        refreshRestStops();
        refreshRestStopDetails();
        refreshHighwayServiceAreaInfos();
        refreshRestOils();
        refreshRestFoods();
        refreshRestThemes();
        refreshRestEvents();
        refreshEvChargers();
        backfillRestStopServiceAreaCodes();
    }

    @Scheduled(cron = "0 0 */3 * * *", zone = "Asia/Seoul")
    public void syncRestOilPricesEveryThreeHours() {
        refreshRestOilPrices();
        backfillRestStopServiceAreaCodes();
    }

    private void refreshRestStops() {
        try {
            int savedCount = restStopSyncService.refreshRestStops();
            log.info("Scheduled rest stop sync completed. savedCount={}", savedCount);
        } catch (RuntimeException e) {
            log.error("Scheduled rest stop sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void refreshRestStopDetails() {
        try {
            int savedCount = restStopDetailSyncService.refreshRestStopDetails();
            log.info("Scheduled rest stop detail sync completed. savedCount={}", savedCount);
        } catch (RuntimeException e) {
            log.error("Scheduled rest stop detail sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void refreshHighwayServiceAreaInfos() {
        try {
            int savedCount = highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos();
            log.info("Scheduled highway service area info sync completed. savedCount={}", savedCount);
        } catch (RuntimeException e) {
            log.error("Scheduled highway service area info sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void refreshRestOils() {
        try {
            int savedCount = restOilSyncService.refreshRestOils();
            log.info("Scheduled rest oil sync completed. savedCount={}", savedCount);
        } catch (RuntimeException e) {
            log.error("Scheduled rest oil sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void refreshRestOilPrices() {
        try {
            int savedCount = restOilPriceSyncService.refreshRestOilPrices();
            log.info("Scheduled rest oil price sync completed. savedCount={}", savedCount);
        } catch (RuntimeException e) {
            log.error("Scheduled rest oil price sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void refreshRestFoods() {
        try {
            int savedCount = restFoodSyncService.refreshRestFoods();
            log.info("Scheduled rest food sync completed. savedCount={}", savedCount);
        } catch (RuntimeException e) {
            log.error("Scheduled rest food sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void refreshRestThemes() {
        try {
            int savedCount = restThemeSyncService.refreshRestThemes();
            log.info("Scheduled rest theme sync completed. savedCount={}", savedCount);
        } catch (RuntimeException e) {
            log.error("Scheduled rest theme sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void refreshRestEvents() {
        try {
            int savedCount = restEventSyncService.refreshRestEvents();
            log.info("Scheduled rest event sync completed. savedCount={}", savedCount);
        } catch (RuntimeException e) {
            log.error("Scheduled rest event sync failed. cause={}", e.getMessage(), e);
        }
    }

    private EvChargerSyncResult refreshEvChargers() {
        try {
            EvChargerSyncResult result = evChargerSyncService.refreshEvChargers();
            log.info("Scheduled EV charger sync completed. result={}", result);
            return result;
        } catch (RuntimeException e) {
            log.error("Scheduled EV charger sync failed. cause={}", e.getMessage(), e);
            return EvChargerSyncResult.failed();
        }
    }

    private void backfillRestStopServiceAreaCodes() {
        try {
            Map<String, Integer> result = restStopServiceAreaCodeBackfillService.backfill();
            log.info("Scheduled rest stop service area code backfill completed. result={}", result);
        } catch (RuntimeException e) {
            log.error("Scheduled rest stop service area code backfill failed. cause={}", e.getMessage(), e);
        }
    }
}

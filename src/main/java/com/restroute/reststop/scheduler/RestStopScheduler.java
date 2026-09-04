package com.restroute.reststop.scheduler;

import com.restroute.evcharger.service.EvChargerSyncService;
import com.restroute.evcharger.service.dto.EvChargerSyncResult;
import com.restroute.oilprice.service.RestOilPriceSyncService;
import com.restroute.oilprice.service.RestOilSyncService;
import com.restroute.reststop.service.HighwayServiceAreaInfoSyncService;
import com.restroute.reststop.service.RestStopDetailSyncService;
import com.restroute.reststop.service.RestStopServiceAreaCodeBackfillService;
import com.restroute.reststop.service.RestStopSyncService;
import com.restroute.reststopcontent.service.RestEventSyncService;
import com.restroute.reststopcontent.service.RestFoodSyncService;
import com.restroute.reststopcontent.service.RestThemeSyncService;
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
        SafeSyncRunner.runScheduled("rest stop", restStopSyncService::refreshRestStops);
        SafeSyncRunner.runScheduled("rest stop detail", restStopDetailSyncService::refreshRestStopDetails);
        SafeSyncRunner.runScheduled(
                "highway service area info", highwayServiceAreaInfoSyncService::refreshHighwayServiceAreaInfos);
        SafeSyncRunner.runScheduled("rest oil", restOilSyncService::refreshRestOils);
        SafeSyncRunner.runScheduled("rest food", restFoodSyncService::refreshRestFoods);
        SafeSyncRunner.runScheduled("rest theme", restThemeSyncService::refreshRestThemes);
        SafeSyncRunner.runScheduled("rest event", restEventSyncService::refreshRestEvents);
        refreshEvChargers();
        backfillRestStopServiceAreaCodes();
    }

    @Scheduled(cron = "0 0 */3 * * *", zone = "Asia/Seoul")
    public void syncRestOilPricesEveryThreeHours() {
        SafeSyncRunner.runScheduled("rest oil price", restOilPriceSyncService::refreshRestOilPrices);
        backfillRestStopServiceAreaCodes();
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
        SafeSyncRunner.runBackfill(
                "Scheduled rest stop service area code backfill", restStopServiceAreaCodeBackfillService::backfill);
    }
}

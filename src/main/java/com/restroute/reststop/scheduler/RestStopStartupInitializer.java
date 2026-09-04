package com.restroute.reststop.scheduler;

import com.restroute.evcharger.service.EvChargerSyncService;
import com.restroute.evcharger.service.dto.EvChargerSyncResult;
import com.restroute.oilprice.service.RestOilPriceSyncService;
import com.restroute.oilprice.service.RestOilSyncService;
import com.restroute.reststop.service.RestStopDetailSyncService;
import com.restroute.reststop.service.RestStopServiceAreaCodeBackfillService;
import com.restroute.reststop.service.RestStopSyncService;
import com.restroute.reststopcontent.service.RestEventSyncService;
import com.restroute.reststopcontent.service.RestFoodSyncService;
import com.restroute.reststopcontent.service.RestThemeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rest-stop.sync", name = "startup-enabled", havingValue = "true", matchIfMissing = true)
public class RestStopStartupInitializer implements ApplicationRunner {

    private final RestStopSyncService restStopSyncService;
    private final RestStopDetailSyncService restStopDetailSyncService;
    private final RestOilSyncService restOilSyncService;
    private final RestOilPriceSyncService restOilPriceSyncService;
    private final RestFoodSyncService restFoodSyncService;
    private final RestStopServiceAreaCodeBackfillService restStopServiceAreaCodeBackfillService;
    private final EvChargerSyncService evChargerSyncService;
    private final RestThemeSyncService restThemeSyncService;
    private final RestEventSyncService restEventSyncService;

    @Override
    public void run(ApplicationArguments args) {
        SafeSyncRunner.runInitial("rest stop", "rest_stop", restStopSyncService::initializeRestStopsIfEmpty);
        SafeSyncRunner.runInitial(
                "rest stop detail", "rest_stop_detail", restStopDetailSyncService::initializeRestStopDetailsIfEmpty);
        SafeSyncRunner.runInitial("rest oil", "rest_oil", restOilSyncService::initializeRestOilsIfEmpty);
        SafeSyncRunner.runInitial(
                "rest oil price", "rest_oil_price", restOilPriceSyncService::initializeRestOilPricesIfEmpty);
        SafeSyncRunner.runInitial("rest food", "rest_food", restFoodSyncService::initializeRestFoodsIfEmpty);
        initializeEvChargers();
        SafeSyncRunner.runInitial("rest theme", "rest_theme", restThemeSyncService::initializeRestThemesIfEmpty);
        SafeSyncRunner.runInitial("rest event", "rest_event", restEventSyncService::initializeRestEventsIfEmpty);
        backfillRestStopServiceAreaCodes();
    }

    private EvChargerSyncResult initializeEvChargers() {
        try {
            EvChargerSyncResult result = evChargerSyncService.initializeEvChargersIfEmpty();
            if (result == null) {
                return EvChargerSyncResult.skipped();
            }
            if (result.savedItemCount() > 0) {
                log.info("Initial EV charger sync completed. result={}", result);
                return result;
            }

            log.info("Initial EV charger sync skipped because ev_charger table already has data.");
            return result;
        } catch (RuntimeException e) {
            log.error("Initial EV charger sync failed. cause={}", e.getMessage(), e);
            return EvChargerSyncResult.failed();
        }
    }

    private void backfillRestStopServiceAreaCodes() {
        SafeSyncRunner.runBackfill(
                "Rest stop service area code backfill", restStopServiceAreaCodeBackfillService::backfill);
    }
}

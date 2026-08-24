package com.restroute.scheduler;

import com.restroute.oilprice.service.RestOilPriceSyncService;
import com.restroute.oilprice.service.RestOilSyncService;
import com.restroute.service.RestEventSyncService;
import com.restroute.service.RestFoodSyncService;
import com.restroute.service.RestStopDetailSyncService;
import com.restroute.service.RestStopServiceAreaCodeBackfillService;
import com.restroute.service.RestStopSyncService;
import com.restroute.service.RestThemeSyncService;
import com.restroute.service.evcharger.EvChargerSyncService;
import com.restroute.service.evcharger.dto.EvChargerSyncResult;
import java.util.Map;
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
        initializeRestStops();
        initializeRestStopDetails();
        initializeRestOils();
        initializeRestOilPrices();
        initializeRestFoods();
        initializeEvChargers();
        initializeRestThemes();
        initializeRestEvents();
        backfillRestStopServiceAreaCodes();
    }

    private void initializeRestStops() {
        try {
            int savedCount = restStopSyncService.initializeRestStopsIfEmpty();
            if (savedCount > 0) {
                log.info("Initial rest stop sync completed. savedCount={}", savedCount);
                return;
            }

            log.info("Initial rest stop sync skipped because rest_stop table already has data.");
        } catch (RuntimeException e) {
            log.error("Initial rest stop sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void initializeRestStopDetails() {
        try {
            int detailSavedCount = restStopDetailSyncService.initializeRestStopDetailsIfEmpty();
            if (detailSavedCount > 0) {
                log.info("Initial rest stop detail sync completed. detailSavedCount={}", detailSavedCount);
                return;
            }

            log.info("Initial rest stop detail sync skipped because rest_stop_detail table already has data.");
        } catch (RuntimeException e) {
            log.error("Initial rest stop detail sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void initializeRestOils() {
        try {
            int savedCount = restOilSyncService.initializeRestOilsIfEmpty();
            if (savedCount > 0) {
                log.info("Initial rest oil sync completed. savedCount={}", savedCount);
                return;
            }

            log.info("Initial rest oil sync skipped because rest_oil table already has data.");
        } catch (RuntimeException e) {
            log.error("Initial rest oil sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void initializeRestOilPrices() {
        try {
            int savedCount = restOilPriceSyncService.initializeRestOilPricesIfEmpty();
            if (savedCount > 0) {
                log.info("Initial rest oil price sync completed. savedCount={}", savedCount);
                return;
            }

            log.info("Initial rest oil price sync skipped because rest_oil_price table already has data.");
        } catch (RuntimeException e) {
            log.error("Initial rest oil price sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void initializeRestFoods() {
        try {
            int savedCount = restFoodSyncService.initializeRestFoodsIfEmpty();
            if (savedCount > 0) {
                log.info("Initial rest food sync completed. savedCount={}", savedCount);
                return;
            }

            log.info("Initial rest food sync skipped because rest_food table already has data.");
        } catch (RuntimeException e) {
            log.error("Initial rest food sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void initializeRestThemes() {
        try {
            int savedCount = restThemeSyncService.initializeRestThemesIfEmpty();
            if (savedCount > 0) {
                log.info("Initial rest theme sync completed. savedCount={}", savedCount);
                return;
            }

            log.info("Initial rest theme sync skipped because rest_theme table already has data.");
        } catch (RuntimeException e) {
            log.error("Initial rest theme sync failed. cause={}", e.getMessage(), e);
        }
    }

    private void initializeRestEvents() {
        try {
            int savedCount = restEventSyncService.initializeRestEventsIfEmpty();
            if (savedCount > 0) {
                log.info("Initial rest event sync completed. savedCount={}", savedCount);
                return;
            }

            log.info("Initial rest event sync skipped because rest_event table already has data.");
        } catch (RuntimeException e) {
            log.error("Initial rest event sync failed. cause={}", e.getMessage(), e);
        }
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
        try {
            Map<String, Integer> result = restStopServiceAreaCodeBackfillService.backfill();
            log.info("Rest stop service area code backfill completed. result={}", result);
        } catch (RuntimeException e) {
            log.error("Rest stop service area code backfill failed. cause={}", e.getMessage(), e);
        }
    }
}

package com.restroute.service;

import com.restroute.domain.EvChargerStationMappingEntity;
import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.EvChargerRepository;
import com.restroute.repository.EvChargerStationMappingRepository;
import com.restroute.repository.RestOilRepository;
import com.restroute.repository.RestStopDetailRepository;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.backfill.HighwayServiceAreaInfoServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestEventServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestFoodServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestOilPriceServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestOilServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestStopDetailServiceAreaCodeBackfiller;
import com.restroute.service.backfill.RestStopProductSalesRankBackfiller;
import com.restroute.service.backfill.RestStopStoreSalesRankBackfiller;
import com.restroute.service.backfill.RestThemeServiceAreaCodeBackfiller;
import com.restroute.service.evcharger.mapping.EvChargerStationMappingCalculator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestStopServiceAreaCodeBackfillService {

    public static final String REST_STOP_DETAIL_MAPPED_COUNT = "restStopDetailMappedCount";
    public static final String HIGHWAY_SERVICE_AREA_INFO_MAPPED_COUNT = "highwayServiceAreaInfoMappedCount";
    public static final String REST_FOOD_MAPPED_COUNT = "restFoodMappedCount";
    public static final String REST_OIL_MAPPED_COUNT = "restOilMappedCount";
    public static final String REST_OIL_PRICE_MAPPED_COUNT = "restOilPriceMappedCount";
    public static final String EV_CHARGER_MAPPED_COUNT = "evChargerMappedCount";
    public static final String PRODUCT_SALES_RANK_MAPPED_COUNT = "productSalesRankMappedCount";
    public static final String STORE_SALES_RANK_MAPPED_COUNT = "storeSalesRankMappedCount";
    public static final String REST_THEME_MAPPED_COUNT = "restThemeMappedCount";
    public static final String REST_EVENT_MAPPED_COUNT = "restEventMappedCount";

    private final RestStopRepository restStopRepository;
    private final RestStopDetailRepository restStopDetailRepository;
    private final RestOilRepository restOilRepository;
    private final EvChargerRepository evChargerRepository;
    private final EvChargerStationMappingRepository evChargerStationMappingRepository;
    private final EvChargerStationMappingCalculator evChargerStationMappingCalculator;
    private final RestStopDetailServiceAreaCodeBackfiller restStopDetailBackfiller;
    private final HighwayServiceAreaInfoServiceAreaCodeBackfiller highwayServiceAreaInfoBackfiller;
    private final RestFoodServiceAreaCodeBackfiller restFoodBackfiller;
    private final RestOilServiceAreaCodeBackfiller restOilBackfiller;
    private final RestOilPriceServiceAreaCodeBackfiller restOilPriceBackfiller;
    private final RestStopProductSalesRankBackfiller productSalesRankBackfiller;
    private final RestStopStoreSalesRankBackfiller storeSalesRankBackfiller;
    private final RestThemeServiceAreaCodeBackfiller restThemeBackfiller;
    private final RestEventServiceAreaCodeBackfiller restEventBackfiller;

    @Transactional
    public Map<String, Integer> backfill() {
        List<RestStopEntity> restStops = restStopRepository.findAll();
        List<String> restStopServiceAreaCodes = findRestStopServiceAreaCodes(restStops);
        Map<String, String> serviceAreaCodeByStdRestCd = mapByStdRestCd(restStops);
        Map<String, String> serviceAreaCodeByOilKey = mapByOilKey(restStops);

        int restStopDetailMappedCount = restStopDetailBackfiller.backfill(restStopServiceAreaCodes);
        int highwayServiceAreaInfoMappedCount = highwayServiceAreaInfoBackfiller.backfill(restStopServiceAreaCodes);
        int restFoodMappedCount = restFoodBackfiller.backfill(serviceAreaCodeByStdRestCd);
        int restOilMappedCount = restOilBackfiller.backfill(serviceAreaCodeByOilKey);
        int restOilPriceMappedCount = restOilPriceBackfiller.backfill(mapByOilStandardRestCode());
        int evChargerMappedCount = backfillEvChargerMappings(restStops);
        int productSalesRankMappedCount = productSalesRankBackfiller.backfill(restStops);
        int storeSalesRankMappedCount = storeSalesRankBackfiller.backfill(restStops);
        int restThemeMappedCount = restThemeBackfiller.backfill(serviceAreaCodeByStdRestCd);
        int restEventMappedCount = restEventBackfiller.backfill(serviceAreaCodeByStdRestCd);

        Map<String, Integer> result = Map.of(
                REST_STOP_DETAIL_MAPPED_COUNT,
                restStopDetailMappedCount,
                HIGHWAY_SERVICE_AREA_INFO_MAPPED_COUNT,
                highwayServiceAreaInfoMappedCount,
                REST_FOOD_MAPPED_COUNT,
                restFoodMappedCount,
                REST_OIL_MAPPED_COUNT,
                restOilMappedCount,
                REST_OIL_PRICE_MAPPED_COUNT,
                restOilPriceMappedCount,
                EV_CHARGER_MAPPED_COUNT,
                evChargerMappedCount,
                PRODUCT_SALES_RANK_MAPPED_COUNT,
                productSalesRankMappedCount,
                STORE_SALES_RANK_MAPPED_COUNT,
                storeSalesRankMappedCount,
                REST_THEME_MAPPED_COUNT,
                restThemeMappedCount,
                REST_EVENT_MAPPED_COUNT,
                restEventMappedCount);
        log.info(
                "Rest stop service area code backfill completed. restStopDetailMappedCount={}, "
                        + "highwayServiceAreaInfoMappedCount={}, restFoodMappedCount={}, restOilMappedCount={}, "
                        + "restOilPriceMappedCount={}, evChargerMappedCount={}, productSalesRankMappedCount={}, "
                        + "storeSalesRankMappedCount={}, restThemeMappedCount={}, restEventMappedCount={}",
                result.get(REST_STOP_DETAIL_MAPPED_COUNT),
                result.get(HIGHWAY_SERVICE_AREA_INFO_MAPPED_COUNT),
                result.get(REST_FOOD_MAPPED_COUNT),
                result.get(REST_OIL_MAPPED_COUNT),
                result.get(REST_OIL_PRICE_MAPPED_COUNT),
                result.get(EV_CHARGER_MAPPED_COUNT),
                result.get(PRODUCT_SALES_RANK_MAPPED_COUNT),
                result.get(STORE_SALES_RANK_MAPPED_COUNT),
                result.get(REST_THEME_MAPPED_COUNT),
                result.get(REST_EVENT_MAPPED_COUNT));
        return result;
    }

    private int backfillEvChargerMappings(List<RestStopEntity> restStops) {
        List<EvChargerStationMappingEntity> mappingsToSave = evChargerStationMappingCalculator.calculate(
                restStops, restStopDetailRepository.findAll(), evChargerRepository.findAllByDelYn("N"));
        evChargerStationMappingRepository.deleteAllInBatch();
        evChargerStationMappingRepository.saveAll(mappingsToSave);
        return mappingsToSave.size();
    }

    private List<String> findRestStopServiceAreaCodes(List<RestStopEntity> restStops) {
        return restStops.stream()
                .map(RestStopEntity::getServiceAreaCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private Map<String, String> mapByStdRestCd(List<RestStopEntity> restStops) {
        return restStops.stream()
                .filter(restStop -> StringUtils.hasText(restStop.getStdRestCd()))
                .filter(restStop -> StringUtils.hasText(restStop.getServiceAreaCode()))
                .collect(Collectors.toMap(
                        RestStopEntity::getStdRestCd, RestStopEntity::getServiceAreaCode, (first, second) -> first));
    }

    private Map<String, String> mapByOilKey(List<RestStopEntity> restStops) {
        return restStops.stream()
                .filter(restStop -> StringUtils.hasText(restStop.getRouteNo()))
                .filter(restStop -> StringUtils.hasText(restStop.getUnitName()))
                .filter(restStop -> StringUtils.hasText(restStop.getServiceAreaCode()))
                .collect(Collectors.toMap(
                        restStop -> oilRestStopKey(
                                restStop.getRouteNo(), RestOilEntity.normalizeStationName(restStop.getUnitName())),
                        RestStopEntity::getServiceAreaCode,
                        (first, second) -> first));
    }

    private Map<String, String> mapByOilStandardRestCode() {
        return restOilRepository.findAll().stream()
                .filter(restOil -> StringUtils.hasText(restOil.getStandardRestCode()))
                .filter(restOil -> StringUtils.hasText(restOil.getRestStopServiceAreaCode()))
                .collect(Collectors.toMap(
                        RestOilEntity::getStandardRestCode,
                        RestOilEntity::getRestStopServiceAreaCode,
                        (first, second) -> first));
    }

    private String oilRestStopKey(String routeCode, String normalizedStationName) {
        return routeCode + "\n" + normalizedStationName;
    }
}

package com.restroute.reststop.service;

import com.restroute.evcharger.service.EvChargerStationMappingBackfiller;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.service.RestOilPriceServiceAreaCodeBackfiller;
import com.restroute.oilprice.service.RestOilServiceAreaCodeBackfiller;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.backfill.HighwayServiceAreaInfoServiceAreaCodeBackfiller;
import com.restroute.reststop.service.backfill.RestStopDetailServiceAreaCodeBackfiller;
import com.restroute.reststop.service.backfill.RestStopProductSalesRankBackfiller;
import com.restroute.reststop.service.backfill.RestStopStoreSalesRankBackfiller;
import com.restroute.reststopcontent.service.RestEventServiceAreaCodeBackfiller;
import com.restroute.reststopcontent.service.RestFoodServiceAreaCodeBackfiller;
import com.restroute.reststopcontent.service.RestThemeServiceAreaCodeBackfiller;
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

    private final RestStopQueryService restStopQueryService;
    private final RestStopDetailServiceAreaCodeBackfiller restStopDetailBackfiller;
    private final HighwayServiceAreaInfoServiceAreaCodeBackfiller highwayServiceAreaInfoBackfiller;
    private final RestFoodServiceAreaCodeBackfiller restFoodBackfiller;
    private final RestOilServiceAreaCodeBackfiller restOilBackfiller;
    private final RestOilPriceServiceAreaCodeBackfiller restOilPriceBackfiller;
    private final EvChargerStationMappingBackfiller evChargerStationMappingBackfiller;
    private final RestStopProductSalesRankBackfiller productSalesRankBackfiller;
    private final RestStopStoreSalesRankBackfiller storeSalesRankBackfiller;
    private final RestThemeServiceAreaCodeBackfiller restThemeBackfiller;
    private final RestEventServiceAreaCodeBackfiller restEventBackfiller;

    @Transactional
    public Map<String, Integer> backfill() {
        List<RestStopEntity> restStops = restStopQueryService.findAll();
        List<String> restStopServiceAreaCodes = findRestStopServiceAreaCodes(restStops);
        Map<String, String> serviceAreaCodeByStdRestCd = mapByStdRestCd(restStops);
        Map<String, String> serviceAreaCodeByOilKey = mapByOilKey(restStops);

        int restStopDetailMappedCount = restStopDetailBackfiller.backfill(restStopServiceAreaCodes);
        int highwayServiceAreaInfoMappedCount = highwayServiceAreaInfoBackfiller.backfill(restStopServiceAreaCodes);
        int restFoodMappedCount = restFoodBackfiller.backfill(serviceAreaCodeByStdRestCd);
        int restOilMappedCount = restOilBackfiller.backfill(serviceAreaCodeByOilKey);
        int restOilPriceMappedCount = restOilPriceBackfiller.backfill();
        int evChargerMappedCount = evChargerStationMappingBackfiller.backfill(restStops);
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

    private String oilRestStopKey(String routeCode, String normalizedStationName) {
        return routeCode + "\n" + normalizedStationName;
    }
}

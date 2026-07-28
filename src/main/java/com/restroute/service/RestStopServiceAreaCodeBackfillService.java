package com.restroute.service;

import com.restroute.domain.EvChargerStationMappingEntity;
import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.domain.RestEventEntity;
import com.restroute.domain.RestFoodEntity;
import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.domain.RestStopProductSalesRankEntity;
import com.restroute.domain.RestStopStoreSalesRankEntity;
import com.restroute.domain.RestThemeEntity;
import com.restroute.repository.EvChargerRepository;
import com.restroute.repository.EvChargerStationMappingRepository;
import com.restroute.repository.HighwayServiceAreaInfoRepository;
import com.restroute.repository.RestEventRepository;
import com.restroute.repository.RestFoodRepository;
import com.restroute.repository.RestOilPriceRepository;
import com.restroute.repository.RestOilRepository;
import com.restroute.repository.RestStopDetailRepository;
import com.restroute.repository.RestStopProductSalesRankRepository;
import com.restroute.repository.RestStopRepository;
import com.restroute.repository.RestStopStoreSalesRankRepository;
import com.restroute.repository.RestThemeRepository;
import com.restroute.service.evcharger.mapping.EvChargerStationMappingCalculator;
import com.restroute.service.salesranking.SalesRankingRestStopNameNormalizer;
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
    private final HighwayServiceAreaInfoRepository highwayServiceAreaInfoRepository;
    private final RestFoodRepository restFoodRepository;
    private final RestOilRepository restOilRepository;
    private final RestOilPriceRepository restOilPriceRepository;
    private final EvChargerRepository evChargerRepository;
    private final EvChargerStationMappingRepository evChargerStationMappingRepository;
    private final EvChargerStationMappingCalculator evChargerStationMappingCalculator;
    private final RestStopProductSalesRankRepository productSalesRankRepository;
    private final RestStopStoreSalesRankRepository storeSalesRankRepository;
    private final RestThemeRepository restThemeRepository;
    private final RestEventRepository restEventRepository;

    @Transactional
    public Map<String, Integer> backfill() {
        List<RestStopEntity> restStops = restStopRepository.findAll();
        List<String> restStopServiceAreaCodes = findRestStopServiceAreaCodes(restStops);
        Map<String, String> serviceAreaCodeByStdRestCd = mapByStdRestCd(restStops);
        Map<String, String> serviceAreaCodeByOilKey = mapByOilKey(restStops);

        int restStopDetailMappedCount = backfillRestStopDetails(restStopServiceAreaCodes);
        int highwayServiceAreaInfoMappedCount = backfillHighwayServiceAreaInfos(restStopServiceAreaCodes);
        int restFoodMappedCount = backfillRestFoods(serviceAreaCodeByStdRestCd);
        int restOilMappedCount = backfillRestOils(serviceAreaCodeByOilKey);
        int restOilPriceMappedCount = backfillRestOilPrices(mapByOilStandardRestCode());
        int evChargerMappedCount = backfillEvChargerMappings(restStops);
        int productSalesRankMappedCount = backfillProductSalesRanks(restStops);
        int storeSalesRankMappedCount = backfillStoreSalesRanks(restStops);
        int restThemeMappedCount = backfillRestThemes(serviceAreaCodeByStdRestCd);
        int restEventMappedCount = backfillRestEvents(serviceAreaCodeByStdRestCd);

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

    private int backfillRestStopDetails(List<String> restStopServiceAreaCodes) {
        int mappedCount = 0;
        for (RestStopDetailEntity detail : restStopDetailRepository.findAll()) {
            String restStopServiceAreaCode =
                    findMatchingServiceAreaCode(detail.getServiceAreaCode(), restStopServiceAreaCodes);
            detail.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }

    private int backfillHighwayServiceAreaInfos(List<String> restStopServiceAreaCodes) {
        int mappedCount = 0;
        for (HighwayServiceAreaInfoEntity info : highwayServiceAreaInfoRepository.findAll()) {
            String restStopServiceAreaCode =
                    findMatchingServiceAreaCode(info.getBusinessFacilityCode(), restStopServiceAreaCodes);
            info.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }

    private String findMatchingServiceAreaCode(String sourceCode, List<String> restStopServiceAreaCodes) {
        return restStopServiceAreaCodes.contains(sourceCode) ? sourceCode : null;
    }

    private int backfillRestFoods(Map<String, String> serviceAreaCodeByStdRestCd) {
        int mappedCount = 0;
        for (RestFoodEntity food : restFoodRepository.findAll()) {
            String restStopServiceAreaCode = serviceAreaCodeByStdRestCd.get(food.getStdRestCd());
            food.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }

    private int backfillRestThemes(Map<String, String> serviceAreaCodeByStdRestCd) {
        int mappedCount = 0;
        for (RestThemeEntity theme : restThemeRepository.findAll()) {
            String restStopServiceAreaCode = serviceAreaCodeByStdRestCd.get(theme.getStdRestCd());
            theme.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }

    private int backfillRestEvents(Map<String, String> serviceAreaCodeByStdRestCd) {
        int mappedCount = 0;
        for (RestEventEntity event : restEventRepository.findAll()) {
            String restStopServiceAreaCode = serviceAreaCodeByStdRestCd.get(event.getStdRestCd());
            event.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }

    private int backfillRestOils(Map<String, String> serviceAreaCodeByOilKey) {
        int mappedCount = 0;
        for (RestOilEntity oil : restOilRepository.findAll()) {
            if (oil.isAdminOverridden()) {
                continue;
            }
            String key = oilRestStopKey(oil.getRouteCode(), oil.getNormalizedStationName());
            String restStopServiceAreaCode = serviceAreaCodeByOilKey.get(key);
            oil.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }

    private int backfillRestOilPrices(Map<String, String> serviceAreaCodeByOilStandardRestCode) {
        int mappedCount = 0;
        for (RestOilPriceEntity oilPrice : restOilPriceRepository.findAll()) {
            if (oilPrice.isAdminOverridden()) {
                continue;
            }
            String restStopServiceAreaCode = serviceAreaCodeByOilStandardRestCode.get(oilPrice.getServiceAreaCode2());
            oilPrice.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }

    private int backfillProductSalesRanks(List<RestStopEntity> restStops) {
        int mappedCount = 0;
        for (RestStopProductSalesRankEntity rank : productSalesRankRepository.findAll()) {
            if (!rank.isUnmapped()) {
                continue;
            }
            String serviceAreaCode = findUniqueServiceAreaCode(restStops, rank.getSourceRestStopName());
            if (serviceAreaCode == null) {
                continue;
            }
            rank.updateRestStopServiceAreaCode(serviceAreaCode);
            mappedCount++;
        }
        return mappedCount;
    }

    private int backfillStoreSalesRanks(List<RestStopEntity> restStops) {
        int mappedCount = 0;
        for (RestStopStoreSalesRankEntity rank : storeSalesRankRepository.findAll()) {
            if (!rank.isUnmapped()) {
                continue;
            }
            String serviceAreaCode = findUniqueServiceAreaCode(restStops, rank.getSourceRestStopName());
            if (serviceAreaCode == null) {
                continue;
            }
            rank.updateRestStopServiceAreaCode(serviceAreaCode);
            mappedCount++;
        }
        return mappedCount;
    }

    private String findUniqueServiceAreaCode(List<RestStopEntity> restStops, String name) {
        List<String> serviceAreaCodes = restStops.stream()
                .filter(restStop -> StringUtils.hasText(restStop.getUnitName()))
                .filter(restStop -> StringUtils.hasText(restStop.getServiceAreaCode()))
                .filter(restStop -> SalesRankingRestStopNameNormalizer.normalize(restStop.getUnitName())
                        .equals(SalesRankingRestStopNameNormalizer.normalize(name)))
                .map(RestStopEntity::getServiceAreaCode)
                .distinct()
                .toList();
        if (serviceAreaCodes.size() != 1) {
            return null;
        }
        return serviceAreaCodes.get(0);
    }

    private String oilRestStopKey(String routeCode, String normalizedStationName) {
        return routeCode + "\n" + normalizedStationName;
    }
}

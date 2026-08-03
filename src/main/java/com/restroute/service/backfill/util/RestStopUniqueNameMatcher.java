package com.restroute.service.backfill.util;

import com.restroute.domain.RestStopEntity;
import com.restroute.service.salesranking.util.SalesRankingRestStopNameNormalizer;
import java.util.List;
import org.springframework.util.StringUtils;

public final class RestStopUniqueNameMatcher {

    private RestStopUniqueNameMatcher() {}

    public static String findUniqueServiceAreaCode(List<RestStopEntity> restStops, String name) {
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
}

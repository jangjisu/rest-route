package com.restroute.oilprice.service;

import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.repository.RestOilRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestOilServiceAreaCodeBackfiller {

    private final RestOilRepository restOilRepository;

    public int backfill(Map<String, String> serviceAreaCodeByOilKey) {
        int mappedCount = 0;
        for (RestOilEntity oil : restOilRepository.findByRestStopServiceAreaCodesAndAdminOverridden(null, false)) {
            String key = oilRestStopKey(oil.getRouteCode(), oil.getNormalizedStationName());
            String restStopServiceAreaCode = serviceAreaCodeByOilKey.get(key);
            oil.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }

    private String oilRestStopKey(String routeCode, String normalizedStationName) {
        return routeCode + "\n" + normalizedStationName;
    }
}

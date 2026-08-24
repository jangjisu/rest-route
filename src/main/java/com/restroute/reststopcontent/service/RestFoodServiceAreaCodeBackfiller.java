package com.restroute.reststopcontent.service;

import com.restroute.reststopcontent.domain.RestFoodEntity;
import com.restroute.reststopcontent.repository.RestFoodRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestFoodServiceAreaCodeBackfiller {

    private final RestFoodRepository restFoodRepository;

    public int backfill(Map<String, String> serviceAreaCodeByStdRestCd) {
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
}

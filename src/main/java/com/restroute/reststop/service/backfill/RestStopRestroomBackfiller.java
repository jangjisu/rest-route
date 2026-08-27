package com.restroute.reststop.service.backfill;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopRestroomEntity;
import com.restroute.reststop.repository.RestStopRestroomRepository;
import com.restroute.reststop.service.backfill.util.RestStopUniqueNameMatcher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestStopRestroomBackfiller {

    private final RestStopRestroomRepository restroomRepository;

    public int backfill(List<RestStopEntity> restStops) {
        int mappedCount = 0;
        for (RestStopRestroomEntity restroom : restroomRepository.findAll()) {
            String serviceAreaCode = matchServiceAreaCode(restroom, restStops);
            if (serviceAreaCode == null) {
                continue;
            }
            restroom.updateRestStopServiceAreaCode(serviceAreaCode);
            mappedCount++;
        }
        return mappedCount;
    }

    private String matchServiceAreaCode(RestStopRestroomEntity restroom, List<RestStopEntity> restStops) {
        if (restroom.isMapped()) {
            return null;
        }
        return RestStopUniqueNameMatcher.findUniqueServiceAreaCode(restStops, restroom.getSourceRestStopName());
    }
}

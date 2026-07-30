package com.restroute.service.backfill;

import com.restroute.domain.RestStopEntity;
import com.restroute.domain.RestStopStoreSalesRankEntity;
import com.restroute.repository.RestStopStoreSalesRankRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestStopStoreSalesRankBackfiller {

    private final RestStopStoreSalesRankRepository storeSalesRankRepository;

    public int backfill(List<RestStopEntity> restStops) {
        int mappedCount = 0;
        for (RestStopStoreSalesRankEntity rank : storeSalesRankRepository.findAll()) {
            if (!rank.isUnmapped()) {
                continue;
            }
            String serviceAreaCode =
                    RestStopUniqueNameMatcher.findUniqueServiceAreaCode(restStops, rank.getSourceRestStopName());
            if (serviceAreaCode == null) {
                continue;
            }
            rank.updateRestStopServiceAreaCode(serviceAreaCode);
            mappedCount++;
        }
        return mappedCount;
    }
}

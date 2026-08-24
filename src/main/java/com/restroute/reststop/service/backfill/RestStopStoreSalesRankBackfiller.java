package com.restroute.reststop.service.backfill;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopStoreSalesRankEntity;
import com.restroute.reststop.repository.RestStopStoreSalesRankRepository;
import com.restroute.reststop.service.backfill.util.RestStopUniqueNameMatcher;
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
            String serviceAreaCode = matchServiceAreaCode(rank, restStops);
            if (serviceAreaCode == null) {
                continue;
            }
            rank.updateRestStopServiceAreaCode(serviceAreaCode);
            mappedCount++;
        }
        return mappedCount;
    }

    private String matchServiceAreaCode(RestStopStoreSalesRankEntity rank, List<RestStopEntity> restStops) {
        if (rank.isMapped()) {
            return null;
        }
        return RestStopUniqueNameMatcher.findUniqueServiceAreaCode(restStops, rank.getSourceRestStopName());
    }
}

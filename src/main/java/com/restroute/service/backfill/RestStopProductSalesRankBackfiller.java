package com.restroute.service.backfill;

import com.restroute.domain.RestStopEntity;
import com.restroute.domain.RestStopProductSalesRankEntity;
import com.restroute.repository.RestStopProductSalesRankRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestStopProductSalesRankBackfiller {

    private final RestStopProductSalesRankRepository productSalesRankRepository;

    public int backfill(List<RestStopEntity> restStops) {
        int mappedCount = 0;
        for (RestStopProductSalesRankEntity rank : productSalesRankRepository.findAll()) {
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

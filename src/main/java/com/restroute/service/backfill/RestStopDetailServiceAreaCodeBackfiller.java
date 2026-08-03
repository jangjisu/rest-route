package com.restroute.service.backfill;

import com.restroute.domain.RestStopDetailEntity;
import com.restroute.repository.RestStopDetailRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestStopDetailServiceAreaCodeBackfiller {

    private final RestStopDetailRepository restStopDetailRepository;

    public int backfill(List<String> restStopServiceAreaCodes) {
        int mappedCount = 0;
        for (RestStopDetailEntity detail : restStopDetailRepository.findAll()) {
            String restStopServiceAreaCode =
                    restStopServiceAreaCodes.contains(detail.getServiceAreaCode()) ? detail.getServiceAreaCode() : null;
            detail.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }
}

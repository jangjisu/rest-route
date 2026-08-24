package com.restroute.reststopcontent.service;

import com.restroute.reststopcontent.domain.RestEventEntity;
import com.restroute.reststopcontent.repository.RestEventRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestEventServiceAreaCodeBackfiller {

    private final RestEventRepository restEventRepository;

    public int backfill(Map<String, String> serviceAreaCodeByStdRestCd) {
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
}

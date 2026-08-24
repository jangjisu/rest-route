package com.restroute.reststop.service.backfill;

import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import com.restroute.reststop.repository.HighwayServiceAreaInfoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HighwayServiceAreaInfoServiceAreaCodeBackfiller {

    private final HighwayServiceAreaInfoRepository highwayServiceAreaInfoRepository;

    public int backfill(List<String> restStopServiceAreaCodes) {
        int mappedCount = 0;
        for (HighwayServiceAreaInfoEntity info : highwayServiceAreaInfoRepository.findAll()) {
            String restStopServiceAreaCode = restStopServiceAreaCodes.contains(info.getBusinessFacilityCode())
                    ? info.getBusinessFacilityCode()
                    : null;
            info.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }
}

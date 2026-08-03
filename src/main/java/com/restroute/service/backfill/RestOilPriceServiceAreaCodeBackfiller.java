package com.restroute.service.backfill;

import com.restroute.domain.RestOilPriceEntity;
import com.restroute.repository.RestOilPriceRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestOilPriceServiceAreaCodeBackfiller {

    private final RestOilPriceRepository restOilPriceRepository;

    public int backfill(Map<String, String> serviceAreaCodeByOilStandardRestCode) {
        int mappedCount = 0;
        for (RestOilPriceEntity oilPrice :
                restOilPriceRepository.findByRestStopServiceAreaCodesAndAdminOverridden(null, false)) {
            String restStopServiceAreaCode = serviceAreaCodeByOilStandardRestCode.get(oilPrice.getServiceAreaCode2());
            oilPrice.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }
}

package com.restroute.service.backfill;

import com.restroute.domain.RestOilPriceEntity;
import com.restroute.repository.RestOilPriceRepository;
import com.restroute.repository.RestOilRepository;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RestOilPriceServiceAreaCodeBackfiller {

    private final RestOilPriceRepository restOilPriceRepository;
    private final RestOilRepository restOilRepository;

    public int backfill() {
        Map<String, String> serviceAreaCodeByOilStandardRestCode = restOilRepository.findAll().stream()
                .filter(restOil -> StringUtils.hasText(restOil.getStandardRestCode()))
                .filter(restOil -> StringUtils.hasText(restOil.getRestStopServiceAreaCode()))
                .collect(Collectors.toMap(
                        restOil -> restOil.getStandardRestCode(),
                        restOil -> restOil.getRestStopServiceAreaCode(),
                        (first, second) -> first));
        int mappedCount = 0;
        for (RestOilPriceEntity oilPrice : restOilPriceRepository.findAll()) {
            String restStopServiceAreaCode = serviceAreaCodeByOilStandardRestCode.get(oilPrice.getServiceAreaCode2());
            oilPrice.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }
}

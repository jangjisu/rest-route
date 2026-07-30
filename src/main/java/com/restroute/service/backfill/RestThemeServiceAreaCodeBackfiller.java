package com.restroute.service.backfill;

import com.restroute.domain.RestThemeEntity;
import com.restroute.repository.RestThemeRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestThemeServiceAreaCodeBackfiller {

    private final RestThemeRepository restThemeRepository;

    public int backfill(Map<String, String> serviceAreaCodeByStdRestCd) {
        int mappedCount = 0;
        for (RestThemeEntity theme : restThemeRepository.findAll()) {
            String restStopServiceAreaCode = serviceAreaCodeByStdRestCd.get(theme.getStdRestCd());
            theme.updateRestStopServiceAreaCode(restStopServiceAreaCode);
            if (restStopServiceAreaCode != null) {
                mappedCount++;
            }
        }
        return mappedCount;
    }
}

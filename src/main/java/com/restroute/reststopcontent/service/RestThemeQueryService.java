package com.restroute.reststopcontent.service;

import com.restroute.reststopcontent.domain.RestThemeEntity;
import com.restroute.reststopcontent.repository.RestThemeRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RestThemeQueryService {

    private final RestThemeRepository restThemeRepository;

    @Transactional(readOnly = true)
    public List<String> findThemeMappedServiceAreaCodes(Collection<String> serviceAreaCodes) {
        List<String> validServiceAreaCodes = serviceAreaCodes.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (validServiceAreaCodes.isEmpty()) {
            return List.of();
        }

        return restThemeRepository.findAllByRestStopServiceAreaCodeIn(validServiceAreaCodes).stream()
                .map(RestThemeEntity::getRestStopServiceAreaCode)
                .distinct()
                .toList();
    }
}

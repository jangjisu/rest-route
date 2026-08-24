package com.restroute.evcharger.service;

import com.restroute.evcharger.domain.EvChargerStationMappingEntity;
import com.restroute.evcharger.repository.EvChargerRepository;
import com.restroute.evcharger.repository.EvChargerStationMappingRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EvChargerQueryService {

    private static final String ACTIVE = "N";

    private final EvChargerRepository evChargerRepository;
    private final EvChargerStationMappingRepository mappingRepository;

    @Transactional(readOnly = true)
    public List<String> findChargerMappedServiceAreaCodes(Collection<String> serviceAreaCodes) {
        List<String> validServiceAreaCodes = serviceAreaCodes.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (validServiceAreaCodes.isEmpty()) {
            return List.of();
        }

        return mappingRepository.findAllByRestStopServiceAreaCodeIn(validServiceAreaCodes).stream()
                .map(EvChargerStationMappingEntity::getRestStopServiceAreaCode)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public int findActiveChargerCount(String serviceAreaCode) {
        if (!StringUtils.hasText(serviceAreaCode)) {
            return 0;
        }
        List<String> statIds = mappingRepository.findAllByRestStopServiceAreaCodeIn(List.of(serviceAreaCode)).stream()
                .map(EvChargerStationMappingEntity::getStatId)
                .distinct()
                .toList();
        if (statIds.isEmpty()) {
            return 0;
        }
        return evChargerRepository.findAllByStatIdInAndDelYn(statIds, ACTIVE).size();
    }
}

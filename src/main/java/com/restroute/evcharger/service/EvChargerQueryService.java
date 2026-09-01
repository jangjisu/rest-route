package com.restroute.evcharger.service;

import com.restroute.evcharger.domain.EvChargerEntity;
import com.restroute.evcharger.domain.EvChargerStationMappingEntity;
import com.restroute.evcharger.repository.EvChargerRepository;
import com.restroute.evcharger.repository.EvChargerStationMappingRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    /**
     * 여러 휴게소의 활성 충전기 수를 한 번에 조회한다 — findActiveChargerCount를 코드마다 반복 호출하면
     * N+1이 되는 목록 화면에서 쓴다. 매핑이 없는 코드는 결과 맵에서 아예 빠진다.
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> findActiveChargerCounts(Collection<String> serviceAreaCodes) {
        List<String> validServiceAreaCodes = serviceAreaCodes.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (validServiceAreaCodes.isEmpty()) {
            return Map.of();
        }

        List<EvChargerStationMappingEntity> mappings =
                mappingRepository.findAllByRestStopServiceAreaCodeIn(validServiceAreaCodes);
        if (mappings.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> statIdsByServiceAreaCode = mappings.stream()
                .collect(Collectors.groupingBy(
                        EvChargerStationMappingEntity::getRestStopServiceAreaCode,
                        Collectors.mapping(EvChargerStationMappingEntity::getStatId, Collectors.toList())));
        List<String> allStatIds = mappings.stream()
                .map(EvChargerStationMappingEntity::getStatId)
                .distinct()
                .toList();
        // 충전기 여러 대(다른 chgerId)가 같은 statId를 공유할 수 있어서, statId별로 몇 대인지 세어둔다 —
        // 활성 statId 집합만 봐서는 대수를 알 수 없다.
        Map<String, Long> chargerCountByStatId =
                evChargerRepository.findAllByStatIdInAndDelYn(allStatIds, ACTIVE).stream()
                        .collect(Collectors.groupingBy(EvChargerEntity::getStatId, Collectors.counting()));

        return statIdsByServiceAreaCode.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .mapToInt(statId -> chargerCountByStatId
                                        .getOrDefault(statId, 0L)
                                        .intValue())
                                .sum()));
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

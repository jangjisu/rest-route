package com.restroute.evcharger.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.evcharger.domain.EvChargerStationMappingEntity;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class EvChargerStationMappingRepositoryTest {

    @Autowired
    private EvChargerStationMappingRepository mappingRepository;

    @Test
    @DisplayName("휴게소 코드로 EV 충전소 매핑을 일괄 조회한다")
    void findAllByRestStopServiceAreaCodeIn_returnsMatchingMappings() {
        mappingRepository.save(mapping("ME1", "A00001"));
        mappingRepository.save(mapping("ME2", "B00001"));

        List<EvChargerStationMappingEntity> result =
                mappingRepository.findAllByRestStopServiceAreaCodeIn(Set.of("A00001"));

        assertThat(result).extracting(EvChargerStationMappingEntity::getStatId).containsExactly("ME1");
    }

    @Test
    @DisplayName("최신 backfill 결과에 없는 statId 매핑을 삭제한다")
    void deleteAllByStatIdNotIn_removesStaleMappings() {
        mappingRepository.save(mapping("ME1", "A00001"));
        mappingRepository.save(mapping("ME2", "B00001"));

        mappingRepository.deleteAllByStatIdNotIn(Set.of("ME1"));

        assertThat(mappingRepository.findAll())
                .extracting(EvChargerStationMappingEntity::getStatId)
                .containsExactly("ME1");
    }

    private EvChargerStationMappingEntity mapping(String statId, String serviceAreaCode) {
        EvChargerStationMappingEntity mapping = EvChargerStationMappingEntity.of(statId);
        mapping.updateMatch(serviceAreaCode);
        return mapping;
    }
}

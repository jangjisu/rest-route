package com.restroute.evcharger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.evcharger.client.response.EvChargerItem;
import com.restroute.evcharger.domain.EvChargerEntity;
import com.restroute.evcharger.domain.EvChargerStationMappingEntity;
import com.restroute.evcharger.repository.EvChargerRepository;
import com.restroute.evcharger.repository.EvChargerStationMappingRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvChargerQueryServiceTest {

    @Mock
    private EvChargerRepository evChargerRepository;

    @Mock
    private EvChargerStationMappingRepository mappingRepository;

    private EvChargerQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new EvChargerQueryService(evChargerRepository, mappingRepository);
    }

    @Test
    @DisplayName("매핑된 휴게소 코드를 일괄 조회한다")
    void findChargerMappedServiceAreaCodes_returnsMappedCodes() {
        EvChargerStationMappingEntity mapping = EvChargerStationMappingEntity.of("ME1");
        mapping.updateMatch("A00001");
        when(mappingRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001")))
                .thenReturn(List.of(mapping));
        List<String> result = queryService.findChargerMappedServiceAreaCodes(List.of("A00001"));

        assertThat(result).containsExactly("A00001");
    }

    @Test
    @DisplayName("휴게소 코드가 없으면 경로용 매핑 결과를 조회하지 않는다")
    void findChargerMappedServiceAreaCodes_returnsEmptyForBlankInput() {
        assertThat(queryService.findChargerMappedServiceAreaCodes(List.of("", " ")))
                .isEmpty();
    }

    @Test
    @DisplayName("여러 충전소가 같은 휴게소에 매핑되어도 경로용 휴게소 코드는 중복하지 않는다")
    void findChargerMappedServiceAreaCodes_removesDuplicateRestStops() {
        EvChargerStationMappingEntity first = EvChargerStationMappingEntity.of("ME1");
        first.updateMatch("A00001");
        EvChargerStationMappingEntity second = EvChargerStationMappingEntity.of("ME2");
        second.updateMatch("A00001");
        when(mappingRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001")))
                .thenReturn(List.of(first, second));

        assertThat(queryService.findChargerMappedServiceAreaCodes(List.of("A00001", "A00001")))
                .containsExactly("A00001");
    }

    @Test
    @DisplayName("매핑된 휴게소의 활성 충전기 대수를 조회한다")
    void findActiveChargerCount_countsActiveChargers() throws Exception {
        EvChargerStationMappingEntity mapping = EvChargerStationMappingEntity.of("ME1");
        mapping.updateMatch("A00001");
        when(mappingRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001")))
                .thenReturn(List.of(mapping));
        when(evChargerRepository.findAllByStatIdInAndDelYn(List.of("ME1"), "N"))
                .thenReturn(List.of(charger("ME1", "01", "N"), charger("ME1", "02", "N")));

        assertThat(queryService.findActiveChargerCount("A00001")).isEqualTo(2);
    }

    @Test
    @DisplayName("한 휴게소에 여러 statId가 매핑되면 활성 충전기 수를 합산한다")
    void findActiveChargerCount_countsChargersAcrossMultipleStations() throws Exception {
        EvChargerStationMappingEntity firstMapping = EvChargerStationMappingEntity.of("ME1");
        firstMapping.updateMatch("A00001");
        EvChargerStationMappingEntity secondMapping = EvChargerStationMappingEntity.of("ME2");
        secondMapping.updateMatch("A00001");
        when(mappingRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001")))
                .thenReturn(List.of(firstMapping, secondMapping));
        when(evChargerRepository.findAllByStatIdInAndDelYn(List.of("ME1", "ME2"), "N"))
                .thenReturn(List.of(charger("ME1", "01", "N"), charger("ME1", "02", "N"), charger("ME2", "01", "N")));

        assertThat(queryService.findActiveChargerCount("A00001")).isEqualTo(3);
    }

    @Test
    @DisplayName("매핑이 없거나 휴게소 코드가 없으면 상세 충전기 수를 0으로 반환한다")
    void findActiveChargerCount_returnsZeroWithoutMapping() {
        when(mappingRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001")))
                .thenReturn(List.of());

        assertThat(queryService.findActiveChargerCount("A00001")).isZero();
        assertThat(queryService.findActiveChargerCount(" ")).isZero();
    }

    @Test
    @DisplayName("여러 휴게소 코드의 활성 충전기 수를 한 번에 조회한다")
    void findActiveChargerCounts_returnsCountPerServiceAreaCode() throws Exception {
        EvChargerStationMappingEntity firstMapping = EvChargerStationMappingEntity.of("ME1");
        firstMapping.updateMatch("A00001");
        EvChargerStationMappingEntity secondMapping = EvChargerStationMappingEntity.of("ME2");
        secondMapping.updateMatch("A00002");
        when(mappingRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001", "A00002")))
                .thenReturn(List.of(firstMapping, secondMapping));
        when(evChargerRepository.findAllByStatIdInAndDelYn(List.of("ME1", "ME2"), "N"))
                .thenReturn(List.of(charger("ME1", "01", "N"), charger("ME1", "02", "N"), charger("ME2", "01", "N")));

        Map<String, Integer> result = queryService.findActiveChargerCounts(List.of("A00001", "A00002"));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("A00001", 2, "A00002", 1));
    }

    @Test
    @DisplayName("매핑이 없는 휴게소 코드는 결과 맵에서 빠진다")
    void findActiveChargerCounts_omitsCodesWithoutMapping() {
        when(mappingRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001")))
                .thenReturn(List.of());

        assertThat(queryService.findActiveChargerCounts(List.of("A00001"))).isEmpty();
    }

    @Test
    @DisplayName("휴게소 코드가 없으면 빈 맵을 반환한다")
    void findActiveChargerCounts_returnsEmptyMapForBlankInput() {
        assertThat(queryService.findActiveChargerCounts(List.of())).isEmpty();
    }

    private EvChargerEntity charger(String statId, String chgerId, String delYn) throws Exception {
        EvChargerItem item = new ObjectMapper()
                .readValue(
                        "{\"statId\":\"" + statId + "\",\"chgerId\":\"" + chgerId + "\",\"delYn\":\"" + delYn + "\"}",
                        EvChargerItem.class);
        return EvChargerEntity.from(item);
    }
}

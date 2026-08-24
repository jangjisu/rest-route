package com.restroute.evcharger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.evcharger.domain.EvChargerStationMappingEntity;
import com.restroute.evcharger.repository.EvChargerRepository;
import com.restroute.evcharger.repository.EvChargerStationMappingRepository;
import com.restroute.evcharger.service.mapping.EvChargerStationMappingCalculator;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopDetailRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvChargerStationMappingBackfillerTest {

    @Mock
    private EvChargerStationMappingCalculator evChargerStationMappingCalculator;

    @Mock
    private RestStopDetailRepository restStopDetailRepository;

    @Mock
    private EvChargerRepository evChargerRepository;

    @Mock
    private EvChargerStationMappingRepository evChargerStationMappingRepository;

    private EvChargerStationMappingBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new EvChargerStationMappingBackfiller(
                evChargerStationMappingCalculator,
                restStopDetailRepository,
                evChargerRepository,
                evChargerStationMappingRepository);
    }

    @Test
    @DisplayName("계산된 매핑으로 기존 매핑을 전부 지우고 다시 저장한 뒤 개수를 반환한다")
    void backfill_replacesExistingMappingsWithCalculatedOnes() {
        List<RestStopEntity> restStops = List.of(mock(RestStopEntity.class));
        List<RestStopDetailEntity> details = List.of(mock(RestStopDetailEntity.class));
        List<EvChargerStationMappingEntity> calculated =
                List.of(mock(EvChargerStationMappingEntity.class), mock(EvChargerStationMappingEntity.class));
        when(restStopDetailRepository.findAll()).thenReturn(details);
        when(evChargerRepository.findAllByDelYn("N")).thenReturn(List.of());
        when(evChargerStationMappingCalculator.calculate(restStops, details, List.of()))
                .thenReturn(calculated);

        int mappedCount = backfiller.backfill(restStops);

        assertThat(mappedCount).isEqualTo(2);
        verify(evChargerStationMappingRepository).deleteAllInBatch();
        verify(evChargerStationMappingRepository).saveAll(calculated);
    }

    @Test
    @DisplayName("계산된 매핑이 없으면 0을 반환하지만 기존 매핑은 여전히 비운다")
    void backfill_clearsExistingMappingsEvenWhenNothingIsCalculated() {
        when(restStopDetailRepository.findAll()).thenReturn(List.of());
        when(evChargerRepository.findAllByDelYn("N")).thenReturn(List.of());
        when(evChargerStationMappingCalculator.calculate(any(), any(), any())).thenReturn(List.of());

        int mappedCount = backfiller.backfill(List.of());

        assertThat(mappedCount).isZero();
        verify(evChargerStationMappingRepository).deleteAllInBatch();
        verify(evChargerStationMappingRepository).saveAll(List.of());
    }
}

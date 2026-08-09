package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.domain.FlightCityEntity;
import com.restroute.flight.repository.FlightCityRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightCitySeedServiceTest {

    @Mock
    private FlightCityRepository flightCityRepository;

    private FlightCitySeedService flightCitySeedService;

    @BeforeEach
    void setUp() {
        flightCitySeedService = new FlightCitySeedService(flightCityRepository);
    }

    @Test
    @DisplayName("테이블이 비어있으면 시드 도시 목록을 저장하고 저장 건수를 반환한다")
    void seedIfEmpty_savesSeedCitiesWhenTableIsEmpty() {
        when(flightCityRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<FlightCityEntity>> captor = ArgumentCaptor.forClass(List.class);

        int savedCount = flightCitySeedService.seedIfEmpty();

        verify(flightCityRepository).saveAll(captor.capture());
        assertThat(savedCount).isEqualTo(13);
        assertThat(captor.getValue()).hasSize(13);
        assertThat(captor.getValue()).extracting(FlightCityEntity::getCode).contains("ICN", "OSA", "BKK", "GUM");
    }

    @Test
    @DisplayName("이미 데이터가 있으면 아무것도 저장하지 않는다")
    void seedIfEmpty_skipsWhenTableAlreadyHasData() {
        when(flightCityRepository.count()).thenReturn(5L);

        int savedCount = flightCitySeedService.seedIfEmpty();

        assertThat(savedCount).isZero();
        verify(flightCityRepository, never()).saveAll(anyList());
    }
}

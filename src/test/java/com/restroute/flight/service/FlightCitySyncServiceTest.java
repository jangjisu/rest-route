package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.client.TravelpayoutsClient;
import com.restroute.flight.client.response.TravelpayoutsCityItem;
import com.restroute.flight.domain.FlightCityEntity;
import com.restroute.flight.repository.FlightCityRepository;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class FlightCitySyncServiceTest {

    @Mock
    private TravelpayoutsClient travelpayoutsClient;

    @Mock
    private FlightCityRepository flightCityRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private FlightCitySyncService flightCitySyncService;

    @BeforeEach
    void setUp() {
        flightCitySyncService =
                new FlightCitySyncService(travelpayoutsClient, flightCityRepository, transactionTemplate);
    }

    private void stubTransactionTemplateToRunAction() {
        doAnswer(invocation -> {
                    Consumer<TransactionStatus> action = invocation.getArgument(0);
                    action.accept(mock(TransactionStatus.class));
                    return null;
                })
                .when(transactionTemplate)
                .executeWithoutResult(any());
    }

    @Test
    @DisplayName("취항 공항이 있는 도시만 걸러서 기존 데이터를 지우고 다시 저장한다")
    void refreshFlightCities_replacesWithFlightableCitiesOnly() {
        stubTransactionTemplateToRunAction();
        when(travelpayoutsClient.citiesData())
                .thenReturn(List.of(
                        new TravelpayoutsCityItem("Osaka", "OSA", "JP", true),
                        new TravelpayoutsCityItem("Tapeta", "TPT", "LR", false)));

        int savedCount = flightCitySyncService.refreshFlightCities();

        assertThat(savedCount).isEqualTo(1);
        verify(flightCityRepository).deleteAllInBatch();
        ArgumentCaptor<List<FlightCityEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(flightCityRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(FlightCityEntity::getCode).containsExactly("OSA");
    }

    @Test
    @DisplayName("외부 API 호출이 실패하면 기존 데이터를 지우지 않고 0을 반환한다")
    void refreshFlightCities_keepsExistingDataWhenFetchFails() {
        when(travelpayoutsClient.citiesData()).thenThrow(new IllegalStateException("boom"));

        int savedCount = flightCitySyncService.refreshFlightCities();

        assertThat(savedCount).isZero();
        verify(flightCityRepository, never()).deleteAllInBatch();
    }

    @Test
    @DisplayName("이미 데이터가 있으면 초기 동기화를 건너뛴다")
    void initializeFlightCitiesIfEmpty_skipsWhenTableAlreadyHasData() {
        when(flightCityRepository.count()).thenReturn(5L);

        int savedCount = flightCitySyncService.initializeFlightCitiesIfEmpty();

        assertThat(savedCount).isZero();
        verify(travelpayoutsClient, never()).citiesData();
    }

    @Test
    @DisplayName("테이블이 비어있으면 전체 동기화를 수행한다")
    void initializeFlightCitiesIfEmpty_syncsWhenTableIsEmpty() {
        stubTransactionTemplateToRunAction();
        when(flightCityRepository.count()).thenReturn(0L);
        when(travelpayoutsClient.citiesData())
                .thenReturn(List.of(new TravelpayoutsCityItem("Osaka", "OSA", "JP", true)));

        int savedCount = flightCitySyncService.initializeFlightCitiesIfEmpty();

        assertThat(savedCount).isEqualTo(1);
    }
}

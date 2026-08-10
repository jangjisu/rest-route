package com.restroute.flight.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import com.restroute.flight.service.FlightCitySyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class FlightCitySchedulerTest {

    @Mock
    private FlightCitySyncService flightCitySyncService;

    @InjectMocks
    private FlightCityScheduler flightCityScheduler;

    @Test
    @DisplayName("매일 갱신을 service에 위임하고 결과를 기록한다")
    void syncFlightCitiesDaily_logsRefreshedCount(CapturedOutput output) {
        when(flightCitySyncService.refreshFlightCities()).thenReturn(3522);

        flightCityScheduler.syncFlightCitiesDaily();

        assertThat(output).contains("Scheduled flight city sync completed. savedCount=3522");
    }

    @Test
    @DisplayName("갱신 실패가 스케줄러 실행으로 전파되지 않는다")
    void syncFlightCitiesDaily_doesNotPropagateFailure(CapturedOutput output) {
        when(flightCitySyncService.refreshFlightCities()).thenThrow(new IllegalStateException("fetch failed"));

        assertThatCode(() -> flightCityScheduler.syncFlightCitiesDaily()).doesNotThrowAnyException();

        assertThat(output).contains("Scheduled flight city sync failed.").contains("fetch failed");
    }
}

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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class FlightCityStartupInitializerTest {

    @Mock
    private FlightCitySyncService flightCitySyncService;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private FlightCityStartupInitializer flightCityStartupInitializer;

    @Test
    @DisplayName("서버 시작 시 도시 초기 동기화를 service에 위임하고 결과를 기록한다")
    void run_logsSyncedCountWhenCitiesSaved(CapturedOutput output) {
        when(flightCitySyncService.initializeFlightCitiesIfEmpty()).thenReturn(3522);

        flightCityStartupInitializer.run(applicationArguments);

        assertThat(output).contains("Initial flight city sync completed. savedCount=3522");
    }

    @Test
    @DisplayName("이미 데이터가 있으면 건너뛰었다고 기록한다")
    void run_logsSkippedWhenAlreadySynced() {
        when(flightCitySyncService.initializeFlightCitiesIfEmpty()).thenReturn(0);

        assertThatCode(() -> flightCityStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("초기 동기화 실패가 앱 시작으로 전파되지 않는다")
    void run_doesNotPropagateSyncFailure(CapturedOutput output) {
        when(flightCitySyncService.initializeFlightCitiesIfEmpty()).thenThrow(new IllegalStateException("db error"));

        assertThatCode(() -> flightCityStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output).contains("Initial flight city sync failed.").contains("db error");
    }
}

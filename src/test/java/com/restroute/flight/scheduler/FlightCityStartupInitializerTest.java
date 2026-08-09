package com.restroute.flight.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import com.restroute.flight.service.FlightCitySeedService;
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
    private FlightCitySeedService flightCitySeedService;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private FlightCityStartupInitializer flightCityStartupInitializer;

    @Test
    @DisplayName("서버 시작 시 도시 시드 적재를 service에 위임하고 결과를 기록한다")
    void run_logsSeededCountWhenCitiesSaved(CapturedOutput output) {
        when(flightCitySeedService.seedIfEmpty()).thenReturn(13);

        flightCityStartupInitializer.run(applicationArguments);

        assertThat(output).contains("Initial flight city seed completed. savedCount=13");
    }

    @Test
    @DisplayName("이미 데이터가 있으면 건너뛰었다고 기록한다")
    void run_logsSkippedWhenAlreadySeeded() {
        when(flightCitySeedService.seedIfEmpty()).thenReturn(0);

        assertThatCode(() -> flightCityStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("시드 적재 실패가 앱 시작으로 전파되지 않는다")
    void run_doesNotPropagateSeedFailure(CapturedOutput output) {
        when(flightCitySeedService.seedIfEmpty()).thenThrow(new IllegalStateException("db error"));

        assertThatCode(() -> flightCityStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output).contains("Initial flight city seed failed.").contains("db error");
    }
}

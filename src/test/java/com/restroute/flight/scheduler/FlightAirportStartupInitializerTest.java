package com.restroute.flight.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.cache.FlightAirportNameCache;
import com.restroute.flight.repository.FlightAirportRepository;
import com.restroute.flight.service.FlightReferenceDataSeeder;
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
class FlightAirportStartupInitializerTest {

    @Mock
    private FlightReferenceDataSeeder flightReferenceDataSeeder;

    @Mock
    private FlightAirportRepository flightAirportRepository;

    @Mock
    private FlightAirportNameCache flightAirportNameCache;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private FlightAirportStartupInitializer flightAirportStartupInitializer;

    @Test
    @DisplayName("서버 시작 시 SQL 시드 로딩을 위임하고 결과를 기록한 뒤 캐시를 채운다")
    void run_logsSeedCountWhenAirportsSaved(CapturedOutput output) {
        when(flightReferenceDataSeeder.seedIfEmpty(eq("data/flight-airport-seed.sql"), any()))
                .thenReturn(3673);

        flightAirportStartupInitializer.run(applicationArguments);

        assertThat(output).contains("Initial flight airport seeding completed. savedCount=3673");
        verify(flightAirportNameCache).refresh();
    }

    @Test
    @DisplayName("이미 데이터가 있으면 건너뛰었다고 기록하고도 캐시는 채운다")
    void run_logsSkippedWhenAlreadySeeded() {
        when(flightReferenceDataSeeder.seedIfEmpty(eq("data/flight-airport-seed.sql"), any()))
                .thenReturn(0);

        assertThatCode(() -> flightAirportStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        verify(flightAirportNameCache).refresh();
    }

    @Test
    @DisplayName("시딩 실패가 앱 시작으로 전파되지 않아도 캐시는 채운다")
    void run_doesNotPropagateSeedingFailure(CapturedOutput output) {
        when(flightReferenceDataSeeder.seedIfEmpty(eq("data/flight-airport-seed.sql"), any()))
                .thenThrow(new IllegalStateException("sql error"));

        assertThatCode(() -> flightAirportStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output).contains("Initial flight airport seeding failed.").contains("sql error");
        verify(flightAirportNameCache).refresh();
    }
}

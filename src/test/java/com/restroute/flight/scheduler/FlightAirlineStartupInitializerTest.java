package com.restroute.flight.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.flight.cache.FlightAirlineNameCache;
import com.restroute.flight.repository.FlightAirlineRepository;
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
class FlightAirlineStartupInitializerTest {

    @Mock
    private FlightReferenceDataSeeder flightReferenceDataSeeder;

    @Mock
    private FlightAirlineRepository flightAirlineRepository;

    @Mock
    private FlightAirlineNameCache flightAirlineNameCache;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private FlightAirlineStartupInitializer flightAirlineStartupInitializer;

    @Test
    @DisplayName("서버 시작 시 SQL 재시딩을 위임하고 결과를 기록한 뒤 캐시를 채운다")
    void run_logsSeedCountWhenAirlinesSaved(CapturedOutput output) {
        when(flightReferenceDataSeeder.reseed(eq("data/flight-airline-seed.sql"), any(), any()))
                .thenReturn(1159);

        flightAirlineStartupInitializer.run(applicationArguments);

        assertThat(output).contains("Initial flight airline seeding completed. savedCount=1159");
        verify(flightAirlineNameCache).refresh();
    }

    @Test
    @DisplayName("시딩 실패가 앱 시작으로 전파되지 않아도 캐시는 채운다")
    void run_doesNotPropagateSeedingFailure(CapturedOutput output) {
        when(flightReferenceDataSeeder.reseed(eq("data/flight-airline-seed.sql"), any(), any()))
                .thenThrow(new IllegalStateException("sql error"));

        assertThatCode(() -> flightAirlineStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output).contains("Initial flight airline seeding failed.").contains("sql error");
        verify(flightAirlineNameCache).refresh();
    }
}

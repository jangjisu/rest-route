package com.restroute.flight.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.restroute.flight.cache.FlightAirlineNameCache;
import com.restroute.flight.cache.FlightAirportNameCache;
import com.restroute.flight.cache.FlightCityNameCache;
import com.restroute.flight.cache.FlightCountryNameCache;
import com.restroute.flight.repository.FlightAirlineRepository;
import com.restroute.flight.repository.FlightAirportRepository;
import com.restroute.flight.repository.FlightCityRepository;
import com.restroute.flight.repository.FlightCountryRepository;
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
import org.springframework.core.env.Environment;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class FlightReferenceDataStartupInitializerTest {

    @Mock
    private FlightReferenceDataSeeder flightReferenceDataSeeder;

    @Mock
    private Environment environment;

    @Mock
    private FlightAirlineRepository flightAirlineRepository;

    @Mock
    private FlightAirlineNameCache flightAirlineNameCache;

    @Mock
    private FlightAirportRepository flightAirportRepository;

    @Mock
    private FlightAirportNameCache flightAirportNameCache;

    @Mock
    private FlightCityRepository flightCityRepository;

    @Mock
    private FlightCityNameCache flightCityNameCache;

    @Mock
    private FlightCountryRepository flightCountryRepository;

    @Mock
    private FlightCountryNameCache flightCountryNameCache;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private FlightReferenceDataStartupInitializer flightReferenceDataStartupInitializer;

    @Test
    @DisplayName("4종 참조 데이터 모두 SQL 재시딩을 위임하고 결과를 기록한 뒤 캐시를 채운다")
    void run_seedsAndRefreshesAllFourDomainsWhenEnabled() {
        allStartupEnabledPropertiesReturn(true);
        when(flightReferenceDataSeeder.reseed(eq("data/flight-airline-seed.sql"), any(), any()))
                .thenReturn(1159);
        when(flightReferenceDataSeeder.reseed(eq("data/flight-airport-seed.sql"), any(), any()))
                .thenReturn(3673);
        when(flightReferenceDataSeeder.reseed(eq("data/flight-city-seed.sql"), any(), any()))
                .thenReturn(3522);
        when(flightReferenceDataSeeder.reseed(eq("data/flight-country-seed.sql"), any(), any()))
                .thenReturn(253);

        flightReferenceDataStartupInitializer.run(applicationArguments);

        verify(flightAirlineNameCache).refresh();
        verify(flightAirportNameCache).refresh();
        verify(flightCityNameCache).refresh();
        verify(flightCountryNameCache).refresh();
    }

    @Test
    @DisplayName("서버 시작 시 SQL 재시딩을 위임하고 결과를 기록한 뒤 캐시를 채운다")
    void run_logsSeedCountWhenSaved(CapturedOutput output) {
        allStartupEnabledPropertiesReturn(true);
        when(flightReferenceDataSeeder.reseed(eq("data/flight-airline-seed.sql"), any(), any()))
                .thenReturn(1159);

        flightReferenceDataStartupInitializer.run(applicationArguments);

        assertThat(output).contains("Initial flight airline seeding completed. savedCount=1159");
    }

    @Test
    @DisplayName("한 도메인의 시딩 실패가 앱 시작으로 전파되지 않고, 그 도메인도 캐시는 채운다")
    void run_doesNotPropagateSeedingFailureAndStillRefreshesThatDomainsCache(CapturedOutput output) {
        allStartupEnabledPropertiesReturn(true);
        when(flightReferenceDataSeeder.reseed(eq("data/flight-airline-seed.sql"), any(), any()))
                .thenThrow(new IllegalStateException("sql error"));

        assertThatCode(() -> flightReferenceDataStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output).contains("Initial flight airline seeding failed.").contains("sql error");
        verify(flightAirlineNameCache).refresh();
    }

    @Test
    @DisplayName("한 도메인의 시딩 실패가 다른 도메인 처리를 막지 않는다")
    void run_continuesOtherDomainsAfterOneFails() {
        allStartupEnabledPropertiesReturn(true);
        when(flightReferenceDataSeeder.reseed(eq("data/flight-airline-seed.sql"), any(), any()))
                .thenThrow(new IllegalStateException("sql error"));
        when(flightReferenceDataSeeder.reseed(eq("data/flight-airport-seed.sql"), any(), any()))
                .thenReturn(3673);

        flightReferenceDataStartupInitializer.run(applicationArguments);

        verify(flightAirportNameCache).refresh();
    }

    @Test
    @DisplayName("도메인 프로퍼티가 꺼져 있으면 재시딩도 캐시 refresh도 하지 않는다")
    void run_skipsDomainWhenStartupDisabled() {
        when(environment.getProperty(eq("flight.airline.sync.startup-enabled"), eq(Boolean.class), eq(true)))
                .thenReturn(false);
        when(environment.getProperty(eq("flight.airport.sync.startup-enabled"), eq(Boolean.class), eq(true)))
                .thenReturn(true);
        when(environment.getProperty(eq("flight.city.sync.startup-enabled"), eq(Boolean.class), eq(true)))
                .thenReturn(true);
        when(environment.getProperty(eq("flight.country.sync.startup-enabled"), eq(Boolean.class), eq(true)))
                .thenReturn(true);
        when(flightReferenceDataSeeder.reseed(eq("data/flight-airport-seed.sql"), any(), any()))
                .thenReturn(3673);
        when(flightReferenceDataSeeder.reseed(eq("data/flight-city-seed.sql"), any(), any()))
                .thenReturn(3522);
        when(flightReferenceDataSeeder.reseed(eq("data/flight-country-seed.sql"), any(), any()))
                .thenReturn(253);

        flightReferenceDataStartupInitializer.run(applicationArguments);

        verify(flightReferenceDataSeeder, never()).reseed(eq("data/flight-airline-seed.sql"), any(), any());
        verifyNoInteractions(flightAirlineNameCache);
    }

    private void allStartupEnabledPropertiesReturn(boolean enabled) {
        when(environment.getProperty(any(String.class), eq(Boolean.class), eq(true)))
                .thenReturn(enabled);
    }
}

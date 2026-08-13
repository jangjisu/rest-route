package com.restroute.flight.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.flight.domain.FlightAirportEntity;
import com.restroute.flight.repository.FlightAirportRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightAirportNameCacheTest {

    @Mock
    private FlightAirportRepository flightAirportRepository;

    private FlightAirportNameCache flightAirportNameCache;

    @BeforeEach
    void setUp() {
        flightAirportNameCache = new FlightAirportNameCache(flightAirportRepository);
    }

    @Test
    @DisplayName("refresh 전에는 조회 결과가 없다")
    void findName_returnsNull_beforeRefresh() {
        assertThat(flightAirportNameCache.findName("ICN")).isNull();
    }

    @Test
    @DisplayName("korName이 있으면 refresh 이후 korName을 반환한다")
    void findName_returnsKorName_whenPresent() {
        when(flightAirportRepository.findAll())
                .thenReturn(List.of(
                        new FlightAirportEntity("ICN", "인천국제공항", "Incheon International Airport", "SEL", "KR")));

        flightAirportNameCache.refresh();

        assertThat(flightAirportNameCache.findName("ICN")).isEqualTo("인천국제공항");
    }

    @Test
    @DisplayName("korName이 없으면 refresh 이후 engName으로 대체한다")
    void findName_fallsBackToEngName_whenKorNameMissing() {
        when(flightAirportRepository.findAll())
                .thenReturn(List.of(new FlightAirportEntity("MBE", null, "Monbetsu Airport", "MBE", "JP")));

        flightAirportNameCache.refresh();

        assertThat(flightAirportNameCache.findName("MBE")).isEqualTo("Monbetsu Airport");
        assertThat(flightAirportNameCache.findName("ZZZ")).isNull();
    }
}

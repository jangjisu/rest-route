package com.restroute.flight.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.flight.domain.FlightCityEntity;
import com.restroute.flight.repository.FlightCityRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightCityNameCacheTest {

    @Mock
    private FlightCityRepository flightCityRepository;

    private FlightCityNameCache flightCityNameCache;

    @BeforeEach
    void setUp() {
        flightCityNameCache = new FlightCityNameCache(flightCityRepository);
    }

    @Test
    @DisplayName("refresh 전에는 조회 결과가 없다")
    void findName_returnsNull_beforeRefresh() {
        assertThat(flightCityNameCache.findName("OSA")).isNull();
    }

    @Test
    @DisplayName("korName이 있으면 refresh 이후 korName을 반환한다")
    void findName_returnsKorName_whenPresent() {
        when(flightCityRepository.findAll()).thenReturn(List.of(new FlightCityEntity("OSA", "오사카", "Osaka", "JP")));

        flightCityNameCache.refresh();

        assertThat(flightCityNameCache.findName("OSA")).isEqualTo("오사카");
    }

    @Test
    @DisplayName("korName이 없으면 refresh 이후 engName으로 대체한다")
    void findName_fallsBackToEngName_whenKorNameMissing() {
        when(flightCityRepository.findAll()).thenReturn(List.of(new FlightCityEntity("YHR", null, "Chevery", "CA")));

        flightCityNameCache.refresh();

        assertThat(flightCityNameCache.findName("YHR")).isEqualTo("Chevery");
        assertThat(flightCityNameCache.findName("ZZZ")).isNull();
    }
}

package com.restroute.flight.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.flight.domain.FlightCountryEntity;
import com.restroute.flight.repository.FlightCountryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightCountryNameCacheTest {

    @Mock
    private FlightCountryRepository flightCountryRepository;

    private FlightCountryNameCache flightCountryNameCache;

    @BeforeEach
    void setUp() {
        flightCountryNameCache = new FlightCountryNameCache(flightCountryRepository);
    }

    @Test
    @DisplayName("refresh 전에는 조회 결과가 없다")
    void findName_returnsNull_beforeRefresh() {
        assertThat(flightCountryNameCache.findName("JP")).isNull();
    }

    @Test
    @DisplayName("refresh 이후에는 DB 왕복 없이 korName을 조회한다")
    void findName_returnsCachedKorName_afterRefresh() {
        when(flightCountryRepository.findAll()).thenReturn(List.of(new FlightCountryEntity("JP", "일본", "Japan")));

        flightCountryNameCache.refresh();

        assertThat(flightCountryNameCache.findName("JP")).isEqualTo("일본");
        assertThat(flightCountryNameCache.findName("ZZ")).isNull();
    }
}

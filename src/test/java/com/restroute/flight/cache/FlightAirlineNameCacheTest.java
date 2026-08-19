package com.restroute.flight.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.flight.domain.FlightAirlineEntity;
import com.restroute.flight.repository.FlightAirlineRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightAirlineNameCacheTest {

    @Mock
    private FlightAirlineRepository flightAirlineRepository;

    private FlightAirlineNameCache flightAirlineNameCache;

    @BeforeEach
    void setUp() {
        flightAirlineNameCache = new FlightAirlineNameCache(flightAirlineRepository);
    }

    @Test
    @DisplayName("refresh 전에는 조회 결과가 없다")
    void findName_returnsNull_beforeRefresh() {
        assertThat(flightAirlineNameCache.findName("7C")).isNull();
    }

    @Test
    @DisplayName("korName이 있으면 refresh 이후 korName을 반환한다")
    void findName_returnsKorName_whenPresent() {
        when(flightAirlineRepository.findAll())
                .thenReturn(List.of(new FlightAirlineEntity("7C", "제주항공", "Jeju Air", true)));

        flightAirlineNameCache.refresh();

        assertThat(flightAirlineNameCache.findName("7C")).isEqualTo("제주항공");
    }

    @Test
    @DisplayName("korName이 없으면 refresh 이후 engName으로 대체한다")
    void findName_fallsBackToEngName_whenKorNameMissing() {
        when(flightAirlineRepository.findAll())
                .thenReturn(List.of(new FlightAirlineEntity("OI", null, "Hinterland Aviation", false)));

        flightAirlineNameCache.refresh();

        assertThat(flightAirlineNameCache.findName("OI")).isEqualTo("Hinterland Aviation");
        assertThat(flightAirlineNameCache.findName("ZZ")).isNull();
    }

    @Test
    @DisplayName("refresh 전에는 저비용 여부 조회도 false다")
    void isLowCost_returnsFalse_beforeRefresh() {
        assertThat(flightAirlineNameCache.isLowCost("7C")).isFalse();
    }

    @Test
    @DisplayName("refresh 이후 코드별 저비용 여부를 반환한다")
    void isLowCost_returnsPerCodeValue_afterRefresh() {
        when(flightAirlineRepository.findAll())
                .thenReturn(List.of(
                        new FlightAirlineEntity("7C", "제주항공", "Jeju Air", true),
                        new FlightAirlineEntity("KE", "대한항공", "Korean Air", false)));

        flightAirlineNameCache.refresh();

        assertThat(flightAirlineNameCache.isLowCost("7C")).isTrue();
        assertThat(flightAirlineNameCache.isLowCost("KE")).isFalse();
    }

    @Test
    @DisplayName("소스에 없던 코드는 저비용 여부를 알 수 없다는 뜻으로 false다")
    void isLowCost_returnsFalse_whenCodeUnknown() {
        when(flightAirlineRepository.findAll())
                .thenReturn(List.of(new FlightAirlineEntity("7C", "제주항공", "Jeju Air", true)));

        flightAirlineNameCache.refresh();

        assertThat(flightAirlineNameCache.isLowCost("ZZ")).isFalse();
    }
}

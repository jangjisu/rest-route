package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightSectorCountriesTest {

    @Test
    @DisplayName("JAPAN은 일본 한 나라로 펼쳐진다")
    void countriesOf_japan() {
        assertThat(FlightSectorCountries.countriesOf(List.of("JAPAN"))).containsExactly("JP");
    }

    @Test
    @DisplayName("SOUTHEAST_ASIA는 태국·베트남 두 나라로 펼쳐진다")
    void countriesOf_southeastAsia() {
        assertThat(FlightSectorCountries.countriesOf(List.of("SOUTHEAST_ASIA"))).containsExactlyInAnyOrder("TH", "VN");
    }

    @Test
    @DisplayName("GREATER_CHINA는 중국·대만·홍콩·마카오 네 나라로 펼쳐진다")
    void countriesOf_greaterChina() {
        assertThat(FlightSectorCountries.countriesOf(List.of("GREATER_CHINA")))
                .containsExactlyInAnyOrder("CN", "TW", "HK", "MO");
    }

    @Test
    @DisplayName("GUAM_SAIPAN은 괌·북마리아나제도 두 나라로 펼쳐진다")
    void countriesOf_guamSaipan() {
        assertThat(FlightSectorCountries.countriesOf(List.of("GUAM_SAIPAN"))).containsExactlyInAnyOrder("GU", "MP");
    }

    @Test
    @DisplayName("여러 sector를 동시에 주면 국가 목록을 합쳐서 반환한다")
    void countriesOf_mergesMultipleSectors() {
        assertThat(FlightSectorCountries.countriesOf(List.of("JAPAN", "GUAM_SAIPAN")))
                .containsExactlyInAnyOrder("JP", "GU", "MP");
    }

    @Test
    @DisplayName("sector가 없거나 비어있으면 빈 목록을 반환한다")
    void countriesOf_returnsEmptyWhenSectorMissing() {
        assertThat(FlightSectorCountries.countriesOf(null)).isEmpty();
        assertThat(FlightSectorCountries.countriesOf(List.of())).isEmpty();
    }
}

package com.restroute.flight.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.domain.FlightCountryEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class FlightCountryRepositoryTest {

    @Autowired
    private FlightCountryRepository flightCountryRepository;

    @Test
    @DisplayName("국가 코드로 국가를 조회한다")
    void findByCode_returnsMatchingCountry() {
        flightCountryRepository.save(new FlightCountryEntity("JP", "일본", "Japan"));

        Optional<FlightCountryEntity> result = flightCountryRepository.findByCode("JP");

        assertThat(result).isPresent();
        assertThat(result.get().getKorName()).isEqualTo("일본");
    }

    @Test
    @DisplayName("존재하지 않는 코드는 빈 값을 반환한다")
    void findByCode_returnsEmptyWhenMissing() {
        Optional<FlightCountryEntity> result = flightCountryRepository.findByCode("ZZ");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("korName 부분 일치로 국가를 검색한다")
    void findAllByNameContaining_matchesKorName() {
        flightCountryRepository.saveAll(List.of(
                new FlightCountryEntity("JP", "일본", "Japan"), new FlightCountryEntity("KR", "대한민국", "South Korea")));

        List<FlightCountryEntity> result =
                flightCountryRepository
                        .findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc("본", "본");

        assertThat(result).extracting(FlightCountryEntity::getCode).containsExactly("JP");
    }
}

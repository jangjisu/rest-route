package com.restroute.flight.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.domain.FlightCityEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class FlightCityRepositoryTest {

    @Autowired
    private FlightCityRepository flightCityRepository;

    @Test
    @DisplayName("IATA 코드로 도시를 조회한다")
    void findByCode_returnsMatchingCity() {
        flightCityRepository.save(new FlightCityEntity("OSA", "Osaka", "JP"));

        Optional<FlightCityEntity> result = flightCityRepository.findByCode("OSA");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Osaka");
    }

    @Test
    @DisplayName("존재하지 않는 코드는 빈 값을 반환한다")
    void findByCode_returnsEmptyWhenMissing() {
        Optional<FlightCityEntity> result = flightCityRepository.findByCode("ZZZ");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("이름 부분 일치로 도시를 가나다순 정렬해 조회한다")
    void findAllByNameContaining_returnsSortedMatches() {
        flightCityRepository.saveAll(List.of(
                new FlightCityEntity("FUK", "Fukuoka", "JP"),
                new FlightCityEntity("OSA", "Osaka", "JP"),
                new FlightCityEntity("BKK", "Bangkok", "TH")));

        List<FlightCityEntity> result = flightCityRepository.findAllByNameContainingIgnoreCaseOrderByNameAsc("o");

        assertThat(result).extracting(FlightCityEntity::getCode).containsExactly("BKK", "FUK", "OSA");
    }
}

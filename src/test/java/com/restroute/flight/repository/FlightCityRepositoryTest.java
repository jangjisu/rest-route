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
        flightCityRepository.save(new FlightCityEntity("OSA", "오사카", "Osaka", "JP"));

        Optional<FlightCityEntity> result = flightCityRepository.findByCode("OSA");

        assertThat(result).isPresent();
        assertThat(result.get().getKorName()).isEqualTo("오사카");
    }

    @Test
    @DisplayName("존재하지 않는 코드는 빈 값을 반환한다")
    void findByCode_returnsEmptyWhenMissing() {
        Optional<FlightCityEntity> result = flightCityRepository.findByCode("ZZZ");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("korName 부분 일치로 도시를 검색한다")
    void findAllByNameContaining_matchesKorName() {
        flightCityRepository.saveAll(List.of(
                new FlightCityEntity("FUK", "후쿠오카", "Fukuoka", "JP"),
                new FlightCityEntity("OSA", "오사카", "Osaka", "JP"),
                new FlightCityEntity("BKK", "방콕", "Bangkok", "TH")));

        List<FlightCityEntity> result =
                flightCityRepository.findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                        "오사카", "오사카");

        assertThat(result).extracting(FlightCityEntity::getCode).containsExactly("OSA");
    }

    @Test
    @DisplayName("korName이 없는 도시도 engName 부분 일치로 검색된다")
    void findAllByNameContaining_matchesEngNameWhenKorNameMissing() {
        flightCityRepository.saveAll(List.of(
                new FlightCityEntity("YHR", null, "Chevery", "CA"), new FlightCityEntity("OSA", "오사카", "Osaka", "JP")));

        List<FlightCityEntity> result =
                flightCityRepository.findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                        "chevery", "chevery");

        assertThat(result).extracting(FlightCityEntity::getCode).containsExactly("YHR");
    }
}

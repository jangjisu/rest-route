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
        flightCityRepository.save(new FlightCityEntity("OSA", "Osaka", "오사카", "JP", "일본", "JAPAN"));

        Optional<FlightCityEntity> result = flightCityRepository.findByCode("OSA");

        assertThat(result).isPresent();
        assertThat(result.get().getNameKo()).isEqualTo("오사카");
    }

    @Test
    @DisplayName("존재하지 않는 코드는 빈 값을 반환한다")
    void findByCode_returnsEmptyWhenMissing() {
        Optional<FlightCityEntity> result = flightCityRepository.findByCode("ZZZ");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("지역권으로 도시를 가나다순 정렬해 조회한다")
    void findAllByRegionGroupOrderByNameKoAsc_returnsSortedMatches() {
        flightCityRepository.saveAll(List.of(
                new FlightCityEntity("FUK", "Fukuoka", "후쿠오카", "JP", "일본", "JAPAN"),
                new FlightCityEntity("OSA", "Osaka", "오사카", "JP", "일본", "JAPAN"),
                new FlightCityEntity("BKK", "Bangkok", "방콕", "TH", "태국", "SOUTHEAST_ASIA")));

        List<FlightCityEntity> result = flightCityRepository.findAllByRegionGroupOrderByNameKoAsc("JAPAN");

        assertThat(result).extracting(FlightCityEntity::getCode).containsExactly("OSA", "FUK");
    }

    @Test
    @DisplayName("영문/한글 이름 부분 일치로 도시를 검색한다")
    void findAllByNameContaining_matchesEnglishOrKoreanName() {
        flightCityRepository.save(new FlightCityEntity("OSA", "Osaka", "오사카", "JP", "일본", "JAPAN"));

        List<FlightCityEntity> byKorean =
                flightCityRepository.findAllByNameContainingIgnoreCaseOrNameKoContainingIgnoreCaseOrderByNameKoAsc(
                        "오사", "오사");
        List<FlightCityEntity> byEnglish =
                flightCityRepository.findAllByNameContainingIgnoreCaseOrNameKoContainingIgnoreCaseOrderByNameKoAsc(
                        "osa", "osa");

        assertThat(byKorean).extracting(FlightCityEntity::getCode).containsExactly("OSA");
        assertThat(byEnglish).extracting(FlightCityEntity::getCode).containsExactly("OSA");
    }
}

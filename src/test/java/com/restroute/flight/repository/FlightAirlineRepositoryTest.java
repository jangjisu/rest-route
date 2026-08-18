package com.restroute.flight.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.domain.FlightAirlineEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class FlightAirlineRepositoryTest {

    @Autowired
    private FlightAirlineRepository flightAirlineRepository;

    @Test
    @DisplayName("IATA 코드로 항공사를 조회한다")
    void findByCode_returnsMatchingAirline() {
        flightAirlineRepository.save(new FlightAirlineEntity("7C", "제주항공", "Jeju Air", true));

        Optional<FlightAirlineEntity> result = flightAirlineRepository.findByCode("7C");

        assertThat(result).isPresent();
        assertThat(result.get().getKorName()).isEqualTo("제주항공");
    }

    @Test
    @DisplayName("존재하지 않는 코드는 빈 값을 반환한다")
    void findByCode_returnsEmptyWhenMissing() {
        Optional<FlightAirlineEntity> result = flightAirlineRepository.findByCode("ZZ");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("korName 부분 일치로 항공사를 검색한다")
    void findAllByNameContaining_matchesKorName() {
        flightAirlineRepository.saveAll(List.of(
                new FlightAirlineEntity("7C", "제주항공", "Jeju Air", true),
                new FlightAirlineEntity("KE", "대한항공", "Korean Air", false)));

        List<FlightAirlineEntity> result =
                flightAirlineRepository
                        .findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc("제주", "제주");

        assertThat(result).extracting(FlightAirlineEntity::getCode).containsExactly("7C");
    }

    @Test
    @DisplayName("korName이 없는 항공사도 engName 부분 일치로 검색된다")
    void findAllByNameContaining_matchesEngNameWhenKorNameMissing() {
        flightAirlineRepository.saveAll(List.of(
                new FlightAirlineEntity("OI", null, "Hinterland Aviation", false),
                new FlightAirlineEntity("7C", "제주항공", "Jeju Air", true)));

        List<FlightAirlineEntity> result =
                flightAirlineRepository
                        .findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                                "hinterland", "hinterland");

        assertThat(result).extracting(FlightAirlineEntity::getCode).containsExactly("OI");
    }
}

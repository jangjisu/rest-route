package com.restroute.flight.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.domain.FlightAirportEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class FlightAirportRepositoryTest {

    @Autowired
    private FlightAirportRepository flightAirportRepository;

    @Test
    @DisplayName("IATA 공항코드로 공항을 조회한다")
    void findByCode_returnsMatchingAirport() {
        flightAirportRepository.save(
                new FlightAirportEntity("ICN", "인천국제공항", "Incheon International Airport", "SEL", "KR"));

        Optional<FlightAirportEntity> result = flightAirportRepository.findByCode("ICN");

        assertThat(result).isPresent();
        assertThat(result.get().getKorName()).isEqualTo("인천국제공항");
    }

    @Test
    @DisplayName("존재하지 않는 코드는 빈 값을 반환한다")
    void findByCode_returnsEmptyWhenMissing() {
        Optional<FlightAirportEntity> result = flightAirportRepository.findByCode("ZZZ");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("korName 부분 일치로 공항을 검색한다")
    void findAllByNameContaining_matchesKorName() {
        flightAirportRepository.saveAll(List.of(
                new FlightAirportEntity("ICN", "인천국제공항", "Incheon International Airport", "SEL", "KR"),
                new FlightAirportEntity("GMP", "김포국제공항", "Gimpo International Airport", "SEL", "KR")));

        List<FlightAirportEntity> result =
                flightAirportRepository
                        .findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc("인천", "인천");

        assertThat(result).extracting(FlightAirportEntity::getCode).containsExactly("ICN");
    }

    @Test
    @DisplayName("korName이 없는 공항도 engName 부분 일치로 검색된다")
    void findAllByNameContaining_matchesEngNameWhenKorNameMissing() {
        flightAirportRepository.saveAll(List.of(
                new FlightAirportEntity("MBE", null, "Monbetsu Airport", "MBE", "JP"),
                new FlightAirportEntity("ICN", "인천국제공항", "Incheon International Airport", "SEL", "KR")));

        List<FlightAirportEntity> result =
                flightAirportRepository
                        .findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                                "monbetsu", "monbetsu");

        assertThat(result).extracting(FlightAirportEntity::getCode).containsExactly("MBE");
    }
}

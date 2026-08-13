package com.restroute.flight.repository;

import com.restroute.flight.domain.FlightAirportEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightAirportRepository extends JpaRepository<FlightAirportEntity, Long> {

    Optional<FlightAirportEntity> findByCode(String code);

    List<FlightAirportEntity> findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
            String korNameKeyword, String engNameKeyword);
}

package com.restroute.flight.repository;

import com.restroute.flight.domain.FlightAirlineEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightAirlineRepository extends JpaRepository<FlightAirlineEntity, Long> {

    Optional<FlightAirlineEntity> findByCode(String code);

    List<FlightAirlineEntity> findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
            String korNameKeyword, String engNameKeyword);
}

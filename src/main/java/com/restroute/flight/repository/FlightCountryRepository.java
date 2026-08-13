package com.restroute.flight.repository;

import com.restroute.flight.domain.FlightCountryEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightCountryRepository extends JpaRepository<FlightCountryEntity, Long> {

    Optional<FlightCountryEntity> findByCode(String code);

    List<FlightCountryEntity> findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
            String korNameKeyword, String engNameKeyword);
}

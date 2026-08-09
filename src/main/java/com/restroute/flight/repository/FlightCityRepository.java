package com.restroute.flight.repository;

import com.restroute.flight.domain.FlightCityEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightCityRepository extends JpaRepository<FlightCityEntity, Long> {

    Optional<FlightCityEntity> findByCode(String code);

    List<FlightCityEntity> findAllByRegionGroupOrderByNameKoAsc(String regionGroup);

    List<FlightCityEntity> findAllByNameContainingIgnoreCaseOrNameKoContainingIgnoreCaseOrderByNameKoAsc(
            String name, String nameKo);
}

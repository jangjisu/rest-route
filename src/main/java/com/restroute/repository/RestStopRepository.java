package com.restroute.repository;

import com.restroute.domain.RestStopEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestStopRepository extends JpaRepository<RestStopEntity, Long> {

    Optional<RestStopEntity> findByServiceAreaCode(String serviceAreaCode);

    boolean existsByServiceAreaCode(String serviceAreaCode);

    List<RestStopEntity> findByUnitNameContainingIgnoreCase(String unitName);

    List<RestStopEntity> findAllByServiceAreaCodeIn(Collection<String> serviceAreaCodes);
}

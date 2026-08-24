package com.restroute.evcharger.repository;

import com.restroute.evcharger.domain.EvChargerStationMappingEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvChargerStationMappingRepository extends JpaRepository<EvChargerStationMappingEntity, Long> {

    List<EvChargerStationMappingEntity> findAllByRestStopServiceAreaCodeIn(Collection<String> serviceAreaCodes);

    List<EvChargerStationMappingEntity> findAllByStatIdIn(Collection<String> statIds);

    void deleteAllByStatIdNotIn(Collection<String> statIds);
}

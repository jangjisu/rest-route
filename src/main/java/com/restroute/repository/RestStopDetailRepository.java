package com.restroute.repository;

import com.restroute.domain.RestStopDetailEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestStopDetailRepository extends JpaRepository<RestStopDetailEntity, Long> {

    Optional<RestStopDetailEntity> findByServiceAreaCode(String serviceAreaCode);

    Optional<RestStopDetailEntity> findByRestStopServiceAreaCode(String restStopServiceAreaCode);

    List<RestStopDetailEntity> findAllByRestStopServiceAreaCodeIn(Collection<String> restStopServiceAreaCodes);
}

package com.restroute.repository;

import com.restroute.domain.RestEventEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestEventRepository extends JpaRepository<RestEventEntity, Long> {

    List<RestEventEntity> findAllByRestStopServiceAreaCodeOrderByIdAsc(String restStopServiceAreaCode);

    List<RestEventEntity> findAllByRestStopServiceAreaCodeIn(Collection<String> serviceAreaCodes);
}

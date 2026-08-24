package com.restroute.reststopcontent.repository;

import com.restroute.reststopcontent.domain.RestEventEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestEventRepository extends JpaRepository<RestEventEntity, Long> {

    List<RestEventEntity> findAllByRestStopServiceAreaCodeOrderByIdAsc(String restStopServiceAreaCode);

    List<RestEventEntity> findAllByRestStopServiceAreaCodeIn(Collection<String> serviceAreaCodes);
}

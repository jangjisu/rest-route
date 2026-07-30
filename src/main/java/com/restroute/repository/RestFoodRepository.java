package com.restroute.repository;

import com.restroute.domain.RestFoodEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestFoodRepository extends JpaRepository<RestFoodEntity, Long> {

    List<RestFoodEntity> findAllByStdRestCdOrderByIdAsc(String stdRestCd);

    List<RestFoodEntity> findAllByRestStopServiceAreaCodeOrderByIdAsc(String restStopServiceAreaCode);

    Optional<RestFoodEntity> findByIdAndRestStopServiceAreaCode(Long id, String restStopServiceAreaCode);

    List<RestFoodEntity> findAllByRestStopServiceAreaCodeIn(Collection<String> restStopServiceAreaCodes);
}

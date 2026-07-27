package com.restroute.repository;

import com.restroute.domain.RestOilPriceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestOilPriceRepository extends JpaRepository<RestOilPriceEntity, Long> {

    Optional<RestOilPriceEntity> findByServiceAreaCode2(String serviceAreaCode2);

    List<RestOilPriceEntity> findAllByRestStopServiceAreaCodeOrderByIdAsc(String restStopServiceAreaCode);

    List<RestOilPriceEntity> findAllByServiceAreaNameContainingIgnoreCaseOrderByIdAsc(String serviceAreaName);

    List<RestOilPriceEntity> findAllByRouteNameOrderByIdAsc(String routeName);

    List<RestOilPriceEntity> findAllByRouteNameAndServiceAreaNameContainingIgnoreCaseOrderByIdAsc(
            String routeName, String serviceAreaName);
}

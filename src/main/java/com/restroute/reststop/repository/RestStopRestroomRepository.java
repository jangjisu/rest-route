package com.restroute.reststop.repository;

import com.restroute.reststop.domain.RestStopRestroomEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestStopRestroomRepository extends JpaRepository<RestStopRestroomEntity, Long> {

    Optional<RestStopRestroomEntity> findByRestStopServiceAreaCode(String restStopServiceAreaCode);

    List<RestStopRestroomEntity> findAllBySourceRestStopNameContainingIgnoreCaseOrderByIdAsc(String sourceRestStopName);

    List<RestStopRestroomEntity> findAllByRouteNameOrderByIdAsc(String routeName);

    List<RestStopRestroomEntity> findAllByRouteNameAndSourceRestStopNameContainingIgnoreCaseOrderByIdAsc(
            String routeName, String sourceRestStopName);
}

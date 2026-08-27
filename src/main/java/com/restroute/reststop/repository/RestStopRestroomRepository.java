package com.restroute.reststop.repository;

import com.restroute.reststop.domain.RestStopRestroomEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestStopRestroomRepository extends JpaRepository<RestStopRestroomEntity, Long> {

    Optional<RestStopRestroomEntity> findByRestStopServiceAreaCode(String restStopServiceAreaCode);
}

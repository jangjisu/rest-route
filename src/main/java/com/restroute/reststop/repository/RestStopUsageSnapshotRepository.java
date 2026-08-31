package com.restroute.reststop.repository;

import com.restroute.reststop.domain.RestStopUsageSnapshotEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestStopUsageSnapshotRepository extends JpaRepository<RestStopUsageSnapshotEntity, Long> {

    Optional<RestStopUsageSnapshotEntity> findByRestStopServiceAreaCode(String restStopServiceAreaCode);

    List<RestStopUsageSnapshotEntity> findAllByRestStopServiceAreaCodeIn(Collection<String> restStopServiceAreaCodes);
}

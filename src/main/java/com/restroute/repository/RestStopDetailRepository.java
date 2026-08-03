package com.restroute.repository;

import com.restroute.domain.RestStopDetailEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestStopDetailRepository extends JpaRepository<RestStopDetailEntity, Long> {

    Optional<RestStopDetailEntity> findByServiceAreaCode(String serviceAreaCode);

    Optional<RestStopDetailEntity> findByRestStopServiceAreaCode(String restStopServiceAreaCode);

    List<RestStopDetailEntity> findAllByRestStopServiceAreaCodeIn(Collection<String> restStopServiceAreaCodes);

    @Query("""
            select e from RestStopDetailEntity e
            where (:serviceAreaCodes is null or e.restStopServiceAreaCode in :serviceAreaCodes)
            and (:adminOverridden is null or e.adminOverridden = :adminOverridden)
            """)
    List<RestStopDetailEntity> findByRestStopServiceAreaCodesAndAdminOverridden(
            @Param("serviceAreaCodes") Collection<String> serviceAreaCodes,
            @Param("adminOverridden") Boolean adminOverridden);
}

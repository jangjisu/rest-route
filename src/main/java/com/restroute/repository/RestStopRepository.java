package com.restroute.repository;

import com.restroute.domain.RestStopEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestStopRepository extends JpaRepository<RestStopEntity, Long> {

    Optional<RestStopEntity> findByServiceAreaCode(String serviceAreaCode);

    boolean existsByServiceAreaCode(String serviceAreaCode);

    List<RestStopEntity> findByUnitNameContainingIgnoreCase(String unitName);

    List<RestStopEntity> findAllByServiceAreaCodeIn(Collection<String> serviceAreaCodes);

    @Query("""
            select r from RestStopEntity r
            where (:serviceAreaCodes is null or r.serviceAreaCode in :serviceAreaCodes)
            and (:adminOverridden is null or r.adminOverridden = :adminOverridden)
            """)
    List<RestStopEntity> findByServiceAreaCodesAndAdminOverridden(
            @Param("serviceAreaCodes") Collection<String> serviceAreaCodes,
            @Param("adminOverridden") Boolean adminOverridden);
}

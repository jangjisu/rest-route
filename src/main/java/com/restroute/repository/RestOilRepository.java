package com.restroute.repository;

import com.restroute.domain.RestOilEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestOilRepository extends JpaRepository<RestOilEntity, Long> {

    List<RestOilEntity> findAllByRouteCodeAndNormalizedStationNameOrderByIdAsc(
            String routeCode, String normalizedStationName);

    List<RestOilEntity> findAllByRestStopServiceAreaCodeOrderByIdAsc(String restStopServiceAreaCode);

    List<RestOilEntity> findAllByStandardRestNameContainingIgnoreCaseOrderByIdAsc(String standardRestName);

    List<RestOilEntity> findAllByStandardRestCodeOrderByIdAsc(String standardRestCode);

    List<RestOilEntity> findAllByRestStopServiceAreaCodeIn(Collection<String> restStopServiceAreaCodes);

    @Query("""
            select e from RestOilEntity e
            where (:serviceAreaCodes is null or e.restStopServiceAreaCode in :serviceAreaCodes)
            and (:adminOverridden is null or e.adminOverridden = :adminOverridden)
            """)
    List<RestOilEntity> findByRestStopServiceAreaCodesAndAdminOverridden(
            @Param("serviceAreaCodes") Collection<String> serviceAreaCodes,
            @Param("adminOverridden") Boolean adminOverridden);
}

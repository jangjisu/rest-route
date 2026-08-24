package com.restroute.reststopcontent.repository;

import com.restroute.reststopcontent.domain.RestFoodEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestFoodRepository extends JpaRepository<RestFoodEntity, Long> {

    List<RestFoodEntity> findAllByStdRestCdOrderByIdAsc(String stdRestCd);

    List<RestFoodEntity> findAllByRestStopServiceAreaCodeOrderByIdAsc(String restStopServiceAreaCode);

    Optional<RestFoodEntity> findByIdAndRestStopServiceAreaCode(Long id, String restStopServiceAreaCode);

    List<RestFoodEntity> findAllByRestStopServiceAreaCodeIn(Collection<String> restStopServiceAreaCodes);

    @Query("""
            select e from RestFoodEntity e
            where (:serviceAreaCodes is null or e.restStopServiceAreaCode in :serviceAreaCodes)
            and (:adminOverridden is null or e.adminOverridden = :adminOverridden)
            """)
    List<RestFoodEntity> findByRestStopServiceAreaCodesAndAdminOverridden(
            @Param("serviceAreaCodes") Collection<String> serviceAreaCodes,
            @Param("adminOverridden") Boolean adminOverridden);
}

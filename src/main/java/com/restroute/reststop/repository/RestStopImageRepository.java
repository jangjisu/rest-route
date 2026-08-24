package com.restroute.reststop.repository;

import com.restroute.reststop.domain.RestStopImageEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestStopImageRepository extends JpaRepository<RestStopImageEntity, String> {

    @Query("select image.detailImageData from RestStopImageEntity image "
            + "where image.serviceAreaCode = :serviceAreaCode")
    Optional<byte[]> findDetailImageDataByServiceAreaCode(@Param("serviceAreaCode") String serviceAreaCode);

    @Query("select image.listImageData from RestStopImageEntity image "
            + "where image.serviceAreaCode = :serviceAreaCode")
    Optional<byte[]> findListImageDataByServiceAreaCode(@Param("serviceAreaCode") String serviceAreaCode);

    @Query("select image.serviceAreaCode from RestStopImageEntity image "
            + "where image.serviceAreaCode in :serviceAreaCodes")
    List<String> findServiceAreaCodesIn(@Param("serviceAreaCodes") Collection<String> serviceAreaCodes);
}

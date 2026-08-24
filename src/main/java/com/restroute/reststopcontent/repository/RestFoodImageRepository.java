package com.restroute.reststopcontent.repository;

import com.restroute.reststopcontent.domain.RestFoodImageEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestFoodImageRepository extends JpaRepository<RestFoodImageEntity, Long> {

    @Query("select r.foodId from RestFoodImageEntity r where r.foodId in :foodIds")
    List<Long> findAllFoodIdsIn(@Param("foodIds") List<Long> foodIds);

    @Query("select r.listImageData from RestFoodImageEntity r where r.foodId = :foodId")
    Optional<byte[]> findListImageDataByFoodId(@Param("foodId") Long foodId);
}

package com.restroute.reststop.repository;

import com.restroute.reststop.domain.RestStopProductSalesRankEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestStopProductSalesRankRepository extends JpaRepository<RestStopProductSalesRankEntity, Long> {

    Optional<RestStopProductSalesRankEntity> findTopByOrderByBaseYearMonthDesc();

    @Query("""
            select product
            from RestStopProductSalesRankEntity product
            where product.restStopServiceAreaCode = :serviceAreaCode
              and product.productName is not null
              and product.productName <> ''
            order by product.baseYearMonth desc
            """)
    List<RestStopProductSalesRankEntity> findAllMappedProductsOrderByLatestMonth(
            @Param("serviceAreaCode") String restStopServiceAreaCode);
}

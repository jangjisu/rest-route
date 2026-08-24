package com.restroute.reststop.repository;

import com.restroute.reststop.domain.RestStopStoreSalesRankEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestStopStoreSalesRankRepository extends JpaRepository<RestStopStoreSalesRankEntity, Long> {

    Optional<RestStopStoreSalesRankEntity> findTopByOrderByBaseYearMonthDesc();

    @Query("""
            select store
            from RestStopStoreSalesRankEntity store
            where store.restStopServiceAreaCode = :serviceAreaCode
              and store.sourceStoreName is not null
              and store.sourceStoreName <> ''
            order by store.baseYearMonth desc
            """)
    List<RestStopStoreSalesRankEntity> findAllMappedStoresOrderByLatestMonth(
            @Param("serviceAreaCode") String restStopServiceAreaCode);
}

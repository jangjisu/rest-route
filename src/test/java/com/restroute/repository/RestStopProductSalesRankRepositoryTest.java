package com.restroute.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.domain.RestStopProductSalesRankEntity;
import com.restroute.service.salesranking.dto.SalesRankingProductRow;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RestStopProductSalesRankRepositoryTest {

    @Autowired
    private RestStopProductSalesRankRepository repository;

    @Test
    void savesProductSalesRank() {
        RestStopProductSalesRankEntity entity = RestStopProductSalesRankEntity.from(
                new SalesRankingProductRow("2026-06", "1", "S000001", "휴게소", "M001", "매장", "P001", "상품"));

        RestStopProductSalesRankEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findsLatestProductSalesRankMonth() {
        repository.save(RestStopProductSalesRankEntity.from(
                new SalesRankingProductRow("2026-05", "1", "S1", "휴게소", "M1", "매장", "P1", "상품")));
        repository.save(RestStopProductSalesRankEntity.from(
                new SalesRankingProductRow("2026-06", "1", "S1", "휴게소", "M1", "매장", "P2", "상품")));

        Optional<RestStopProductSalesRankEntity> latest = repository.findTopByOrderByBaseYearMonthDesc();

        assertThat(latest)
                .isPresent()
                .get()
                .extracting(RestStopProductSalesRankEntity::getBaseYearMonth)
                .isEqualTo("2026-06");
    }
}

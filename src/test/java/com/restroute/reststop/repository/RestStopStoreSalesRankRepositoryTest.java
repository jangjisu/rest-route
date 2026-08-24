package com.restroute.reststop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.reststop.domain.RestStopStoreSalesRankEntity;
import com.restroute.reststop.service.salesranking.dto.SalesRankingStoreRow;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RestStopStoreSalesRankRepositoryTest {

    @Autowired
    private RestStopStoreSalesRankRepository repository;

    @Test
    void savesStoreSalesRank() {
        RestStopStoreSalesRankEntity entity = RestStopStoreSalesRankEntity.from(
                new SalesRankingStoreRow("2026-06", "1", "1", "S000001", "휴게소", "M001", "매장"));

        RestStopStoreSalesRankEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findsLatestStoreSalesRankMonth() {
        repository.save(RestStopStoreSalesRankEntity.from(
                new SalesRankingStoreRow("2026-05", "1", "1", "S1", "휴게소", "M1", "매장")));
        repository.save(RestStopStoreSalesRankEntity.from(
                new SalesRankingStoreRow("2026-06", "1", "1", "S1", "휴게소", "M1", "매장2")));

        Optional<RestStopStoreSalesRankEntity> latest = repository.findTopByOrderByBaseYearMonthDesc();

        assertThat(latest)
                .isPresent()
                .get()
                .extracting(RestStopStoreSalesRankEntity::getBaseYearMonth)
                .isEqualTo("2026-06");
    }
}

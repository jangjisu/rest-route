package com.restroute.service.backfill;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.domain.RestStopEntity;
import com.restroute.domain.RestStopStoreSalesRankEntity;
import com.restroute.repository.RestStopStoreSalesRankRepository;
import com.restroute.service.salesranking.SalesRankingStoreRow;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestStopStoreSalesRankBackfillerTest {

    @Mock
    private RestStopStoreSalesRankRepository storeSalesRankRepository;

    private RestStopStoreSalesRankBackfiller backfiller;

    @BeforeEach
    void setUp() {
        backfiller = new RestStopStoreSalesRankBackfiller(storeSalesRankRepository);
    }

    @Test
    @DisplayName("비어 있는 매핑만 유일한 휴게소명으로 연결하고 개수를 센다")
    void backfill_mapsOnlyUnmappedRanksByUniqueName() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopStoreSalesRankEntity unmapped = RestStopStoreSalesRankEntity.from(
                new SalesRankingStoreRow("2026-06", "1", "1", "S000001", "서울만남(부산)휴게소", "M1", "매장"));
        RestStopStoreSalesRankEntity alreadyMapped = RestStopStoreSalesRankEntity.from(
                new SalesRankingStoreRow("2026-06", "2", "2", "S000002", "서울만남(부산)휴게소", "M1", "매장"));
        alreadyMapped.updateRestStopServiceAreaCode("MANUAL");
        when(storeSalesRankRepository.findAll()).thenReturn(List.of(unmapped, alreadyMapped));

        int mappedCount = backfiller.backfill(List.of(restStop));

        assertThat(mappedCount).isEqualTo(1);
        assertThat(unmapped.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(alreadyMapped.getRestStopServiceAreaCode()).isEqualTo("MANUAL");
    }

    @Test
    @DisplayName("이름이 모호하게 여러 휴게소와 일치하면 건드리지 않는다")
    void backfill_skipsAmbiguousNames() {
        RestStopEntity first = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소", "A00001"));
        RestStopEntity second = RestStopEntity.from(restStopItem("002", "서울만남(부산)휴게소", "A00002"));
        RestStopStoreSalesRankEntity ambiguous = RestStopStoreSalesRankEntity.from(
                new SalesRankingStoreRow("2026-06", "1", "1", "S000001", "서울만남(부산)휴게소", "M1", "매장"));
        when(storeSalesRankRepository.findAll()).thenReturn(List.of(ambiguous));

        int mappedCount = backfiller.backfill(List.of(first, second));

        assertThat(mappedCount).isZero();
        assertThat(ambiguous.getRestStopServiceAreaCode()).isEmpty();
    }
}

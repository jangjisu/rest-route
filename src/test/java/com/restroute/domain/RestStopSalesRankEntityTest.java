package com.restroute.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.service.salesranking.dto.SalesRankingProductRow;
import com.restroute.service.salesranking.dto.SalesRankingStoreRow;
import org.junit.jupiter.api.Test;

class RestStopSalesRankEntityTest {

    @Test
    void newProductRankStartsUnmappedAndKeepsExistingMappingOnUpdate() {
        RestStopProductSalesRankEntity entity = RestStopProductSalesRankEntity.from(
                new SalesRankingProductRow("2026-06", "1", "S1", "휴게소", "M1", "매장", "P1", "상품"));
        entity.updateRestStopServiceAreaCode("A00001");
        entity.updateFrom(new SalesRankingProductRow("2026-06", "2", "S1", "휴게소", "M1", "매장", "P1", "변경상품"));

        assertThat(entity.isUnmapped()).isFalse();
        assertThat(entity.isMapped()).isTrue();
        assertThat(entity.getProductName()).isEqualTo("변경상품");
        assertThat(entity.getRestStopServiceAreaCode()).isEqualTo("A00001");
    }

    @Test
    void newStoreRankStartsUnmapped() {
        RestStopStoreSalesRankEntity entity = RestStopStoreSalesRankEntity.from(
                new SalesRankingStoreRow("2026-06", "1", "1", "S1", "휴게소", "M1", "매장"));

        assertThat(entity.isUnmapped()).isTrue();
        assertThat(entity.isMapped()).isFalse();
        assertThat(entity.getRestStopServiceAreaCode()).isEmpty();
    }

    @Test
    void storeRankUpdateKeepsExistingMapping() {
        RestStopStoreSalesRankEntity entity = RestStopStoreSalesRankEntity.from(
                new SalesRankingStoreRow("2026-06", "1", "1", "S1", "휴게소", "M1", "매장"));
        entity.updateRestStopServiceAreaCode("A00001");

        entity.updateFrom(new SalesRankingStoreRow("2026-06", "2", "2", "S1", "휴게소", "M1", "변경매장"));

        assertThat(entity.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(entity.getSourceStoreName()).isEqualTo("변경매장");
    }
}

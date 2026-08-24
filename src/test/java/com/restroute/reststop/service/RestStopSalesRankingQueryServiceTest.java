package com.restroute.reststop.service;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopProductSalesRankEntity;
import com.restroute.reststop.domain.RestStopStoreSalesRankEntity;
import com.restroute.reststop.repository.RestStopProductSalesRankRepository;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.repository.RestStopStoreSalesRankRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestStopSalesRankingQueryServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopProductSalesRankRepository productSalesRankRepository;

    @Mock
    private RestStopStoreSalesRankRepository storeSalesRankRepository;

    private RestStopSalesRankingQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new RestStopSalesRankingQueryService(
                restStopRepository, productSalesRankRepository, storeSalesRankRepository);
    }

    @Test
    void returnsTopFiveProductsFromLatestMonthInRankOrder() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(productSalesRankRepository.findAllMappedProductsOrderByLatestMonth("A00001"))
                .thenReturn(List.of(
                        product("2026-06", "6", "여섯번째 메뉴"),
                        product("2026-06", "2", "두번째 메뉴"),
                        product("2026-05", "1", "이전 메뉴"),
                        product("2026-06", "1", "첫번째 메뉴"),
                        product("2026-06", "5", "다섯번째 메뉴"),
                        product("2026-06", "3", "세번째 메뉴"),
                        product("2026-06", "4", "네번째 메뉴")));
        when(storeSalesRankRepository.findAllMappedStoresOrderByLatestMonth("A00001"))
                .thenReturn(List.of());

        var result = queryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().baseYearMonth()).isEqualTo("2026-06");
        assertThat(result.get().products())
                .extracting("rank", "productName")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "첫번째 메뉴"),
                        org.assertj.core.groups.Tuple.tuple(2, "두번째 메뉴"),
                        org.assertj.core.groups.Tuple.tuple(3, "세번째 메뉴"),
                        org.assertj.core.groups.Tuple.tuple(4, "네번째 메뉴"),
                        org.assertj.core.groups.Tuple.tuple(5, "다섯번째 메뉴"));
    }

    @Test
    void returnsStoreAndProductRankingsFromTheSameLatestMonth() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(productSalesRankRepository.findAllMappedProductsOrderByLatestMonth("A00001"))
                .thenReturn(List.of(product("2026-06", "1", "대표 메뉴")));
        when(storeSalesRankRepository.findAllMappedStoresOrderByLatestMonth("A00001"))
                .thenReturn(List.of(
                        store("2026-06", "2", "두번째 매장"),
                        store("2026-06", "1", "첫번째 매장"),
                        store("2026-05", "1", "이전 매장")));

        var result = queryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().storeRankings())
                .extracting("rank", "storeName")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "첫번째 매장"),
                        org.assertj.core.groups.Tuple.tuple(2, "두번째 매장"));
    }

    @Test
    void returnsStoreRankingsWhenProductRankingsAreUnavailable() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(productSalesRankRepository.findAllMappedProductsOrderByLatestMonth("A00001"))
                .thenReturn(List.of());
        when(storeSalesRankRepository.findAllMappedStoresOrderByLatestMonth("A00001"))
                .thenReturn(List.of(store("2026-06", "1", "CU편의점")));

        var result = queryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().products()).isEmpty();
        assertThat(result.get().storeRankings()).hasSize(1);
    }

    @Test
    void returnsEmptyDataWhenSalesRankingDoesNotExist() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(productSalesRankRepository.findAllMappedProductsOrderByLatestMonth("A00001"))
                .thenReturn(List.of());
        when(storeSalesRankRepository.findAllMappedStoresOrderByLatestMonth("A00001"))
                .thenReturn(List.of());

        var result = queryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().baseYearMonth()).isNull();
        assertThat(result.get().products()).isEmpty();
    }

    @Test
    void ignoresBlankProductNamesAndInvalidRanks() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(productSalesRankRepository.findAllMappedProductsOrderByLatestMonth("A00001"))
                .thenReturn(List.of(product("2026-06", "1", " "), product("2026-06", "순위없음", "메뉴")));
        when(storeSalesRankRepository.findAllMappedStoresOrderByLatestMonth("A00001"))
                .thenReturn(List.of());

        var result = queryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().products()).isEmpty();
    }

    @Test
    void returnsEmptyWhenRestStopDoesNotExist() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThat(queryService.findByServiceAreaCode("UNKNOWN")).isEmpty();
    }

    @Test
    void returnsEmptyForBlankServiceAreaCode() {
        assertThat(queryService.findByServiceAreaCode("  ")).isEmpty();
    }

    @Test
    void excludesProductsWithInvalidRankingFields() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(productSalesRankRepository.findAllMappedProductsOrderByLatestMonth("A00001"))
                .thenReturn(List.of(
                        product("", "1", "기준월 없음"),
                        product("2026-06", "", "순위 없음"),
                        product("2026-06", "0", "0순위"),
                        product("2026-06", "문자열", "문자열 순위")));
        when(storeSalesRankRepository.findAllMappedStoresOrderByLatestMonth("A00001"))
                .thenReturn(List.of());

        var result = queryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().baseYearMonth()).isNull();
        assertThat(result.get().products()).isEmpty();
    }

    private RestStopProductSalesRankEntity product(String month, String rank, String name) {
        var row = new com.restroute.reststop.service.salesranking.dto.SalesRankingProductRow(
                month, rank, "S000001", "휴게소", "M001", "매장", "P001", name);
        RestStopProductSalesRankEntity product = RestStopProductSalesRankEntity.from(row);
        product.updateRestStopServiceAreaCode("A00001");
        return product;
    }

    private RestStopStoreSalesRankEntity store(String month, String rank, String name) {
        var row = new com.restroute.reststop.service.salesranking.dto.SalesRankingStoreRow(
                month, rank, rank, "S000001", "휴게소", "M001", name);
        RestStopStoreSalesRankEntity store = RestStopStoreSalesRankEntity.from(row);
        store.updateRestStopServiceAreaCode("A00001");
        return store;
    }
}

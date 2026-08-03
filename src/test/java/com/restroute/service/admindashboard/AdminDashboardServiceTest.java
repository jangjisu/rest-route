package com.restroute.service.admindashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.domain.AdminActivityLogEntity;
import com.restroute.domain.RestStopProductSalesRankEntity;
import com.restroute.domain.RestStopStoreSalesRankEntity;
import com.restroute.repository.RestStopProductSalesRankRepository;
import com.restroute.repository.RestStopRepository;
import com.restroute.repository.RestStopStoreSalesRankRepository;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admindashboard.dto.AdminActivityLogItemResponse;
import com.restroute.service.admindashboard.dto.AdminDashboardSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopProductSalesRankRepository productRepository;

    @Mock
    private RestStopStoreSalesRankRepository storeRepository;

    @Mock
    private AdminActivityLogService adminActivityLogService;

    private AdminDashboardService service;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardService(
                restStopRepository, productRepository, storeRepository, adminActivityLogService);
    }

    @Test
    void returnsRestStopCountAndLatestMonthAcrossBothRankingTables() {
        when(restStopRepository.count()).thenReturn(203L);
        when(productRepository.findTopByOrderByBaseYearMonthDesc()).thenReturn(Optional.of(product("2026-05")));
        when(storeRepository.findTopByOrderByBaseYearMonthDesc()).thenReturn(Optional.of(store("2026-06")));
        when(adminActivityLogService.findRecent()).thenReturn(List.of());

        assertThat(service.getSummary()).isEqualTo(new AdminDashboardSummary(203L, "2026-06", "준비중", List.of()));
    }

    @Test
    void returnsNullLatestMonthWhenRankingTablesAreEmpty() {
        when(restStopRepository.count()).thenReturn(0L);
        when(productRepository.findTopByOrderByBaseYearMonthDesc()).thenReturn(Optional.empty());
        when(storeRepository.findTopByOrderByBaseYearMonthDesc()).thenReturn(Optional.empty());
        when(adminActivityLogService.findRecent()).thenReturn(List.of());

        assertThat(service.getSummary()).isEqualTo(new AdminDashboardSummary(0L, null, "준비중", List.of()));
    }

    @Test
    void mapsRecentActivityLogEntriesToResponseItems() {
        when(restStopRepository.count()).thenReturn(0L);
        when(productRepository.findTopByOrderByBaseYearMonthDesc()).thenReturn(Optional.empty());
        when(storeRepository.findTopByOrderByBaseYearMonthDesc()).thenReturn(Optional.empty());
        AdminActivityLogEntity entity = AdminActivityLogEntity.of(
                "admin", "상품 판매순위 CSV(product.csv)를 업로드했습니다.", LocalDateTime.of(2026, 7, 21, 15, 32));
        when(adminActivityLogService.findRecent()).thenReturn(List.of(entity));

        List<AdminActivityLogItemResponse> logs = service.getSummary().recentActivityLogs();

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).actor()).isEqualTo("admin");
        assertThat(logs.get(0).message()).isEqualTo("상품 판매순위 CSV(product.csv)를 업로드했습니다.");
        assertThat(logs.get(0).occurredAt()).isEqualTo("2026-07-21 15:32");
    }

    private RestStopProductSalesRankEntity product(String month) {
        return RestStopProductSalesRankEntity.from(new com.restroute.service.salesranking.dto.SalesRankingProductRow(
                month, "1", "S1", "휴게소", "M1", "매장", "P1", "상품"));
    }

    private RestStopStoreSalesRankEntity store(String month) {
        return RestStopStoreSalesRankEntity.from(new com.restroute.service.salesranking.dto.SalesRankingStoreRow(
                month, "1", "1", "S1", "휴게소", "M1", "매장"));
    }
}

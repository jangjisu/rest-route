package com.restroute.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.service.RestStopServiceAreaCodeBackfillService;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admindashboard.AdminDashboardService;
import com.restroute.service.admindashboard.AdminDashboardSummary;
import com.restroute.service.salesranking.SalesRankingUploadService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

class AdminDashboardControllerTest {

    private final Authentication authentication = mock(Authentication.class);

    @Test
    @DisplayName("관리자 대시보드 API는 조회 요약을 반환한다")
    void dashboard_returnsSummary() {
        AdminDashboardSummary summary = new AdminDashboardSummary(203, "2026-06", "준비중", List.of());
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(dashboardService.getSummary()).thenReturn(summary);
        AdminDashboardController controller = new AdminDashboardController(
                mock(SalesRankingUploadService.class),
                mock(RestStopServiceAreaCodeBackfillService.class),
                dashboardService,
                mock(AdminActivityLogService.class));

        assertThat(controller.dashboard().getBody().getData()).isEqualTo(summary);
    }

    @Test
    @DisplayName("상품 판매순위 업로드는 처리 건수를 JSON으로 반환하고 활동 로그를 남긴다")
    void uploadProductSalesRankings_returnsUploadedCount() {
        SalesRankingUploadService service = mock(SalesRankingUploadService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminDashboardController controller = new AdminDashboardController(
                service,
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                activityLogService);
        MockMultipartFile product = new MockMultipartFile("productFile", "product.csv", "text/csv", new byte[] {1});
        when(service.uploadProducts(product)).thenReturn(128);

        assertThat(controller
                        .uploadProductSalesRankings(product, authentication)
                        .getBody()
                        .getData())
                .isEqualTo(128);
        verify(activityLogService).logProductSalesUpload(authentication, "product.csv");
    }

    @Test
    @DisplayName("매장 판매순위 업로드는 처리 건수를 JSON으로 반환하고 활동 로그를 남긴다")
    void uploadStoreSalesRankings_returnsUploadedCount() {
        SalesRankingUploadService service = mock(SalesRankingUploadService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminDashboardController controller = new AdminDashboardController(
                service,
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                activityLogService);
        MockMultipartFile store = new MockMultipartFile("storeFile", "store.csv", "text/csv", new byte[] {1});
        when(service.uploadStores(store)).thenReturn(84);

        assertThat(controller
                        .uploadStoreSalesRankings(store, authentication)
                        .getBody()
                        .getData())
                .isEqualTo(84);
        verify(activityLogService).logStoreSalesUpload(authentication, "store.csv");
    }

    @Test
    @DisplayName("판매순위 매핑 실행은 도메인별 매핑 건수를 JSON으로 반환하고 활동 로그를 남긴다")
    void backfillSalesRankings_returnsMappedCounts() {
        RestStopServiceAreaCodeBackfillService backfillService = mock(RestStopServiceAreaCodeBackfillService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminDashboardController controller = new AdminDashboardController(
                mock(SalesRankingUploadService.class),
                backfillService,
                mock(AdminDashboardService.class),
                activityLogService);
        Map<String, Integer> counts = Map.of(RestStopServiceAreaCodeBackfillService.REST_FOOD_MAPPED_COUNT, 3);
        when(backfillService.backfill()).thenReturn(counts);

        assertThat(controller.backfillSalesRankings(authentication).getBody().getData())
                .isEqualTo(counts);
        verify(activityLogService).logBackfill(authentication);
    }

    @Test
    @DisplayName("상품 판매순위 업로드가 실패하면 예외를 그대로 던지고 활동 로그는 남기지 않는다(GlobalExceptionHandler가 JSON 에러로 응답)")
    void uploadProductSalesRankings_propagatesExceptionWhenUploadFails() {
        SalesRankingUploadService service = mock(SalesRankingUploadService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminDashboardController controller = new AdminDashboardController(
                service,
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                activityLogService);
        MockMultipartFile product = new MockMultipartFile("productFile", "broken.csv", "text/csv", new byte[] {1});
        when(service.uploadProducts(product)).thenThrow(new RuntimeException("upload failed"));

        assertThatThrownBy(() -> controller.uploadProductSalesRankings(product, authentication))
                .isInstanceOf(RuntimeException.class);
        verify(activityLogService, never()).logProductSalesUpload(any(), any());
    }

    @Test
    @DisplayName("매장 판매순위 업로드가 실패하면 예외를 그대로 던지고 활동 로그는 남기지 않는다")
    void uploadStoreSalesRankings_propagatesExceptionWhenUploadFails() {
        SalesRankingUploadService service = mock(SalesRankingUploadService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminDashboardController controller = new AdminDashboardController(
                service,
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                activityLogService);
        MockMultipartFile store = new MockMultipartFile("storeFile", "broken.csv", "text/csv", new byte[] {1});
        when(service.uploadStores(store)).thenThrow(new RuntimeException("upload failed"));

        assertThatThrownBy(() -> controller.uploadStoreSalesRankings(store, authentication))
                .isInstanceOf(RuntimeException.class);
        verify(activityLogService, never()).logStoreSalesUpload(any(), any());
    }

    @Test
    @DisplayName("판매순위 매핑 실행이 실패하면 예외를 그대로 던지고 활동 로그는 남기지 않는다")
    void backfillSalesRankings_propagatesExceptionWhenBackfillFails() {
        RestStopServiceAreaCodeBackfillService backfillService = mock(RestStopServiceAreaCodeBackfillService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminDashboardController controller = new AdminDashboardController(
                mock(SalesRankingUploadService.class),
                backfillService,
                mock(AdminDashboardService.class),
                activityLogService);
        when(backfillService.backfill()).thenThrow(new RuntimeException("backfill failed"));

        assertThatThrownBy(() -> controller.backfillSalesRankings(authentication))
                .isInstanceOf(RuntimeException.class);
        verify(activityLogService, never()).logBackfill(any());
    }
}

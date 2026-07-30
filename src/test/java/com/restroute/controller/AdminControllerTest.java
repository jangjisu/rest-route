package com.restroute.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

class AdminControllerTest {

    private final Authentication authentication = mock(Authentication.class);

    @Test
    @DisplayName("GET /admin은 관리자 대시보드 템플릿을 반환한다")
    void admin_returnsAdminDashboardView() {
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(dashboardService.getSummary()).thenReturn(new AdminDashboardSummary(203, "2026-06", "준비중", List.of()));

        assertThat(new AdminController(
                                mock(SalesRankingUploadService.class),
                                mock(RestStopServiceAreaCodeBackfillService.class),
                                dashboardService,
                                mock(AdminActivityLogService.class))
                        .admin())
                .isEqualTo("admin-dashboard");
    }

    @Test
    @DisplayName("GET /admin/rest-stops/images는 휴게소 이미지 관리 템플릿을 반환한다")
    void restStopImages_returnsAdminRestStopImagesView() {
        AdminController controller = new AdminController(
                mock(SalesRankingUploadService.class),
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                mock(AdminActivityLogService.class));

        assertThat(controller.restStopImages()).isEqualTo("admin-rest-stop-images");
    }

    @Test
    @DisplayName("GET /admin/rest-stops/edit는 휴게소 정보 관리 템플릿을 반환한다")
    void restStopEdit_returnsAdminRestStopEditView() {
        AdminController controller = new AdminController(
                mock(SalesRankingUploadService.class),
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                mock(AdminActivityLogService.class));

        assertThat(controller.restStopEdit()).isEqualTo("admin-rest-stop-edit");
    }

    @Test
    @DisplayName("GET /admin/rest-stops/foods는 휴게소 음식 관리 템플릿을 반환한다")
    void restStopFoods_returnsAdminRestStopFoodsView() {
        AdminController controller = new AdminController(
                mock(SalesRankingUploadService.class),
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                mock(AdminActivityLogService.class));

        assertThat(controller.restStopFoods()).isEqualTo("admin-rest-stop-foods");
    }

    @Test
    @DisplayName("GET /admin/rest-stops/oil-links는 휴게소 주유소 연결 관리 템플릿을 반환한다")
    void restStopOilLinks_returnsAdminRestStopOilLinksView() {
        AdminController controller = new AdminController(
                mock(SalesRankingUploadService.class),
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                mock(AdminActivityLogService.class));

        assertThat(controller.restStopOilLinks()).isEqualTo("admin-rest-stop-oil-links");
    }

    @Test
    @DisplayName("관리자 대시보드 API는 조회 요약을 반환한다")
    void dashboard_returnsSummary() {
        AdminDashboardSummary summary = new AdminDashboardSummary(203, "2026-06", "준비중", List.of());
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(dashboardService.getSummary()).thenReturn(summary);
        AdminController controller = new AdminController(
                mock(SalesRankingUploadService.class),
                mock(RestStopServiceAreaCodeBackfillService.class),
                dashboardService,
                mock(AdminActivityLogService.class));

        assertThat(controller.dashboard().getData()).isEqualTo(summary);
    }

    @Test
    @DisplayName("판매순위 업로드 후 관리자 화면으로 돌아가고 활동 로그를 남긴다")
    void uploadProductSalesRankings_redirectsToAdmin() {
        SalesRankingUploadService service = mock(SalesRankingUploadService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminController controller = new AdminController(
                service,
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                activityLogService);
        MockMultipartFile product = new MockMultipartFile("productFile", "product.csv", "text/csv", new byte[] {1});

        assertThat(controller.uploadProductSalesRankings(product, authentication))
                .isEqualTo("redirect:/admin?upload=success&type=product");
        verify(service).uploadProducts(product);
        verify(activityLogService).logProductSalesUpload(authentication, "product.csv");
    }

    @Test
    @DisplayName("매장 판매순위 업로드 후 관리자 화면으로 돌아가고 활동 로그를 남긴다")
    void uploadStoreSalesRankings_redirectsToAdmin() {
        SalesRankingUploadService service = mock(SalesRankingUploadService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminController controller = new AdminController(
                service,
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                activityLogService);
        MockMultipartFile store = new MockMultipartFile("storeFile", "store.csv", "text/csv", new byte[] {1});

        assertThat(controller.uploadStoreSalesRankings(store, authentication))
                .isEqualTo("redirect:/admin?upload=success&type=store");
        verify(service).uploadStores(store);
        verify(activityLogService).logStoreSalesUpload(authentication, "store.csv");
    }

    @Test
    @DisplayName("판매순위 매핑 실행 후 관리자 화면으로 돌아가고 활동 로그를 남긴다")
    void backfillSalesRankings_redirectsToAdmin() {
        RestStopServiceAreaCodeBackfillService backfillService = mock(RestStopServiceAreaCodeBackfillService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminController controller = new AdminController(
                mock(SalesRankingUploadService.class),
                backfillService,
                mock(AdminDashboardService.class),
                activityLogService);

        assertThat(controller.backfillSalesRankings(authentication)).isEqualTo("redirect:/admin?backfill=success");
        verify(backfillService).backfill();
        verify(activityLogService).logBackfill(authentication);
    }

    @Test
    @DisplayName("상품 판매순위 업로드가 실패하면 JSON이 아니라 에러 파라미터로 리다이렉트하고 활동 로그는 남기지 않는다")
    void uploadProductSalesRankings_redirectsWithErrorWhenUploadFails() {
        SalesRankingUploadService service = mock(SalesRankingUploadService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminController controller = new AdminController(
                service,
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                activityLogService);
        MockMultipartFile product = new MockMultipartFile("productFile", "broken.csv", "text/csv", new byte[] {1});
        when(service.uploadProducts(product)).thenThrow(new RuntimeException("upload failed"));

        assertThat(controller.uploadProductSalesRankings(product, authentication))
                .isEqualTo("redirect:/admin?upload=error&type=product");
        verify(activityLogService, never()).logProductSalesUpload(any(), any());
    }

    @Test
    @DisplayName("매장 판매순위 업로드가 실패하면 JSON이 아니라 에러 파라미터로 리다이렉트하고 활동 로그는 남기지 않는다")
    void uploadStoreSalesRankings_redirectsWithErrorWhenUploadFails() {
        SalesRankingUploadService service = mock(SalesRankingUploadService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminController controller = new AdminController(
                service,
                mock(RestStopServiceAreaCodeBackfillService.class),
                mock(AdminDashboardService.class),
                activityLogService);
        MockMultipartFile store = new MockMultipartFile("storeFile", "broken.csv", "text/csv", new byte[] {1});
        when(service.uploadStores(store)).thenThrow(new RuntimeException("upload failed"));

        assertThat(controller.uploadStoreSalesRankings(store, authentication))
                .isEqualTo("redirect:/admin?upload=error&type=store");
        verify(activityLogService, never()).logStoreSalesUpload(any(), any());
    }

    @Test
    @DisplayName("판매순위 매핑 실행이 실패하면 JSON이 아니라 에러 파라미터로 리다이렉트하고 활동 로그는 남기지 않는다")
    void backfillSalesRankings_redirectsWithErrorWhenBackfillFails() {
        RestStopServiceAreaCodeBackfillService backfillService = mock(RestStopServiceAreaCodeBackfillService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminController controller = new AdminController(
                mock(SalesRankingUploadService.class),
                backfillService,
                mock(AdminDashboardService.class),
                activityLogService);
        when(backfillService.backfill()).thenThrow(new RuntimeException("backfill failed"));

        assertThat(controller.backfillSalesRankings(authentication)).isEqualTo("redirect:/admin?backfill=error");
        verify(activityLogService, never()).logBackfill(any());
    }
}

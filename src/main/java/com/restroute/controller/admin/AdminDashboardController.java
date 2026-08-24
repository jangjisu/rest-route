package com.restroute.controller.admin;

import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.common.ApiResponse;
import com.restroute.service.RestStopServiceAreaCodeBackfillService;
import com.restroute.service.admindashboard.AdminDashboardService;
import com.restroute.service.admindashboard.dto.AdminDashboardSummary;
import com.restroute.service.salesranking.SalesRankingUploadService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class AdminDashboardController {

    private static final String PRODUCT_SALES_UPLOAD_MESSAGE = "상품 판매순위 CSV(%s)를 업로드했습니다.";
    private static final String STORE_SALES_UPLOAD_MESSAGE = "매장 판매순위 CSV(%s)를 업로드했습니다.";
    private static final String BACKFILL_MESSAGE = "전체 휴게소명 매핑을 실행했습니다.";

    private final SalesRankingUploadService salesRankingUploadService;
    private final RestStopServiceAreaCodeBackfillService backfillService;
    private final AdminDashboardService dashboardService;
    private final AdminActivityLogService adminActivityLogService;

    @GetMapping("/api/admin/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardSummary>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSummary()));
    }

    /**
     * @param productFile 상품 판매순위 CSV 파일(EUC-KR/MS949, 헤더 포함). 예: "product_sales_2026-07.csv"
     */
    @PostMapping("/api/admin/sales-rankings/products")
    public ResponseEntity<ApiResponse<Integer>> uploadProductSalesRankings(
            @RequestParam("productFile") MultipartFile productFile, Authentication authentication) {
        int uploadedCount = salesRankingUploadService.uploadProducts(productFile);
        adminActivityLogService.log(
                authentication, String.format(PRODUCT_SALES_UPLOAD_MESSAGE, productFile.getOriginalFilename()));
        return ResponseEntity.ok(ApiResponse.success(uploadedCount));
    }

    /**
     * @param storeFile 매장 판매순위 CSV 파일(EUC-KR/MS949, 헤더 포함). 예: "store_sales_2026-07.csv"
     */
    @PostMapping("/api/admin/sales-rankings/stores")
    public ResponseEntity<ApiResponse<Integer>> uploadStoreSalesRankings(
            @RequestParam("storeFile") MultipartFile storeFile, Authentication authentication) {
        int uploadedCount = salesRankingUploadService.uploadStores(storeFile);
        adminActivityLogService.log(
                authentication, String.format(STORE_SALES_UPLOAD_MESSAGE, storeFile.getOriginalFilename()));
        return ResponseEntity.ok(ApiResponse.success(uploadedCount));
    }

    @PostMapping("/api/admin/sales-rankings/backfill")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> backfillSalesRankings(Authentication authentication) {
        Map<String, Integer> mappedCounts = backfillService.backfill();
        adminActivityLogService.log(authentication, BACKFILL_MESSAGE);
        return ResponseEntity.ok(ApiResponse.success(mappedCounts));
    }
}

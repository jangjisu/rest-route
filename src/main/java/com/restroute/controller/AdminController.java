package com.restroute.controller;

import com.restroute.common.ApiResponse;
import com.restroute.service.RestStopServiceAreaCodeBackfillService;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admindashboard.AdminDashboardService;
import com.restroute.service.admindashboard.AdminDashboardSummary;
import com.restroute.service.salesranking.SalesRankingUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final SalesRankingUploadService salesRankingUploadService;
    private final RestStopServiceAreaCodeBackfillService backfillService;
    private final AdminDashboardService dashboardService;
    private final AdminActivityLogService adminActivityLogService;

    @GetMapping("/admin")
    public String admin() {
        return "admin-dashboard";
    }

    @GetMapping("/admin/rest-stops/images")
    public String restStopImages() {
        return "admin-rest-stop-images";
    }

    @GetMapping("/admin/rest-stops/edit")
    public String restStopEdit() {
        return "admin-rest-stop-edit";
    }

    @GetMapping("/admin/rest-stops/foods")
    public String restStopFoods() {
        return "admin-rest-stop-foods";
    }

    @GetMapping("/admin/rest-stops/oil-links")
    public String restStopOilLinks() {
        return "admin-rest-stop-oil-links";
    }

    @GetMapping("/api/admin/dashboard")
    @ResponseBody
    public ApiResponse<AdminDashboardSummary> dashboard() {
        return ApiResponse.success(dashboardService.getSummary());
    }

    @PostMapping("/admin/sales-rankings/products")
    public String uploadProductSalesRankings(
            @RequestParam("productFile") MultipartFile productFile, Authentication authentication) {
        salesRankingUploadService.uploadProducts(productFile);
        adminActivityLogService.logProductSalesUpload(authentication, productFile.getOriginalFilename());
        return "redirect:/admin?upload=success&type=product";
    }

    @PostMapping("/admin/sales-rankings/stores")
    public String uploadStoreSalesRankings(
            @RequestParam("storeFile") MultipartFile storeFile, Authentication authentication) {
        salesRankingUploadService.uploadStores(storeFile);
        adminActivityLogService.logStoreSalesUpload(authentication, storeFile.getOriginalFilename());
        return "redirect:/admin?upload=success&type=store";
    }

    @PostMapping("/admin/sales-rankings/backfill")
    public String backfillSalesRankings(Authentication authentication) {
        backfillService.backfill();
        adminActivityLogService.logBackfill(authentication);
        return "redirect:/admin?backfill=success";
    }
}

package com.restroute.reststop.controller.admin;

import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.common.ApiResponse;
import com.restroute.reststop.service.usage.RestStopUsageSnapshotUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/rest-stops")
public class AdminRestStopUsageSnapshotController {

    private static final String USAGE_SNAPSHOT_UPLOAD_MESSAGE = "이용객 및 교통량 현황 CSV(%s)를 업로드했습니다.";

    private final RestStopUsageSnapshotUploadService usageSnapshotUploadService;
    private final AdminActivityLogService adminActivityLogService;

    @PostMapping("/usage-snapshots")
    public ResponseEntity<ApiResponse<Integer>> uploadUsageSnapshots(
            @RequestParam("usageSnapshotFile") MultipartFile usageSnapshotFile, Authentication authentication) {
        int uploadedCount = usageSnapshotUploadService.upload(usageSnapshotFile);
        adminActivityLogService.log(
                authentication, String.format(USAGE_SNAPSHOT_UPLOAD_MESSAGE, usageSnapshotFile.getOriginalFilename()));
        return ResponseEntity.ok(ApiResponse.success(uploadedCount));
    }
}

package com.restroute.reststop.controller.admin;

import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.common.ApiResponse;
import com.restroute.reststop.service.restroom.RestStopRestroomUploadService;
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
public class AdminRestStopRestroomController {

    private static final String RESTROOM_UPLOAD_MESSAGE = "화장실 현황 CSV(%s)를 업로드했습니다.";

    private final RestStopRestroomUploadService restroomUploadService;
    private final AdminActivityLogService adminActivityLogService;

    @PostMapping("/restrooms")
    public ResponseEntity<ApiResponse<Integer>> uploadRestrooms(
            @RequestParam("restroomFile") MultipartFile restroomFile, Authentication authentication) {
        int uploadedCount = restroomUploadService.upload(restroomFile);
        adminActivityLogService.log(
                authentication, String.format(RESTROOM_UPLOAD_MESSAGE, restroomFile.getOriginalFilename()));
        return ResponseEntity.ok(ApiResponse.success(uploadedCount));
    }
}

package com.restroute.controller.admin;

import com.restroute.common.ApiResponse;
import com.restroute.controller.request.AdminFlightHolidayRequest;
import com.restroute.controller.response.AdminFlightHolidayResponse;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admin.AdminFlightHolidayService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/flights/holidays")
public class AdminFlightHolidayController {

    private final AdminFlightHolidayService adminFlightHolidayService;
    private final AdminActivityLogService adminActivityLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminFlightHolidayResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(adminFlightHolidayService.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminFlightHolidayResponse>> create(
            @RequestBody AdminFlightHolidayRequest request, Authentication authentication) {
        AdminFlightHolidayResponse response = adminFlightHolidayService.create(request);
        adminActivityLogService.logFlightHolidayAdded(authentication, response.date(), response.name());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{holidayId}")
    public ResponseEntity<Void> delete(@PathVariable Long holidayId, Authentication authentication) {
        AdminFlightHolidayResponse deleted = adminFlightHolidayService.delete(holidayId);
        adminActivityLogService.logFlightHolidayDeleted(authentication, deleted.date());
        return ResponseEntity.noContent().build();
    }
}

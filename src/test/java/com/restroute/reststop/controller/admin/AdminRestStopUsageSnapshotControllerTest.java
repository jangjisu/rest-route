package com.restroute.reststop.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.reststop.service.usage.RestStopUsageSnapshotUploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

class AdminRestStopUsageSnapshotControllerTest {

    private final Authentication authentication = mock(Authentication.class);

    @Test
    @DisplayName("이용객 및 교통량 현황 업로드는 처리 건수를 JSON으로 반환하고 활동 로그를 남긴다")
    void uploadUsageSnapshots_returnsUploadedCount() {
        RestStopUsageSnapshotUploadService service = mock(RestStopUsageSnapshotUploadService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminRestStopUsageSnapshotController controller =
                new AdminRestStopUsageSnapshotController(service, activityLogService);
        MockMultipartFile usageFile =
                new MockMultipartFile("usageSnapshotFile", "usage.csv", "text/csv", new byte[] {1});
        when(service.upload(usageFile)).thenReturn(206);

        assertThat(controller
                        .uploadUsageSnapshots(usageFile, authentication)
                        .getBody()
                        .getData())
                .isEqualTo(206);
        verify(activityLogService).log(authentication, "이용객 및 교통량 현황 CSV(usage.csv)를 업로드했습니다.");
    }
}

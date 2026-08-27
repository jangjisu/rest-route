package com.restroute.reststop.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.reststop.service.restroom.RestStopRestroomUploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

class AdminRestStopRestroomControllerTest {

    private final Authentication authentication = mock(Authentication.class);

    @Test
    @DisplayName("화장실 현황 업로드는 처리 건수를 JSON으로 반환하고 활동 로그를 남긴다")
    void uploadRestrooms_returnsUploadedCount() {
        RestStopRestroomUploadService service = mock(RestStopRestroomUploadService.class);
        AdminActivityLogService activityLogService = mock(AdminActivityLogService.class);
        AdminRestStopRestroomController controller = new AdminRestStopRestroomController(service, activityLogService);
        MockMultipartFile restroom = new MockMultipartFile("restroomFile", "restroom.csv", "text/csv", new byte[] {1});
        when(service.upload(restroom)).thenReturn(211);

        assertThat(controller
                        .uploadRestrooms(restroom, authentication)
                        .getBody()
                        .getData())
                .isEqualTo(211);
        verify(activityLogService).log(authentication, "화장실 현황 CSV(restroom.csv)를 업로드했습니다.");
    }
}

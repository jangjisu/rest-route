package com.restroute.controller.admin;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.common.GlobalExceptionHandler;
import com.restroute.service.image.RestStopImageCommandService;
import com.restroute.service.image.exception.InvalidRestStopImageException;
import com.restroute.service.image.exception.RestStopNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminRestStopImageControllerTest {

    @Mock
    private RestStopImageCommandService commandService;

    @Mock
    private AdminActivityLogService adminActivityLogService;

    private MockMvc mockMvc;
    private final Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminRestStopImageController(commandService, adminActivityLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("PUT /api/admin/rest-stops/{serviceAreaCode}/image는 이미지를 저장하고 204를 반환하며 활동 로그를 남긴다")
    void save_returnsNoContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {1});

        mockMvc.perform(multipart("/api/admin/rest-stops/A00001/image")
                        .file(file)
                        .principal(authentication)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isNoContent());

        verify(commandService).save("A00001", file);
        verify(adminActivityLogService).log(authentication, "휴게소(A00001) 이미지를 등록했습니다.");
    }

    @Test
    @DisplayName("DELETE /api/admin/rest-stops/{serviceAreaCode}/image는 이미지를 삭제하고 204를 반환하며 활동 로그를 남긴다")
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/rest-stops/A00001/image").principal(authentication))
                .andExpect(status().isNoContent());

        verify(commandService).delete("A00001");
        verify(adminActivityLogService).log(authentication, "휴게소(A00001) 이미지를 삭제했습니다.");
    }

    @Test
    @DisplayName("없는 휴게소의 관리자 이미지 변경은 404를 반환한다")
    void save_returnsNotFoundWhenRestStopIsMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {1});
        doThrow(new RestStopNotFoundException("UNKNOWN")).when(commandService).save("UNKNOWN", file);

        mockMvc.perform(multipart("/api/admin/rest-stops/UNKNOWN/image")
                        .file(file)
                        .principal(authentication)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("잘못된 이미지는 관리자 변경 API에서 400을 반환한다")
    void save_returnsBadRequestWhenImageIsInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.gif", "image/gif", new byte[] {1});
        doThrow(new InvalidRestStopImageException("invalid"))
                .when(commandService)
                .save("A00001", file);

        mockMvc.perform(multipart("/api/admin/rest-stops/A00001/image")
                        .file(file)
                        .principal(authentication)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}

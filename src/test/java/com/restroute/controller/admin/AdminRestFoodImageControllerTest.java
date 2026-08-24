package com.restroute.controller.admin;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.common.GlobalExceptionHandler;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admin.AdminRestFoodImageCommandService;
import com.restroute.service.admin.AdminRestFoodImageQueryService;
import com.restroute.service.admin.exception.RestFoodNotFoundException;
import java.util.Optional;
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
class AdminRestFoodImageControllerTest {

    @Mock
    private AdminRestFoodImageCommandService commandService;

    @Mock
    private AdminRestFoodImageQueryService queryService;

    @Mock
    private AdminActivityLogService adminActivityLogService;

    private MockMvc mockMvc;
    private final Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminRestFoodImageController(commandService, queryService, adminActivityLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET .../foods/{foodId}/image는 저장된 이미지를 반환한다")
    void getImage_returnsStoredImage() throws Exception {
        when(queryService.findListImage("A00001", 1L)).thenReturn(Optional.of(new byte[] {1, 2}));

        mockMvc.perform(get("/api/admin/rest-stops/A00001/foods/1/image"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1, 2}));
    }

    @Test
    @DisplayName("GET .../foods/{foodId}/image는 이미지가 없으면 204를 반환한다")
    void getImage_returnsNoContentWhenImageMissing() throws Exception {
        when(queryService.findListImage("A00001", 1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/rest-stops/A00001/foods/1/image")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET .../foods/{foodId}/image는 메뉴가 없으면 404를 반환한다")
    void getImage_returnsNotFoundWhenFoodMissing() throws Exception {
        when(queryService.findListImage("A00001", 99L)).thenThrow(RestFoodNotFoundException.forId(99L));

        mockMvc.perform(get("/api/admin/rest-stops/A00001/foods/99/image"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT .../foods/{foodId}/image는 이미지를 저장하고 204와 활동 로그를 남긴다")
    void save_returnsNoContentAndLogs() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {1});

        mockMvc.perform(multipart("/api/admin/rest-stops/A00001/foods/1/image")
                        .file(file)
                        .principal(authentication)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isNoContent());

        verify(commandService).save("A00001", 1L, file);
        verify(adminActivityLogService).log(authentication, "메뉴(1) 이미지를 등록했습니다.");
    }

    @Test
    @DisplayName("DELETE .../foods/{foodId}/image는 이미지를 삭제하고 204와 활동 로그를 남긴다")
    void delete_returnsNoContentAndLogs() throws Exception {
        mockMvc.perform(delete("/api/admin/rest-stops/A00001/foods/1/image").principal(authentication))
                .andExpect(status().isNoContent());

        verify(commandService).delete("A00001", 1L);
        verify(adminActivityLogService).log(authentication, "메뉴(1) 이미지를 삭제했습니다.");
    }
}

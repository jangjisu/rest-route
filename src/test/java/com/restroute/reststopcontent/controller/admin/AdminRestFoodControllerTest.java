package com.restroute.reststopcontent.controller.admin;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.common.GlobalExceptionHandler;
import com.restroute.controller.request.AdminRestFoodRequest;
import com.restroute.reststopcontent.controller.response.AdminRestFoodResponse;
import com.restroute.reststopcontent.service.admin.AdminRestFoodService;
import com.restroute.reststopcontent.service.admin.exception.InvalidRestFoodEditException;
import com.restroute.reststopcontent.service.admin.exception.RestFoodNotFoundException;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.image.exception.RestStopNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminRestFoodControllerTest {

    @Mock
    private AdminRestFoodService adminRestFoodService;

    @Mock
    private AdminActivityLogService adminActivityLogService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminRestFoodController(adminRestFoodService, adminActivityLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AdminRestFoodRequest request() {
        return new AdminRestFoodRequest("커스텀메뉴", "5000", "설명");
    }

    private AdminRestFoodResponse response(boolean overridden, boolean created) {
        return response(overridden, created, false);
    }

    private AdminRestFoodResponse response(boolean overridden, boolean created, boolean hasImage) {
        return new AdminRestFoodResponse(1L, "커스텀메뉴", "5000", "설명", overridden, created, hasImage);
    }

    @Test
    @DisplayName("GET .../foods는 휴게소의 전체 메뉴 목록을 반환한다")
    void findByServiceAreaCode_returnsOk() throws Exception {
        when(adminRestFoodService.findByServiceAreaCode("A00001")).thenReturn(List.of(response(false, false, true)));

        mockMvc.perform(get("/api/admin/rest-stops/A00001/foods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].foodName").value("커스텀메뉴"))
                .andExpect(jsonPath("$.data[0].hasImage").value(true));
    }

    @Test
    @DisplayName("POST .../foods는 메뉴를 생성하고 활동 로그를 남긴다")
    void create_returnsCreatedAndLogs() throws Exception {
        when(adminRestFoodService.create("A00001", request())).thenReturn(response(true, true));

        mockMvc.perform(post("/api/admin/rest-stops/A00001/foods")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.foodName").value("커스텀메뉴"));

        verify(adminActivityLogService).log(authentication, "커스텀메뉴 메뉴를 추가했습니다.");
    }

    @Test
    @DisplayName("POST .../foods는 휴게소가 없으면 404를 반환한다")
    void create_returnsNotFoundWhenRestStopMissing() throws Exception {
        doThrow(RestStopNotFoundException.forServiceAreaCode("UNKNOWN"))
                .when(adminRestFoodService)
                .create("UNKNOWN", request());

        mockMvc.perform(post("/api/admin/rest-stops/UNKNOWN/foods")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT .../foods/{foodId}는 메뉴를 수정하고 활동 로그를 남긴다")
    void update_returnsOkAndLogs() throws Exception {
        when(adminRestFoodService.update("A00001", 1L, request())).thenReturn(response(true, false));

        mockMvc.perform(put("/api/admin/rest-stops/A00001/foods/1")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminOverridden").value(true));

        verify(adminActivityLogService).log(authentication, "커스텀메뉴 메뉴를 수정했습니다.");
    }

    @Test
    @DisplayName("PUT .../foods/{foodId}는 메뉴가 없으면 404를 반환한다")
    void update_returnsNotFoundWhenFoodMissing() throws Exception {
        doThrow(RestFoodNotFoundException.forId(99L)).when(adminRestFoodService).update("A00001", 99L, request());

        mockMvc.perform(put("/api/admin/rest-stops/A00001/foods/99")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE .../foods/{foodId}/override는 잠금을 해제하고 활동 로그를 남긴다")
    void clearOverride_returnsOkAndLogs() throws Exception {
        when(adminRestFoodService.clearOverride("A00001", 1L)).thenReturn(response(false, false));

        mockMvc.perform(delete("/api/admin/rest-stops/A00001/foods/1/override").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminOverridden").value(false));

        verify(adminActivityLogService).log(authentication, "커스텀메뉴 메뉴의 동기화 잠금을 해제했습니다.");
    }

    @Test
    @DisplayName("DELETE .../foods/{foodId}는 관리자 추가 메뉴를 삭제하고 활동 로그를 남긴다")
    void delete_returnsNoContentAndLogs() throws Exception {
        mockMvc.perform(delete("/api/admin/rest-stops/A00001/foods/1").principal(authentication))
                .andExpect(status().isNoContent());

        verify(adminRestFoodService).delete("A00001", 1L);
        verify(adminActivityLogService).log(authentication, "메뉴(1)를 삭제했습니다.");
    }

    @Test
    @DisplayName("DELETE .../foods/{foodId}는 동기화 메뉴를 삭제하려 하면 400을 반환한다")
    void delete_returnsBadRequestWhenFoodIsSynced() throws Exception {
        doThrow(InvalidRestFoodEditException.forSyncedFoodDeletion(1L))
                .when(adminRestFoodService)
                .delete("A00001", 1L);

        mockMvc.perform(delete("/api/admin/rest-stops/A00001/foods/1").principal(authentication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}

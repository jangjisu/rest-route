package com.restroute.controller.admin;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.common.GlobalExceptionHandler;
import com.restroute.controller.request.AdminRestStopUpdateRequest;
import com.restroute.controller.response.AdminRestStopEditableResponse;
import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admin.AdminRestStopEditService;
import com.restroute.service.admin.InvalidRestStopEditException;
import com.restroute.service.image.RestStopNotFoundException;
import java.util.Optional;
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
class AdminRestStopEditControllerTest {

    @Mock
    private AdminRestStopEditService editService;

    @Mock
    private AdminActivityLogService adminActivityLogService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminRestStopEditController(editService, adminActivityLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AdminRestStopEditableResponse sampleResponse(boolean overridden) {
        return new AdminRestStopEditableResponse(
                "A00001",
                "001",
                "서울만남(부산)휴게소",
                "0010",
                "경부선",
                "127.0",
                "37.0",
                "054-751-6890",
                "투썸플레이스",
                "0010",
                "주소",
                "수유실",
                "X",
                "X",
                overridden);
    }

    @Test
    @DisplayName("GET .../editable은 편집 가능한 정보를 200으로 반환한다")
    void find_returnsOk() throws Exception {
        when(editService.findEditable("A00001")).thenReturn(Optional.of(sampleResponse(false)));

        mockMvc.perform(get("/api/admin/rest-stops/A00001/editable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unitName").value("서울만남(부산)휴게소"))
                .andExpect(jsonPath("$.data.adminOverridden").value(false));
    }

    @Test
    @DisplayName("GET .../editable은 없는 휴게소면 404를 반환한다")
    void find_returnsNotFoundWhenMissing() throws Exception {
        when(editService.findEditable("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/rest-stops/UNKNOWN/editable"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT .../editable은 저장 후 200과 갱신된 정보를 반환하며 활동 로그를 남긴다")
    void update_returnsOk() throws Exception {
        AdminRestStopUpdateRequest request = new AdminRestStopUpdateRequest(
                "수정된이름", "9999", "수정된노선", "128.0", "38.0", "031-000-0000", "수정브랜드", "9998", "수정주소", "샤워실", "O", "O");
        when(editService.update("A00001", request)).thenReturn(sampleResponse(true));

        mockMvc.perform(put("/api/admin/rest-stops/A00001/editable")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminOverridden").value(true));

        verify(editService).update("A00001", request);
        verify(adminActivityLogService).logRestStopEdited(authentication, "서울만남(부산)휴게소");
    }

    @Test
    @DisplayName("PUT .../editable은 없는 휴게소면 404를 반환한다")
    void update_returnsNotFoundWhenMissing() throws Exception {
        AdminRestStopUpdateRequest request = new AdminRestStopUpdateRequest(
                "수정된이름", "9999", "수정된노선", "128.0", "38.0", "031-000-0000", "수정브랜드", "9998", "수정주소", "샤워실", "O", "O");
        doThrow(RestStopNotFoundException.forServiceAreaCode("UNKNOWN"))
                .when(editService)
                .update("UNKNOWN", request);

        mockMvc.perform(put("/api/admin/rest-stops/UNKNOWN/editable")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT .../editable은 좌표가 잘못되면 400을 반환한다")
    void update_returnsBadRequestWhenCoordinateInvalid() throws Exception {
        AdminRestStopUpdateRequest request = new AdminRestStopUpdateRequest(
                "수정된이름", "9999", "수정된노선", "숫자아님", "38.0", "031-000-0000", "수정브랜드", "9998", "수정주소", "샤워실", "O", "O");
        doThrow(new InvalidRestStopEditException("Invalid coordinate value: 숫자아님"))
                .when(editService)
                .update("A00001", request);

        mockMvc.perform(put("/api/admin/rest-stops/A00001/editable")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("DELETE .../editable/override는 잠금을 해제하고 200을 반환하며 활동 로그를 남긴다")
    void clearOverride_returnsOk() throws Exception {
        when(editService.clearOverride("A00001")).thenReturn(sampleResponse(false));

        mockMvc.perform(delete("/api/admin/rest-stops/A00001/editable/override").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminOverridden").value(false));

        verify(adminActivityLogService).logRestStopOverrideCleared(authentication, "서울만남(부산)휴게소");
    }

    @Test
    @DisplayName("DELETE .../editable/override는 없는 휴게소면 404를 반환한다")
    void clearOverride_returnsNotFoundWhenMissing() throws Exception {
        doThrow(RestStopNotFoundException.forServiceAreaCode("UNKNOWN"))
                .when(editService)
                .clearOverride("UNKNOWN");

        mockMvc.perform(delete("/api/admin/rest-stops/UNKNOWN/editable/override")
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}

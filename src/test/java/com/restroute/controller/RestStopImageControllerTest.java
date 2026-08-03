package com.restroute.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.common.GlobalExceptionHandler;
import com.restroute.service.image.RestStopImageQueryService;
import com.restroute.service.image.exception.RestStopNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.DigestUtils;

@ExtendWith(MockitoExtension.class)
class RestStopImageControllerTest {

    @Mock
    private RestStopImageQueryService queryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RestStopImageController(queryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET detail 이미지는 WebP 본문과 검증용 캐시 헤더를 반환한다")
    void getDetailImage_returnsWebpWithCacheHeaders() throws Exception {
        byte[] image = new byte[] {1, 2, 3};
        String eTag = "\"" + DigestUtils.md5DigestAsHex(image) + "\"";
        when(queryService.findDetailImage("A00001")).thenReturn(Optional.of(image));

        mockMvc.perform(get("/api/rest-stops/A00001/images/detail"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(image))
                .andExpect(header().string("ETag", eTag))
                .andExpect(header().string("Cache-Control", "public, no-cache"));
    }

    @Test
    @DisplayName("GET detail 이미지는 일치하는 If-None-Match에 304를 반환한다")
    void getDetailImage_returnsNotModifiedWhenETagMatches() throws Exception {
        byte[] image = new byte[] {1, 2, 3};
        String eTag = "\"" + DigestUtils.md5DigestAsHex(image) + "\"";
        when(queryService.findDetailImage("A00001")).thenReturn(Optional.of(image));

        mockMvc.perform(get("/api/rest-stops/A00001/images/detail").header("If-None-Match", eTag))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""))
                .andExpect(header().string("ETag", eTag))
                .andExpect(header().string("Cache-Control", "public, no-cache"));
    }

    @Test
    @DisplayName("GET detail 이미지는 If-None-Match 목록의 일치하는 ETag에 304를 반환한다")
    void getDetailImage_returnsNotModifiedWhenETagMatchesInAList() throws Exception {
        byte[] image = new byte[] {1, 2, 3};
        String eTag = "\"" + DigestUtils.md5DigestAsHex(image) + "\"";
        when(queryService.findDetailImage("A00001")).thenReturn(Optional.of(image));

        mockMvc.perform(get("/api/rest-stops/A00001/images/detail")
                        .header("If-None-Match", "\"another-version\", " + eTag))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("GET detail 이미지는 약한 If-None-Match ETag에 304를 반환한다")
    void getDetailImage_returnsNotModifiedWhenWeakETagMatches() throws Exception {
        byte[] image = new byte[] {1, 2, 3};
        String eTag = "\"" + DigestUtils.md5DigestAsHex(image) + "\"";
        when(queryService.findDetailImage("A00001")).thenReturn(Optional.of(image));

        mockMvc.perform(get("/api/rest-stops/A00001/images/detail").header("If-None-Match", "W/" + eTag))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("GET detail 이미지는 wildcard If-None-Match에 304를 반환한다")
    void getDetailImage_returnsNotModifiedWhenETagWildcardMatches() throws Exception {
        byte[] image = new byte[] {1, 2, 3};
        when(queryService.findDetailImage("A00001")).thenReturn(Optional.of(image));

        mockMvc.perform(get("/api/rest-stops/A00001/images/detail").header("If-None-Match", "*"))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("GET detail 이미지는 일치하지 않는 If-None-Match에 200과 본문을 반환한다")
    void getDetailImage_returnsBodyWhenETagDoesNotMatch() throws Exception {
        byte[] image = new byte[] {1, 2, 3};
        when(queryService.findDetailImage("A00001")).thenReturn(Optional.of(image));

        mockMvc.perform(get("/api/rest-stops/A00001/images/detail").header("If-None-Match", "\"other\""))
                .andExpect(status().isOk())
                .andExpect(content().bytes(image));
    }

    @Test
    @DisplayName("GET list 이미지는 목록 변형 WebP 본문을 반환한다")
    void getListImage_returnsListImage() throws Exception {
        byte[] image = new byte[] {4, 5, 6};
        when(queryService.findListImage("A00001")).thenReturn(Optional.of(image));

        mockMvc.perform(get("/api/rest-stops/A00001/images/list"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(image));
    }

    @Test
    @DisplayName("이미지가 없는 기존 휴게소의 GET은 204를 반환한다")
    void getDetailImage_returnsNoContentWhenImageIsMissing() throws Exception {
        when(queryService.findDetailImage("A00001")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rest-stops/A00001/images/detail")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("없는 휴게소의 공개 이미지 GET은 404를 반환한다")
    void getDetailImage_returnsNotFoundWhenRestStopIsMissing() throws Exception {
        when(queryService.findDetailImage("UNKNOWN")).thenThrow(new RestStopNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/api/rest-stops/UNKNOWN/images/detail"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}

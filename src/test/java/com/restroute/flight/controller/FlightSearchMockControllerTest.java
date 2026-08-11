package com.restroute.flight.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FlightSearchMockControllerTest {

    private static final String VALID_ORIGIN = "ICN";
    private static final String VALID_DATE_FROM = "2099-01-10";
    private static final String VALID_DATE_TO = "2099-02-10";

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlightSearchMockController())
                .setControllerAdvice(new FlightExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("유효한 요청이면 첫 항목이 계산된 값 그대로 채워진 첫 페이지를 반환한다")
    void searchMock_returnsFirstPageWithComputedFields() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.items.length()").value(20))
                .andExpect(jsonPath("$.data.meta.totalCount").value(342))
                .andExpect(jsonPath("$.data.meta.hasNext").value(true))
                .andExpect(jsonPath("$.data.items[0].destination.iata").value("FUK"))
                .andExpect(jsonPath("$.data.items[0].departureAt").value("2099-01-10T09:20:00+09:00"))
                .andExpect(jsonPath("$.data.items[0].returnAt").value("2099-01-13T11:20:00+09:00"))
                .andExpect(jsonPath("$.data.items[0].nights").value(3))
                .andExpect(jsonPath("$.data.items[0].airline").value("LJ"))
                .andExpect(jsonPath("$.data.items[0].flightNumber").value("LJ100"))
                .andExpect(jsonPath("$.data.items[0].price.amount").value(89000))
                .andExpect(jsonPath("$.data.items[0].price.currency").value("KRW"))
                .andExpect(jsonPath("$.data.items[0].gateName").value("Trip.com"));
    }

    @Test
    @DisplayName("destination을 지정하면 모든 항목이 그 목적지로 고정된다")
    void searchMock_fixesDestinationWhenGiven() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("destination", "OSA")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].destination.iata").value("OSA"))
                .andExpect(jsonPath("$.data.items[4].destination.iata").value("OSA"));
    }

    @Test
    @DisplayName("필수 파라미터가 여러 개 없으면 VALIDATION_FAILED와 details를 반환한다")
    void searchMock_returnsValidationFailedForMissingRequiredParams() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details.length()").value(4))
                .andExpect(jsonPath("$.error.details[0].field").value("origin"))
                .andExpect(jsonPath("$.error.details[0].code").value("REQUIRED"));
    }

    @Test
    @DisplayName("알 수 없는 커서는 DEAL_NOT_FOUND 에러를 반환한다")
    void searchMock_returnsDealNotFoundForUnknownCursor() throws Exception {
        mockMvc.perform(get("/api/flights/search/mock")
                        .param("origin", VALID_ORIGIN)
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3")
                        .param("cursor", "deal_notreal1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("DEAL_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Deal not found or already expired"));
    }

    @Test
    @DisplayName("실제 nextCursor를 계속 이어가면 중복/누락 없이 전체 342건을 정확히 순회한다")
    void searchMock_paginatesThroughFullDatasetWithoutDuplicates() throws Exception {
        Set<String> seenIds = new HashSet<>();
        String cursor = null;
        boolean hasNext = true;
        int pageCount = 0;
        int lastPageSize = 0;

        while (hasNext) {
            var requestBuilder = get("/api/flights/search/mock")
                    .param("origin", VALID_ORIGIN)
                    .param("dateFrom", VALID_DATE_FROM)
                    .param("dateTo", VALID_DATE_TO)
                    .param("nights", "3")
                    .param("size", "20");
            if (cursor != null) {
                requestBuilder = requestBuilder.param("cursor", cursor);
            }

            String body = mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode json = objectMapper.readTree(body);
            JsonNode items = json.at("/data/items");
            for (JsonNode item : items) {
                assertThat(seenIds.add(item.get("id").asText())).isTrue();
            }

            lastPageSize = items.size();
            hasNext = json.at("/data/meta/hasNext").asBoolean();
            cursor = hasNext ? json.at("/data/meta/nextCursor").asText() : null;
            pageCount++;
        }

        assertThat(seenIds).hasSize(342);
        assertThat(pageCount).isEqualTo(18);
        assertThat(lastPageSize).isEqualTo(2);
    }
}

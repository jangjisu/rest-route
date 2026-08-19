package com.restroute.flight.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchMeta;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import com.restroute.flight.service.FlightSearchService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FlightSearchControllerTest {

    private static final String VALID_ORIGIN = "ICN";
    private static final String VALID_DATE_FROM = LocalDate.now().plusDays(10).toString();
    private static final String VALID_DATE_TO = LocalDate.now().plusDays(13).toString();

    private final FlightSearchService flightSearchService = mock(FlightSearchService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlightSearchController(flightSearchService))
                .setControllerAdvice(new FlightExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("유효한 요청이면 flightSearchService.search 결과를 그대로 응답에 담는다")
    void search_returnsServiceResultAsIs() throws Exception {
        FlightDealResponse.Leg leg =
                new FlightDealResponse.Leg("2026-09-15T09:00:00+09:00", "2026-09-15T10:30:00+09:00", 90, 0);
        FlightDealResponse deal = new FlightDealResponse(
                "TOK1_0001",
                new FlightDealResponse.Destination("KIX", "오사카"),
                leg,
                leg,
                3,
                List.of(),
                new FlightDealResponse.Airline("LJ", "진에어", true),
                new FlightDealResponse.Price(89000, "KRW"),
                true,
                "Aviasales",
                "https://example.com/link",
                null);
        FlightDealSearchMeta meta = FlightDealSearchMeta.of(null, false, 1, "ko", "krw", "2026-09-01T00:00:00+09:00");
        when(flightSearchService.search(any())).thenReturn(FlightDealSearchResponse.of(List.of(deal), meta));

        mockMvc.perform(get("/api/flights/search")
                        .param("origin", VALID_ORIGIN)
                        .param("searchMode", "range")
                        .param("dateFrom", VALID_DATE_FROM)
                        .param("dateTo", VALID_DATE_TO)
                        .param("nights", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value("TOK1_0001"))
                .andExpect(jsonPath("$.data[0].destination.code").value("KIX"))
                .andExpect(jsonPath("$.meta.totalCount").value(1))
                .andExpect(jsonPath("$.meta.hasNext").value(false));
    }

    @Test
    @DisplayName("필수 파라미터가 없으면 validation_failed로 실패한다")
    void search_returnsValidationFailedForMissingRequiredParams() throws Exception {
        mockMvc.perform(get("/api/flights/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
    }
}

package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.flight.controller.response.FlightAirportResponse;
import com.restroute.flight.domain.FlightAirportEntity;
import com.restroute.flight.repository.FlightAirportRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightAirportQueryServiceTest {

    @Mock
    private FlightAirportRepository flightAirportRepository;

    private FlightAirportQueryService flightAirportQueryService;

    @BeforeEach
    void setUp() {
        flightAirportQueryService = new FlightAirportQueryService(flightAirportRepository);
    }

    @Test
    @DisplayName("keyword가 있으면 korName/engName 부분 일치로 검색한다")
    void search_byKeyword_searchesByKorOrEngName() {
        FlightAirportEntity icn =
                new FlightAirportEntity("ICN", "인천국제공항", "Incheon International Airport", "SEL", "KR");
        when(flightAirportRepository.findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                        "인천", "인천"))
                .thenReturn(List.of(icn));

        List<FlightAirportResponse> result = flightAirportQueryService.search("인천");

        assertThat(result).extracting(FlightAirportResponse::code).containsExactly("ICN");
    }

    @Test
    @DisplayName("keyword가 없으면 전체 목록을 조회한다")
    void search_returnsAll_whenKeywordMissing() {
        FlightAirportEntity icn =
                new FlightAirportEntity("ICN", "인천국제공항", "Incheon International Airport", "SEL", "KR");
        when(flightAirportRepository.findAll()).thenReturn(List.of(icn));

        List<FlightAirportResponse> result = flightAirportQueryService.search(null);

        assertThat(result).extracting(FlightAirportResponse::code).containsExactly("ICN");
    }
}

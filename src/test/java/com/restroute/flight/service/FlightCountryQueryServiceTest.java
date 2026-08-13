package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.flight.controller.response.FlightCountryResponse;
import com.restroute.flight.domain.FlightCountryEntity;
import com.restroute.flight.repository.FlightCountryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightCountryQueryServiceTest {

    @Mock
    private FlightCountryRepository flightCountryRepository;

    private FlightCountryQueryService flightCountryQueryService;

    @BeforeEach
    void setUp() {
        flightCountryQueryService = new FlightCountryQueryService(flightCountryRepository);
    }

    @Test
    @DisplayName("keyword가 있으면 korName/engName 부분 일치로 검색한다")
    void search_byKeyword_searchesByKorOrEngName() {
        FlightCountryEntity japan = new FlightCountryEntity("JP", "일본", "Japan");
        when(flightCountryRepository.findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                        "일본", "일본"))
                .thenReturn(List.of(japan));

        List<FlightCountryResponse> result = flightCountryQueryService.search("일본");

        assertThat(result).extracting(FlightCountryResponse::code).containsExactly("JP");
    }

    @Test
    @DisplayName("keyword가 없으면 전체 목록을 조회한다")
    void search_returnsAll_whenKeywordMissing() {
        FlightCountryEntity japan = new FlightCountryEntity("JP", "일본", "Japan");
        when(flightCountryRepository.findAll()).thenReturn(List.of(japan));

        List<FlightCountryResponse> result = flightCountryQueryService.search(null);

        assertThat(result).extracting(FlightCountryResponse::code).containsExactly("JP");
    }
}

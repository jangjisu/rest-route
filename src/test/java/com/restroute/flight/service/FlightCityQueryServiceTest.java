package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.flight.controller.response.FlightCityResponse;
import com.restroute.flight.domain.FlightCityEntity;
import com.restroute.flight.repository.FlightCityRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightCityQueryServiceTest {

    @Mock
    private FlightCityRepository flightCityRepository;

    private FlightCityQueryService flightCityQueryService;

    @BeforeEach
    void setUp() {
        flightCityQueryService = new FlightCityQueryService(flightCityRepository);
    }

    @Test
    @DisplayName("keyword가 있으면 korName/engName 부분 일치로 검색한다")
    void search_byKeyword_searchesByKorOrEngName() {
        FlightCityEntity osaka = new FlightCityEntity("OSA", "오사카", "Osaka", "JP");
        when(flightCityRepository.findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                        "오사카", "오사카"))
                .thenReturn(List.of(osaka));

        List<FlightCityResponse> result = flightCityQueryService.search("오사카");

        assertThat(result).extracting(FlightCityResponse::code).containsExactly("OSA");
    }

    @Test
    @DisplayName("keyword가 없으면 전체 목록을 조회한다")
    void search_returnsAll_whenKeywordMissing() {
        FlightCityEntity osaka = new FlightCityEntity("OSA", "오사카", "Osaka", "JP");
        when(flightCityRepository.findAll()).thenReturn(List.of(osaka));

        List<FlightCityResponse> result = flightCityQueryService.search(null);

        assertThat(result).extracting(FlightCityResponse::code).containsExactly("OSA");
    }
}

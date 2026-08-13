package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.flight.controller.response.FlightAirlineResponse;
import com.restroute.flight.domain.FlightAirlineEntity;
import com.restroute.flight.repository.FlightAirlineRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightAirlineQueryServiceTest {

    @Mock
    private FlightAirlineRepository flightAirlineRepository;

    private FlightAirlineQueryService flightAirlineQueryService;

    @BeforeEach
    void setUp() {
        flightAirlineQueryService = new FlightAirlineQueryService(flightAirlineRepository);
    }

    @Test
    @DisplayName("keyword가 있으면 korName/engName 부분 일치로 검색한다")
    void search_byKeyword_searchesByKorOrEngName() {
        FlightAirlineEntity jejuAir = new FlightAirlineEntity("7C", "제주항공", "Jeju Air");
        when(flightAirlineRepository.findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                        "제주", "제주"))
                .thenReturn(List.of(jejuAir));

        List<FlightAirlineResponse> result = flightAirlineQueryService.search("제주");

        assertThat(result).extracting(FlightAirlineResponse::code).containsExactly("7C");
    }

    @Test
    @DisplayName("keyword가 없으면 전체 목록을 조회한다")
    void search_returnsAll_whenKeywordMissing() {
        FlightAirlineEntity jejuAir = new FlightAirlineEntity("7C", "제주항공", "Jeju Air");
        when(flightAirlineRepository.findAll()).thenReturn(List.of(jejuAir));

        List<FlightAirlineResponse> result = flightAirlineQueryService.search(null);

        assertThat(result).extracting(FlightAirlineResponse::code).containsExactly("7C");
    }
}

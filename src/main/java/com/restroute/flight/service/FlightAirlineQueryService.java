package com.restroute.flight.service;

import com.restroute.flight.controller.response.FlightAirlineResponse;
import com.restroute.flight.repository.FlightAirlineRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FlightAirlineQueryService {

    private final FlightAirlineRepository flightAirlineRepository;

    @Transactional(readOnly = true)
    public List<FlightAirlineResponse> search(String keyword) {
        if (StringUtils.hasText(keyword)) {
            return flightAirlineRepository
                    .findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                            keyword, keyword)
                    .stream()
                    .map(FlightAirlineResponse::from)
                    .toList();
        }

        return flightAirlineRepository.findAll().stream()
                .map(FlightAirlineResponse::from)
                .toList();
    }
}

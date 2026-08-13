package com.restroute.flight.service;

import com.restroute.flight.controller.response.FlightAirportResponse;
import com.restroute.flight.repository.FlightAirportRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FlightAirportQueryService {

    private final FlightAirportRepository flightAirportRepository;

    @Transactional(readOnly = true)
    public List<FlightAirportResponse> search(String keyword) {
        if (StringUtils.hasText(keyword)) {
            return flightAirportRepository
                    .findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                            keyword, keyword)
                    .stream()
                    .map(FlightAirportResponse::from)
                    .toList();
        }

        return flightAirportRepository.findAll().stream()
                .map(FlightAirportResponse::from)
                .toList();
    }
}

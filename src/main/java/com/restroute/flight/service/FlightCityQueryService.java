package com.restroute.flight.service;

import com.restroute.flight.controller.response.FlightCityResponse;
import com.restroute.flight.repository.FlightCityRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FlightCityQueryService {

    private final FlightCityRepository flightCityRepository;

    @Transactional(readOnly = true)
    public List<FlightCityResponse> search(String keyword) {
        if (StringUtils.hasText(keyword)) {
            return flightCityRepository
                    .findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                            keyword, keyword)
                    .stream()
                    .map(FlightCityResponse::from)
                    .toList();
        }

        return flightCityRepository.findAll().stream()
                .map(FlightCityResponse::from)
                .toList();
    }
}

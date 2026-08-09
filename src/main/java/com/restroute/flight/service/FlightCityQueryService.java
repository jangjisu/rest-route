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
    public List<FlightCityResponse> search(String keyword, String regionGroup) {
        if (StringUtils.hasText(keyword)) {
            return flightCityRepository
                    .findAllByNameContainingIgnoreCaseOrNameKoContainingIgnoreCaseOrderByNameKoAsc(keyword, keyword)
                    .stream()
                    .map(FlightCityResponse::from)
                    .toList();
        }

        if (StringUtils.hasText(regionGroup)) {
            return flightCityRepository.findAllByRegionGroupOrderByNameKoAsc(regionGroup).stream()
                    .map(FlightCityResponse::from)
                    .toList();
        }

        return flightCityRepository.findAll().stream()
                .map(FlightCityResponse::from)
                .toList();
    }
}

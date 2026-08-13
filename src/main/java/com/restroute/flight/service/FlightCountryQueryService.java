package com.restroute.flight.service;

import com.restroute.flight.controller.response.FlightCountryResponse;
import com.restroute.flight.repository.FlightCountryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FlightCountryQueryService {

    private final FlightCountryRepository flightCountryRepository;

    @Transactional(readOnly = true)
    public List<FlightCountryResponse> search(String keyword) {
        if (StringUtils.hasText(keyword)) {
            return flightCountryRepository
                    .findAllByKorNameContainingIgnoreCaseOrEngNameContainingIgnoreCaseOrderByKorNameAsc(
                            keyword, keyword)
                    .stream()
                    .map(FlightCountryResponse::from)
                    .toList();
        }

        return flightCountryRepository.findAll().stream()
                .map(FlightCountryResponse::from)
                .toList();
    }
}

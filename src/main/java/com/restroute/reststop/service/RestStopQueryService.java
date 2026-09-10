package com.restroute.reststop.service;

import com.restroute.reststop.controller.response.RestStopDetailViewResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestStopQueryService {

    private final RestStopRepository restStopRepository;

    @Transactional(readOnly = true)
    public List<RestStopEntity> findAll() {
        return restStopRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<RestStopDetailViewResponse> findDetailByServiceAreaCode(String serviceAreaCode) {
        return restStopRepository.findByServiceAreaCode(serviceAreaCode).map(RestStopDetailViewResponse::from);
    }

    @Transactional(readOnly = true)
    public List<RestStopEntity> searchByName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return restStopRepository.findByUnitNameContainingIgnoreCase(trimmed);
    }
}

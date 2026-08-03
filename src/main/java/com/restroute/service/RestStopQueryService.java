package com.restroute.service;

import com.restroute.controller.response.RestStopDetailViewResponse;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestStopRepository;
import java.util.Collection;
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

    /**
     * serviceAreaCodes가 null이거나 비어 있으면 코드로 거르지 않고, adminOverridden이 null이면
     * override 여부로 거르지 않는다. 예: 경로 조회는 특정 코드만, override 여부는 상관없이(null)
     * 조회하고, backfill은 코드 제한 없이(null) override=false인 행만 조회한다.
     */
    @Transactional(readOnly = true)
    public List<RestStopEntity> findByServiceAreaCodesAndAdminOverridden(
            Collection<String> serviceAreaCodes, Boolean adminOverridden) {
        Collection<String> normalizedCodes =
                (serviceAreaCodes == null || serviceAreaCodes.isEmpty()) ? null : serviceAreaCodes;
        return restStopRepository.findByServiceAreaCodesAndAdminOverridden(normalizedCodes, adminOverridden);
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

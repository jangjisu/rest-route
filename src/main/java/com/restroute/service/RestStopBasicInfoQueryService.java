package com.restroute.service;

import com.restroute.controller.response.RestStopBasicInfoResponse;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.dto.RestStopRelatedInfo;
import com.restroute.service.evcharger.EvChargerQueryService;
import com.restroute.service.image.RestStopImageQueryService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestStopBasicInfoQueryService {

    private final RestStopRepository restStopRepository;
    private final RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;
    private final EvChargerQueryService evChargerQueryService;
    private final RestStopImageQueryService restStopImageQueryService;

    @Transactional(readOnly = true)
    public Optional<RestStopBasicInfoResponse> findByServiceAreaCode(String serviceAreaCode) {
        return restStopRepository.findByServiceAreaCode(serviceAreaCode).map(this::findByRestStop);
    }

    private RestStopBasicInfoResponse findByRestStop(RestStopEntity restStop) {
        RestStopRelatedInfo relatedInfo = restStopRelatedInfoQueryService.findByRestStop(restStop);
        int evChargerCount = evChargerQueryService.findActiveChargerCount(restStop.getServiceAreaCode());
        String detailImageUrl = restStopImageQueryService.findDetailImageUrl(restStop.getServiceAreaCode());
        return RestStopBasicInfoResponse.of(
                restStop, relatedInfo.detail(), evChargerCount, detailImageUrl, relatedInfo.themes());
    }
}

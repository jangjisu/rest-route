package com.restroute.reststop.service;

import com.restroute.reststop.controller.response.RestStopFacilityResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.repository.RestStopRestroomRepository;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestStopFacilityQueryService {

    private final RestStopRepository restStopRepository;
    private final RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;
    private final RestStopRestroomRepository restStopRestroomRepository;

    @Transactional(readOnly = true)
    public Optional<RestStopFacilityResponse> findByServiceAreaCode(String serviceAreaCode) {
        return restStopRepository.findByServiceAreaCode(serviceAreaCode).map(this::findByRestStop);
    }

    private RestStopFacilityResponse findByRestStop(RestStopEntity restStop) {
        RestStopRelatedInfo relatedInfo = restStopRelatedInfoQueryService.findByRestStop(restStop);
        return RestStopFacilityResponse.of(
                relatedInfo.detail(),
                relatedInfo.highwayServiceAreaInfos(),
                restStopRestroomRepository.findByRestStopServiceAreaCode(restStop.getServiceAreaCode()));
    }
}

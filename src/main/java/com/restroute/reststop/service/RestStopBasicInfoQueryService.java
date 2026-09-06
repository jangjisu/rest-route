package com.restroute.reststop.service;

import com.restroute.evcharger.service.EvChargerQueryService;
import com.restroute.reststop.controller.response.RestStopBasicInfoResponse;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopUsageSnapshotEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.repository.RestStopUsageSnapshotRepository;
import com.restroute.reststop.service.image.RestStopImageQueryService;
import com.restroute.reststopcontent.domain.RestThemeEntity;
import java.util.List;
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
    private final RestStopUsageSnapshotRepository restStopUsageSnapshotRepository;

    @Transactional(readOnly = true)
    public Optional<RestStopBasicInfoResponse> findByServiceAreaCode(String serviceAreaCode) {
        return restStopRepository.findByServiceAreaCode(serviceAreaCode).map(this::findByRestStop);
    }

    private RestStopBasicInfoResponse findByRestStop(RestStopEntity restStop) {
        String serviceAreaCode = restStop.getServiceAreaCode();
        Optional<RestStopDetailEntity> detail = restStopRelatedInfoQueryService.findDetail(serviceAreaCode);
        List<RestThemeEntity> themes = restStopRelatedInfoQueryService.findThemes(serviceAreaCode);
        int evChargerCount = evChargerQueryService.findActiveChargerCount(serviceAreaCode);
        String detailImageUrl = restStopImageQueryService.findDetailImageUrl(serviceAreaCode);
        boolean topTrafficTier = restStopUsageSnapshotRepository
                .findByRestStopServiceAreaCode(serviceAreaCode)
                .map(RestStopUsageSnapshotEntity::isTopTrafficTier)
                .orElse(false);
        return RestStopBasicInfoResponse.of(restStop, detail, evChargerCount, detailImageUrl, themes, topTrafficTier);
    }
}

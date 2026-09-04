package com.restroute.oilprice.service;

import com.restroute.oilprice.controller.response.OilInfoResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.service.RestStopRelatedInfoQueryService;
import com.restroute.reststop.service.dto.RestStopOilInfo;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestStopOilInfoQueryService {

    private final RestStopRepository restStopRepository;
    private final RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;

    @Transactional(readOnly = true)
    public Optional<OilInfoResponse> findByServiceAreaCode(String serviceAreaCode) {
        return restStopRepository.findByServiceAreaCode(serviceAreaCode).flatMap(this::findByRestStop);
    }

    private Optional<OilInfoResponse> findByRestStop(RestStopEntity restStop) {
        RestStopOilInfo oilInfo = restStopRelatedInfoQueryService.findOilInfo(restStop.getServiceAreaCode());
        if (oilInfo.oilServiceAreaCode2().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(OilInfoResponse.from(oilInfo.oilPrice(), oilInfo.oilStationConveniences()));
    }
}

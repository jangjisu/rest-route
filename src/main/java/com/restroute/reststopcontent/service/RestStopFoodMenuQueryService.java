package com.restroute.reststopcontent.service;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.service.RestStopRelatedInfoQueryService;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import com.restroute.reststopcontent.controller.response.FoodMenuResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestStopFoodMenuQueryService {

    private final RestStopRepository restStopRepository;
    private final RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;

    @Transactional(readOnly = true)
    public Optional<FoodMenuResponse> findByServiceAreaCode(String serviceAreaCode) {
        return restStopRepository.findByServiceAreaCode(serviceAreaCode).map(this::findByRestStop);
    }

    private FoodMenuResponse findByRestStop(RestStopEntity restStop) {
        RestStopRelatedInfo relatedInfo = restStopRelatedInfoQueryService.findByRestStop(restStop);
        return FoodMenuResponse.from(relatedInfo.foods());
    }
}

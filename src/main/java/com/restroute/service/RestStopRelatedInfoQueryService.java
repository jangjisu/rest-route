package com.restroute.service;

import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.domain.RestEventEntity;
import com.restroute.domain.RestFoodEntity;
import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.domain.RestThemeEntity;
import com.restroute.repository.HighwayServiceAreaInfoRepository;
import com.restroute.repository.RestEventRepository;
import com.restroute.repository.RestFoodRepository;
import com.restroute.repository.RestOilPriceRepository;
import com.restroute.repository.RestOilRepository;
import com.restroute.repository.RestStopDetailRepository;
import com.restroute.repository.RestThemeRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RestStopRelatedInfoQueryService {

    private final RestStopDetailRepository restStopDetailRepository;
    private final HighwayServiceAreaInfoRepository highwayServiceAreaInfoRepository;
    private final RestOilRepository restOilRepository;
    private final RestOilPriceRepository restOilPriceRepository;
    private final RestFoodRepository restFoodRepository;
    private final RestThemeRepository restThemeRepository;
    private final RestEventRepository restEventRepository;

    @Transactional(readOnly = true)
    public RestStopRelatedInfo findByRestStop(RestStopEntity restStop) {
        String serviceAreaCode = restStop.getServiceAreaCode();
        Optional<RestStopDetailEntity> detail = restStopDetailRepository.findByRestStopServiceAreaCode(serviceAreaCode);
        List<HighwayServiceAreaInfoEntity> infos = findHighwayServiceAreaInfos(serviceAreaCode);
        List<RestOilEntity> oilConveniences = findOilStationConveniences(serviceAreaCode);
        Optional<String> oilServiceAreaCode2 = firstOilServiceAreaCode2(oilConveniences);
        Optional<RestOilPriceEntity> oilPrice = findOilPrice(serviceAreaCode, oilServiceAreaCode2);
        List<RestFoodEntity> foods = findFoods(serviceAreaCode);
        List<RestThemeEntity> themes = findThemes(serviceAreaCode);
        List<RestEventEntity> events = findEvents(serviceAreaCode);

        return RestStopRelatedInfo.of(
                detail, infos, oilConveniences, oilServiceAreaCode2, oilPrice, foods, themes, events);
    }

    private List<HighwayServiceAreaInfoEntity> findHighwayServiceAreaInfos(String serviceAreaCode) {
        return highwayServiceAreaInfoRepository.findAllByRestStopServiceAreaCode(serviceAreaCode);
    }

    private List<RestOilEntity> findOilStationConveniences(String serviceAreaCode) {
        return restOilRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc(serviceAreaCode);
    }

    private Optional<RestOilPriceEntity> findOilPrice(String serviceAreaCode, Optional<String> oilServiceAreaCode2) {
        if (oilServiceAreaCode2.isEmpty()) {
            return Optional.empty();
        }

        return restOilPriceRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc(serviceAreaCode).stream()
                .findFirst();
    }

    private List<RestFoodEntity> findFoods(String serviceAreaCode) {
        return restFoodRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc(serviceAreaCode);
    }

    private List<RestThemeEntity> findThemes(String serviceAreaCode) {
        return restThemeRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc(serviceAreaCode);
    }

    private List<RestEventEntity> findEvents(String serviceAreaCode) {
        return restEventRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc(serviceAreaCode);
    }

    private Optional<String> firstOilServiceAreaCode2(List<RestOilEntity> oilConveniences) {
        return oilConveniences.stream()
                .map(RestOilEntity::getStandardRestCode)
                .filter(StringUtils::hasText)
                .findFirst();
    }
}

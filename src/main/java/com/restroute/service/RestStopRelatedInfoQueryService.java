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
import com.restroute.service.dto.RestStopRelatedInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    /**
     * findByRestStop을 후보마다 반복 호출하면 후보 수 x 7개의 쿼리가 발생한다(N+1).
     * 경로 탐색처럼 여러 휴게소를 한 번에 다뤄야 하는 호출부는 이 배치 메서드를 써야 한다.
     *
     * <p>adminOverridden이 null이면 override 여부와 상관없이 전부 조회하고, false를 넘기면
     * 관리자가 override하지 않은 행만 조회한다(각 도메인 테이블 자체의 override 필드 기준).
     * theme/event는 override 개념이 없는 테이블이라 이 파라미터의 영향을 받지 않는다.
     */
    @Transactional(readOnly = true)
    public Map<String, RestStopRelatedInfo> findAllByRestStops(
            List<RestStopEntity> restStops, Boolean adminOverridden) {
        List<String> serviceAreaCodes = restStops.stream()
                .map(RestStopEntity::getServiceAreaCode)
                .distinct()
                .toList();
        if (serviceAreaCodes.isEmpty()) {
            return Map.of();
        }

        Map<String, RestStopDetailEntity> detailsByCode =
                restStopDetailRepository
                        .findByRestStopServiceAreaCodesAndAdminOverridden(serviceAreaCodes, adminOverridden)
                        .stream()
                        .collect(Collectors.toMap(
                                RestStopDetailEntity::getRestStopServiceAreaCode,
                                Function.identity(),
                                (first, second) -> first));
        Map<String, List<HighwayServiceAreaInfoEntity>> infosByCode =
                highwayServiceAreaInfoRepository.findAllByRestStopServiceAreaCodeIn(serviceAreaCodes).stream()
                        .collect(Collectors.groupingBy(HighwayServiceAreaInfoEntity::getRestStopServiceAreaCode));
        Map<String, List<RestOilEntity>> oilConveniencesByCode =
                restOilRepository
                        .findByRestStopServiceAreaCodesAndAdminOverridden(serviceAreaCodes, adminOverridden)
                        .stream()
                        .collect(Collectors.groupingBy(RestOilEntity::getRestStopServiceAreaCode));
        Map<String, List<RestOilPriceEntity>> oilPricesByCode =
                restOilPriceRepository
                        .findByRestStopServiceAreaCodesAndAdminOverridden(serviceAreaCodes, adminOverridden)
                        .stream()
                        .collect(Collectors.groupingBy(RestOilPriceEntity::getRestStopServiceAreaCode));
        Map<String, List<RestFoodEntity>> foodsByCode =
                restFoodRepository
                        .findByRestStopServiceAreaCodesAndAdminOverridden(serviceAreaCodes, adminOverridden)
                        .stream()
                        .collect(Collectors.groupingBy(RestFoodEntity::getRestStopServiceAreaCode));
        Map<String, List<RestThemeEntity>> themesByCode =
                restThemeRepository.findAllByRestStopServiceAreaCodeIn(serviceAreaCodes).stream()
                        .collect(Collectors.groupingBy(RestThemeEntity::getRestStopServiceAreaCode));
        Map<String, List<RestEventEntity>> eventsByCode =
                restEventRepository.findAllByRestStopServiceAreaCodeIn(serviceAreaCodes).stream()
                        .collect(Collectors.groupingBy(RestEventEntity::getRestStopServiceAreaCode));

        return serviceAreaCodes.stream().collect(Collectors.toMap(Function.identity(), serviceAreaCode -> {
            List<RestOilEntity> oilConveniences = oilConveniencesByCode.getOrDefault(serviceAreaCode, List.of());
            Optional<String> oilServiceAreaCode2 = firstOilServiceAreaCode2(oilConveniences);
            Optional<RestOilPriceEntity> oilPrice = oilServiceAreaCode2.isEmpty()
                    ? Optional.empty()
                    : oilPricesByCode.getOrDefault(serviceAreaCode, List.of()).stream()
                            .findFirst();
            return RestStopRelatedInfo.of(
                    Optional.ofNullable(detailsByCode.get(serviceAreaCode)),
                    infosByCode.getOrDefault(serviceAreaCode, List.of()),
                    oilConveniences,
                    oilServiceAreaCode2,
                    oilPrice,
                    foodsByCode.getOrDefault(serviceAreaCode, List.of()),
                    themesByCode.getOrDefault(serviceAreaCode, List.of()),
                    eventsByCode.getOrDefault(serviceAreaCode, List.of()));
        }));
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

package com.restroute.service;

import com.restroute.domain.RestStopEntity;
import com.restroute.service.evcharger.EvChargerQueryService;
import com.restroute.service.image.RestStopImageQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REST_STOP_SERVICE_AREA_CODE로 연결되는 휴게소 관련 정보(상세/주유/음식/테마/이벤트/EV차저/이미지)를
 * 한 번에 조합해 반환한다. serviceAreaCodes가 null이거나 비어 있으면 코드로 거르지 않고,
 * adminOverridden이 null이면 override 여부로 거르지 않는다. 예: 경로 조회는 특정 코드만, override
 * 여부는 상관없이(null) 조회하고, backfill은 코드 제한 없이(null) override=false인 것만 조회한다.
 *
 * <p>{@link RouteRestStopService}, {@code RestStopServiceAreaCodeBackfillService}처럼 여러 QueryService를
 * 각자 호출하고 서비스 코드 기준으로 직접 짜맞추던 로직을 이 서비스 하나로 대체하기 위한 것이다.
 * 기존 소비자를 이 서비스로 교체하는 작업은 아직 하지 않았다 — 우선 조합 형태(뼈대)만 확정한다.
 */
@Service
@RequiredArgsConstructor
public class RestStopAggregateQueryService {

    private final RestStopQueryService restStopQueryService;
    private final RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;
    private final EvChargerQueryService evChargerQueryService;
    private final RestStopImageQueryService restStopImageQueryService;
    private final RestThemeQueryService restThemeQueryService;
    private final RestStopEventQueryService restStopEventQueryService;

    @Transactional(readOnly = true)
    public Map<String, RestStopAggregate> findByServiceAreaCodesAndAdminOverridden(
            Collection<String> serviceAreaCodes, Boolean adminOverridden) {
        List<RestStopEntity> restStops =
                restStopQueryService.findByServiceAreaCodesAndAdminOverridden(serviceAreaCodes, adminOverridden);
        if (restStops.isEmpty()) {
            return Map.of();
        }

        List<String> codes =
                restStops.stream().map(RestStopEntity::getServiceAreaCode).toList();
        Map<String, RestStopRelatedInfo> relatedInfoByCode =
                restStopRelatedInfoQueryService.findAllByRestStops(restStops, adminOverridden);
        Set<String> evChargerCodes = Set.copyOf(evChargerQueryService.findChargerMappedServiceAreaCodes(codes));
        Set<String> imageCodes = restStopImageQueryService.findExistingServiceAreaCodes(codes);
        Set<String> themeCodes = Set.copyOf(restThemeQueryService.findThemeMappedServiceAreaCodes(codes));
        Set<String> eventCodes = Set.copyOf(restStopEventQueryService.findActiveEventMappedServiceAreaCodes(codes));

        return restStops.stream()
                .collect(Collectors.toMap(
                        RestStopEntity::getServiceAreaCode,
                        restStop -> RestStopAggregate.of(
                                restStop,
                                relatedInfoByCode.get(restStop.getServiceAreaCode()),
                                evChargerCodes.contains(restStop.getServiceAreaCode()),
                                imageCodes.contains(restStop.getServiceAreaCode()),
                                themeCodes.contains(restStop.getServiceAreaCode()),
                                eventCodes.contains(restStop.getServiceAreaCode())),
                        (first, second) -> first));
    }
}

package com.restroute.reststop.service;

import com.restroute.evcharger.service.EvChargerQueryService;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.dto.RestStopAggregate;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import com.restroute.reststop.service.image.RestStopImageQueryService;
import com.restroute.reststopcontent.service.RestStopEventQueryService;
import com.restroute.reststopcontent.service.RestThemeQueryService;
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
 * {@code RouteOptionAssemblyService}가 이 서비스를 사용해 경로 상의 휴게소 정보를 조합한다.
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
        return findByRestStopsAndAdminOverridden(restStops, adminOverridden);
    }

    /**
     * 호출부가 이미 조회해둔 {@link RestStopEntity} 목록으로 집계한다 — 위 메서드가 매번 새로
     * 조회하는 것과 달리, 같은 요청 안에서 이미 가진 엔티티를 재사용해 중복 조회를 없앨 때 쓴다.
     * restStops를 어떤 기준으로 걸렀는지는 호출부 책임이고, 여기서는 그대로 신뢰해서 쓴다.
     */
    @Transactional(readOnly = true)
    public Map<String, RestStopAggregate> findByRestStopsAndAdminOverridden(
            List<RestStopEntity> restStops, Boolean adminOverridden) {
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

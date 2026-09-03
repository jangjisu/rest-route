package com.restroute.reststop.service;

import com.restroute.evcharger.service.EvChargerQueryService;
import com.restroute.evcharger.service.util.CoordinateDistanceCalculator;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.service.NationalOilPriceService;
import com.restroute.reststop.controller.response.RestStopNearbyItemResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.dto.RestStopAggregate;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.route.dto.FuelType;
import com.restroute.route.dto.FuelTypeSelection;
import com.restroute.route.service.util.RouteCoordinateFormat;
import com.restroute.route.service.util.RouteRestStopNumberParser;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * "이름·거리로 찾기" 목록 하나로 통합된 조회 — 위치/이름/관심 항목이 몇 개 있든 이 서비스 하나로
 * 처리한다. 값이 없는 파라미터는 대응하는 응답 필드를 null로 남긴다.
 */
@Service
@RequiredArgsConstructor
public class RestStopNearbyQueryService {

    private final RestStopQueryService restStopQueryService;
    private final RestStopAggregateQueryService restStopAggregateQueryService;
    private final EvChargerQueryService evChargerQueryService;
    private final NationalOilPriceService nationalOilPriceService;

    @Transactional(readOnly = true)
    public List<RestStopNearbyItemResponse> findNearby(
            Double originLat, Double originLng, String name, FuelTypeSelection fuelSelection) {
        List<RestStopEntity> restStops =
                StringUtils.hasText(name) ? restStopQueryService.searchByName(name) : restStopQueryService.findAll();
        if (restStops.isEmpty()) {
            return List.of();
        }

        Map<String, RestStopAggregate> aggregatesByServiceAreaCode =
                restStopAggregateQueryService.findByRestStopsAndAdminOverridden(restStops, null);
        Map<String, Integer> evChargerCountsByServiceAreaCode = evChargerCountsFor(fuelSelection, restStops);
        Optional<NationalOilPriceSummary> nationalOilPriceSummary =
                fuelSelection.wantsFuelPriceInfo() ? nationalOilPriceService.getTodaySummary() : Optional.empty();

        List<RestStopNearbyItemResponse> items = restStops.stream()
                .map(restStop -> toItem(
                        restStop,
                        originLat,
                        originLng,
                        fuelSelection,
                        aggregatesByServiceAreaCode.get(restStop.getServiceAreaCode()),
                        evChargerCountsByServiceAreaCode,
                        nationalOilPriceSummary))
                .toList();

        if (originLat == null || originLng == null) {
            return items;
        }
        return items.stream()
                .sorted(Comparator.comparing(
                        RestStopNearbyItemResponse::distanceMeters, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private RestStopNearbyItemResponse toItem(
            RestStopEntity restStop,
            Double originLat,
            Double originLng,
            FuelTypeSelection fuelSelection,
            RestStopAggregate aggregate,
            Map<String, Integer> evChargerCountsByServiceAreaCode,
            Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        Double distance = distanceMeters(originLat, originLng, restStop);
        if (fuelSelection.wantsFuelPriceInfo()) {
            return RestStopNearbyItemResponse.ofFuelPriceInfo(
                    restStop, distance, aggregate, fuelBelowAverage(aggregate, fuelSelection, nationalOilPriceSummary));
        }
        return RestStopNearbyItemResponse.ofEvChargerInfo(
                restStop, distance, aggregate, evChargerCount(restStop, evChargerCountsByServiceAreaCode));
    }

    private Map<String, Integer> evChargerCountsFor(FuelTypeSelection fuelSelection, List<RestStopEntity> restStops) {
        if (!fuelSelection.wantsEvChargerInfo()) {
            return Map.of();
        }
        return evChargerQueryService.findActiveChargerCounts(
                restStops.stream().map(RestStopEntity::getServiceAreaCode).toList());
    }

    private Double distanceMeters(Double originLat, Double originLng, RestStopEntity restStop) {
        if (originLat == null || originLng == null) {
            return null;
        }
        Double latitude = RouteCoordinateFormat.parse(restStop.getYValue());
        Double longitude = RouteCoordinateFormat.parse(restStop.getXValue());
        if (latitude == null || longitude == null) {
            return null;
        }
        return CoordinateDistanceCalculator.meters(originLat, originLng, latitude, longitude);
    }

    private Integer evChargerCount(RestStopEntity restStop, Map<String, Integer> evChargerCountsByServiceAreaCode) {
        Integer count = evChargerCountsByServiceAreaCode.get(restStop.getServiceAreaCode());
        return count == null || count <= 0 ? null : count;
    }

    /**
     * 선택한 유종이 오늘자 오피넷 전국 평균보다 쌀 때만 true, 그 외엔(비싸거나 데이터가 없으면) null —
     * "평균보다 안 싸다"는 배지 자체를 안 보여줘야 해서 false를 따로 구분하지 않는다.
     */
    private Boolean fuelBelowAverage(
            RestStopAggregate aggregate,
            FuelTypeSelection fuelSelection,
            Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        if (aggregate == null || nationalOilPriceSummary.isEmpty()) {
            return null;
        }
        Optional<RestOilPriceEntity> oilPrice = aggregate.relatedInfo().oilPrice();
        if (oilPrice.isEmpty()) {
            return null;
        }

        FuelType fuelType = fuelSelection.fuelType();
        Optional<Integer> price =
                RouteRestStopNumberParser.parsePrice(oilPrice.get().getPriceByFuelType(fuelType));
        Optional<Integer> average = RouteRestStopNumberParser.parsePrice(
                nationalOilPriceSummary.get().getAveragePriceByFuelType(fuelType));
        if (price.isEmpty() || average.isEmpty()) {
            return null;
        }
        return price.get() < average.get() ? true : null;
    }
}

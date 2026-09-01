package com.restroute.reststop.service;

import com.restroute.evcharger.service.EvChargerQueryService;
import com.restroute.evcharger.service.util.CoordinateDistanceCalculator;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.service.NationalOilPriceService;
import com.restroute.reststop.controller.response.RestStopNearbyItemResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.dto.RestStopAggregate;
import com.restroute.reststop.service.dto.RestStopInterest;
import com.restroute.route.controller.response.RouteRestStopResponse.AverageOilPrice;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
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
            Double originLat, Double originLng, String name, RestStopInterest interest) {
        List<RestStopEntity> restStops =
                StringUtils.hasText(name) ? restStopQueryService.searchByName(name) : restStopQueryService.findAll();
        if (restStops.isEmpty()) {
            return List.of();
        }

        Map<String, RestStopAggregate> aggregatesByServiceAreaCode =
                restStopAggregateQueryService.findByRestStopsAndAdminOverridden(restStops, null);
        Map<String, Integer> evChargerCountsByServiceAreaCode = evChargerCountsFor(interest, restStops);
        Optional<NationalOilPriceSummary> nationalOilPriceSummary =
                isFuelInterest(interest) ? nationalOilPriceService.getTodaySummary() : Optional.empty();

        List<RestStopNearbyItemResponse> items = restStops.stream()
                .map(restStop -> toItem(
                        restStop,
                        originLat,
                        originLng,
                        interest,
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

    private Map<String, Integer> evChargerCountsFor(RestStopInterest interest, List<RestStopEntity> restStops) {
        if (interest != RestStopInterest.EV) {
            return Map.of();
        }
        return evChargerQueryService.findActiveChargerCounts(
                restStops.stream().map(RestStopEntity::getServiceAreaCode).toList());
    }

    private RestStopNearbyItemResponse toItem(
            RestStopEntity restStop,
            Double originLat,
            Double originLng,
            RestStopInterest interest,
            RestStopAggregate aggregate,
            Map<String, Integer> evChargerCountsByServiceAreaCode,
            Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        return RestStopNearbyItemResponse.of(
                restStop,
                distanceMeters(originLat, originLng, restStop),
                aggregate == null ? null : aggregate.sizeTier(),
                aggregate != null && aggregate.topTrafficTier(),
                aggregate != null && aggregate.hasTheme(),
                aggregate != null && aggregate.hasEvent(),
                evChargerCount(restStop, interest, evChargerCountsByServiceAreaCode),
                fuelBelowAverage(aggregate, interest, nationalOilPriceSummary));
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

    private Integer evChargerCount(
            RestStopEntity restStop, RestStopInterest interest, Map<String, Integer> evChargerCountsByServiceAreaCode) {
        if (interest != RestStopInterest.EV) {
            return null;
        }
        Integer count = evChargerCountsByServiceAreaCode.get(restStop.getServiceAreaCode());
        return count == null || count <= 0 ? null : count;
    }

    private boolean isFuelInterest(RestStopInterest interest) {
        return interest == RestStopInterest.GASOLINE
                || interest == RestStopInterest.DIESEL
                || interest == RestStopInterest.LPG;
    }

    /**
     * 선택한 유종이 오늘자 오피넷 전국 평균보다 쌀 때만 true, 그 외엔(비싸거나 데이터가 없으면) null —
     * "평균보다 안 싸다"는 배지 자체를 안 보여줘야 해서 false를 따로 구분하지 않는다.
     */
    private Boolean fuelBelowAverage(
            RestStopAggregate aggregate,
            RestStopInterest interest,
            Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        if (aggregate == null || !isFuelInterest(interest) || nationalOilPriceSummary.isEmpty()) {
            return null;
        }
        Optional<RestOilPriceEntity> oilPrice = aggregate.relatedInfo().oilPrice();
        if (oilPrice.isEmpty()) {
            return null;
        }

        Optional<Integer> price = RouteRestStopNumberParser.parsePrice(fuelPrice(oilPrice.get(), interest));
        Optional<Integer> average =
                RouteRestStopNumberParser.parsePrice(nationalAveragePrice(nationalOilPriceSummary.get(), interest));
        if (price.isEmpty() || average.isEmpty()) {
            return null;
        }
        return price.get() < average.get() ? true : null;
    }

    private String fuelPrice(RestOilPriceEntity oilPrice, RestStopInterest interest) {
        return switch (interest) {
            case GASOLINE -> oilPrice.getGasolinePrice();
            case DIESEL -> oilPrice.getDieselPrice();
            case LPG -> oilPrice.getLpgPrice();
            default -> null;
        };
    }

    private String nationalAveragePrice(NationalOilPriceSummary summary, RestStopInterest interest) {
        AverageOilPrice averageOilPrice =
                switch (interest) {
                    case GASOLINE -> summary.gasoline();
                    case DIESEL -> summary.diesel();
                    case LPG -> summary.lpg();
                    default -> null;
                };
        return averageOilPrice == null ? null : averageOilPrice.price();
    }
}

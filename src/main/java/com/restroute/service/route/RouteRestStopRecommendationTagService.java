package com.restroute.service.route;

import com.restroute.controller.response.RouteRestStopResponse.ComparisonSummary;
import com.restroute.controller.response.RouteRestStopResponse.RecommendationTag;
import com.restroute.service.route.dto.FuelType;
import com.restroute.service.route.dto.RouteRestStopComparison;
import com.restroute.service.route.dto.RouteRestStopRecommendationStandards;
import com.restroute.service.route.util.RouteRestStopNumberParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class RouteRestStopRecommendationTagService {

    private static final int MANY_FACILITIES_THRESHOLD = 3;

    RouteRestStopRecommendationStandards standards(List<RouteRestStopComparison> comparisons) {
        return RouteRestStopRecommendationStandards.of(
                lowestPrice(comparisons, FuelType.GASOLINE),
                lowestPrice(comparisons, FuelType.DIESEL),
                lowestPrice(comparisons, FuelType.LPG),
                largestParkingCount(comparisons));
    }

    List<RecommendationTag> create(RouteRestStopComparison comparison, RouteRestStopRecommendationStandards standards) {
        List<RecommendationTag> tags = new ArrayList<>();
        addLowestPriceTag(tags, comparison.summary(), FuelType.GASOLINE, standards.lowestGasolinePrice());
        addLowestPriceTag(tags, comparison.summary(), FuelType.DIESEL, standards.lowestDieselPrice());
        addLowestPriceTag(tags, comparison.summary(), FuelType.LPG, standards.lowestLpgPrice());
        addLargestParkingTag(tags, comparison.summary(), standards.largestParkingCount());
        addFoodTag(tags, comparison.summary());
        addFacilityTag(tags, comparison.summary());
        return tags;
    }

    private Integer lowestPrice(List<RouteRestStopComparison> comparisons, FuelType fuelType) {
        return comparisons.stream()
                .map(comparison -> priceOf(comparison.summary(), fuelType))
                .flatMap(Optional::stream)
                .min(Integer::compareTo)
                .orElse(null);
    }

    private Integer largestParkingCount(List<RouteRestStopComparison> comparisons) {
        return comparisons.stream()
                .map(comparison -> comparison.summary().totalParkingCount())
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private void addLowestPriceTag(
            List<RecommendationTag> tags, ComparisonSummary summary, FuelType fuelType, Integer lowestPrice) {
        Optional<Integer> price = priceOf(summary, fuelType);
        if (price.isEmpty()) {
            return;
        }
        if (!price.get().equals(lowestPrice)) {
            return;
        }
        tags.add(RecommendationTag.of(fuelType.tagKey(), fuelType.tagLabel()));
    }

    private void addLargestParkingTag(
            List<RecommendationTag> tags, ComparisonSummary summary, Integer largestParkingCount) {
        if (largestParkingCount == null || !largestParkingCount.equals(summary.totalParkingCount())) {
            return;
        }
        tags.add(RecommendationTag.of("largest-parking", "주차장 큼"));
    }

    private void addFoodTag(List<RecommendationTag> tags, ComparisonSummary summary) {
        if (summary.foodMenuCount() <= 0) {
            return;
        }
        tags.add(RecommendationTag.of("food-available", "먹거리 있음"));
    }

    private void addFacilityTag(List<RecommendationTag> tags, ComparisonSummary summary) {
        if (summary.facilityCount() < MANY_FACILITIES_THRESHOLD) {
            return;
        }
        tags.add(RecommendationTag.of("many-facilities", "시설 많음"));
    }

    private Optional<Integer> priceOf(ComparisonSummary summary, FuelType fuelType) {
        return switch (fuelType) {
            case GASOLINE -> RouteRestStopNumberParser.parsePrice(summary.gasolinePrice());
            case DIESEL -> RouteRestStopNumberParser.parsePrice(summary.dieselPrice());
            case LPG -> RouteRestStopNumberParser.parsePrice(summary.lpgPrice());
        };
    }
}

package com.restroute.route.service;

import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.route.controller.response.FuelPriceTier;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.route.dto.FuelType;
import com.restroute.route.service.dto.QueriedOilPriceStats;
import com.restroute.route.service.util.RouteRestStopNumberParser;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link RouteRestStopComparisonSummaryService#fuelPriceTier}와 같은 CHEAPEST/BELOW_AVERAGE
 * 두 단계 판정이지만, 세 유종을 전부 보는 대신 요청에 실린 유종 하나만 비교한다(finder
 * "목적지로 추천받기"의 연료 선택 팝업 전용). 지도 화면이 쓰는 기존 판정 로직은 그대로 둔다.
 */
@Component
public class RouteRestStopFuelTierCalculator {

    public FuelPriceTier tier(
            FuelType fuelType,
            Optional<RestOilPriceEntity> oilPrice,
            QueriedOilPriceStats queriedStats,
            Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        if (fuelType == null || oilPrice.isEmpty()) {
            return null;
        }
        Optional<Integer> price =
                RouteRestStopNumberParser.parsePrice(oilPrice.get().getPriceByFuelType(fuelType));
        if (price.isEmpty()) {
            return null;
        }
        if (price.get().equals(queriedStats.minByFuelType(fuelType))) {
            return FuelPriceTier.CHEAPEST;
        }
        if (isBelowNationalAverage(fuelType, price.get(), nationalOilPriceSummary)) {
            return FuelPriceTier.BELOW_AVERAGE;
        }
        return null;
    }

    private boolean isBelowNationalAverage(
            FuelType fuelType, int price, Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        return nationalOilPriceSummary
                .map(summary -> summary.getAveragePriceByFuelType(fuelType))
                .flatMap(RouteRestStopNumberParser::parsePrice)
                .map(average -> price < average)
                .orElse(false);
    }
}

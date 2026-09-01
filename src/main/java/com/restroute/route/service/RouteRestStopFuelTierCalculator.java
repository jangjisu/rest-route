package com.restroute.route.service;

import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.route.controller.response.FuelPriceTier;
import com.restroute.route.controller.response.RouteRestStopResponse.AverageOilPrice;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.route.service.dto.FuelType;
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
        Optional<Integer> price = RouteRestStopNumberParser.parsePrice(priceOf(fuelType, oilPrice.get()));
        if (price.isEmpty()) {
            return null;
        }
        if (price.get().equals(minOf(fuelType, queriedStats))) {
            return FuelPriceTier.CHEAPEST;
        }
        if (isBelowNationalAverage(fuelType, price.get(), nationalOilPriceSummary)) {
            return FuelPriceTier.BELOW_AVERAGE;
        }
        return null;
    }

    private String priceOf(FuelType fuelType, RestOilPriceEntity oilPrice) {
        return switch (fuelType) {
            case GASOLINE -> oilPrice.getGasolinePrice();
            case DIESEL -> oilPrice.getDieselPrice();
            case LPG -> oilPrice.getLpgPrice();
        };
    }

    private Integer minOf(FuelType fuelType, QueriedOilPriceStats queriedStats) {
        return switch (fuelType) {
            case GASOLINE -> queriedStats.gasolineMin();
            case DIESEL -> queriedStats.dieselMin();
            case LPG -> queriedStats.lpgMin();
        };
    }

    private boolean isBelowNationalAverage(
            FuelType fuelType, int price, Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        return nationalOilPriceSummary
                .map(summary -> averageOilPrice(fuelType, summary))
                .flatMap(average -> RouteRestStopNumberParser.parsePrice(average.price()))
                .map(average -> price < average)
                .orElse(false);
    }

    private AverageOilPrice averageOilPrice(FuelType fuelType, NationalOilPriceSummary summary) {
        return switch (fuelType) {
            case GASOLINE -> summary.gasoline();
            case DIESEL -> summary.diesel();
            case LPG -> summary.lpg();
        };
    }
}

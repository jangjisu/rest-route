package com.restroute.route.service;

import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.service.dto.NationalCheapestOilPrice;
import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.service.dto.RestStopRelatedInfo;
import com.restroute.route.controller.response.FuelPriceTier;
import com.restroute.route.controller.response.RouteRestStopResponse.AverageOilPrice;
import com.restroute.route.controller.response.RouteRestStopResponse.ComparisonSummary;
import com.restroute.route.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.route.service.dto.FuelType;
import com.restroute.route.service.util.RouteRestStopNumberParser;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
class RouteRestStopComparisonSummaryService {

    ComparisonSummary create(
            RestStopRelatedInfo relatedInfo, Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        Optional<RestStopDetailEntity> detail = relatedInfo.detail();
        List<HighwayServiceAreaInfoEntity> infos = relatedInfo.highwayServiceAreaInfos();
        List<RestOilEntity> oilConveniences = relatedInfo.oilStationConveniences();
        Optional<RestOilPriceEntity> oilPrice = relatedInfo.oilPrice();
        int foodMenuCount = relatedInfo.foods().size();
        return ComparisonSummary.of(
                oilPrice.map(RestOilPriceEntity::getGasolinePrice).orElse(null),
                oilPrice.map(RestOilPriceEntity::getDieselPrice).orElse(null),
                oilPrice.map(RestOilPriceEntity::getLpgPrice).orElse(null),
                diffFromAverage(
                        oilPrice.map(RestOilPriceEntity::getGasolinePrice).orElse(null),
                        nationalOilPriceSummary,
                        FuelType.GASOLINE),
                diffFromAverage(
                        oilPrice.map(RestOilPriceEntity::getDieselPrice).orElse(null),
                        nationalOilPriceSummary,
                        FuelType.DIESEL),
                diffFromAverage(
                        oilPrice.map(RestOilPriceEntity::getLpgPrice).orElse(null),
                        nationalOilPriceSummary,
                        FuelType.LPG),
                totalParkingCount(infos),
                foodMenuCount,
                facilityCount(detail, oilConveniences));
    }

    /**
     * 보유한 유종 중 하나라도 우리가 추적 중인 전국 최저가와 같으면 CHEAPEST, 아니면 하나라도
     * 전국 평균보다 싸면 BELOW_AVERAGE, 둘 다 아니면(또는 유가 정보 자체가 없으면) null.
     */
    FuelPriceTier fuelPriceTier(
            Optional<RestOilPriceEntity> oilPrice,
            Optional<NationalOilPriceSummary> nationalOilPriceSummary,
            NationalCheapestOilPrice nationalCheapest) {
        if (oilPrice.isEmpty()) {
            return null;
        }
        if (isCheapest(oilPrice.get(), nationalCheapest)) {
            return FuelPriceTier.CHEAPEST;
        }
        if (isBelowAverage(oilPrice.get(), nationalOilPriceSummary)) {
            return FuelPriceTier.BELOW_AVERAGE;
        }
        return null;
    }

    private boolean isCheapest(RestOilPriceEntity oilPrice, NationalCheapestOilPrice nationalCheapest) {
        return matchesCheapest(oilPrice.getGasolinePrice(), nationalCheapest.gasoline())
                || matchesCheapest(oilPrice.getDieselPrice(), nationalCheapest.diesel())
                || matchesCheapest(oilPrice.getLpgPrice(), nationalCheapest.lpg());
    }

    private boolean matchesCheapest(String price, Integer nationalCheapestPrice) {
        if (nationalCheapestPrice == null) {
            return false;
        }
        return RouteRestStopNumberParser.parsePrice(price)
                .map(nationalCheapestPrice::equals)
                .orElse(false);
    }

    private boolean isBelowAverage(
            RestOilPriceEntity oilPrice, Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        return isNegative(diffFromAverage(oilPrice.getGasolinePrice(), nationalOilPriceSummary, FuelType.GASOLINE))
                || isNegative(diffFromAverage(oilPrice.getDieselPrice(), nationalOilPriceSummary, FuelType.DIESEL))
                || isNegative(diffFromAverage(oilPrice.getLpgPrice(), nationalOilPriceSummary, FuelType.LPG));
    }

    private boolean isNegative(Integer diff) {
        return diff != null && diff < 0;
    }

    private Integer diffFromAverage(
            String price, Optional<NationalOilPriceSummary> nationalOilPriceSummary, FuelType fuelType) {
        Optional<Integer> parsedPrice = RouteRestStopNumberParser.parsePrice(price);
        Optional<Integer> averagePrice = averagePrice(nationalOilPriceSummary, fuelType);
        if (parsedPrice.isEmpty() || averagePrice.isEmpty()) {
            return null;
        }
        return parsedPrice.get() - averagePrice.get();
    }

    private Optional<Integer> averagePrice(
            Optional<NationalOilPriceSummary> nationalOilPriceSummary, FuelType fuelType) {
        return nationalOilPriceSummary
                .map(summary -> averageOilPrice(summary, fuelType))
                .flatMap(price -> RouteRestStopNumberParser.parsePrice(price.price()));
    }

    private AverageOilPrice averageOilPrice(NationalOilPriceSummary summary, FuelType fuelType) {
        return switch (fuelType) {
            case GASOLINE -> summary.gasoline();
            case DIESEL -> summary.diesel();
            case LPG -> summary.lpg();
        };
    }

    private Integer totalParkingCount(List<HighwayServiceAreaInfoEntity> infos) {
        int total = infos.stream()
                .mapToInt(info -> RouteRestStopNumberParser.parseCount(info.getCompactCarParkingCount())
                        + RouteRestStopNumberParser.parseCount(info.getFullSizeCarParkingCount())
                        + RouteRestStopNumberParser.parseCount(info.getDisabledParkingCount()))
                .sum();
        if (total == 0) {
            return null;
        }
        return total;
    }

    private int facilityCount(Optional<RestStopDetailEntity> detail, List<RestOilEntity> oilConveniences) {
        long detailConvenienceCount = detail.stream()
                .map(RestStopDetailEntity::getConvenience)
                .filter(StringUtils::hasText)
                .flatMap(convenience -> List.of(convenience.split("[,/]")).stream())
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        long detailFlagCount = detail.stream()
                .mapToLong(restStopDetail ->
                        ynCount(restStopDetail.getMaintenanceYn()) + ynCount(restStopDetail.getTruckSaYn()))
                .sum();
        long oilConvenienceCount = oilConveniences.stream()
                .map(RestOilEntity::getConvenienceName)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        return Math.toIntExact(detailConvenienceCount + detailFlagCount + oilConvenienceCount);
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private int ynCount(String value) {
        if ("Y".equals(value)) {
            return 1;
        }
        return 0;
    }
}

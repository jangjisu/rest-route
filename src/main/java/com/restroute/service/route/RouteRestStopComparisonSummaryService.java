package com.restroute.service.route;

import com.restroute.controller.response.RouteRestStopResponse.AverageOilPrice;
import com.restroute.controller.response.RouteRestStopResponse.ComparisonSummary;
import com.restroute.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.service.dto.RestStopRelatedInfo;
import com.restroute.service.route.dto.FuelType;
import com.restroute.service.route.util.RouteRestStopNumberParser;
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

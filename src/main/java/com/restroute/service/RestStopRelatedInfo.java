package com.restroute.service;

import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.domain.RestEventEntity;
import com.restroute.domain.RestFoodEntity;
import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.domain.RestThemeEntity;
import java.util.List;
import java.util.Optional;

public record RestStopRelatedInfo(
        Optional<RestStopDetailEntity> detail,
        List<HighwayServiceAreaInfoEntity> highwayServiceAreaInfos,
        List<RestOilEntity> oilStationConveniences,
        Optional<String> oilServiceAreaCode2,
        Optional<RestOilPriceEntity> oilPrice,
        List<RestFoodEntity> foods,
        List<RestThemeEntity> themes,
        List<RestEventEntity> events) {

    public static RestStopRelatedInfo of(
            Optional<RestStopDetailEntity> detail,
            List<HighwayServiceAreaInfoEntity> highwayServiceAreaInfos,
            List<RestOilEntity> oilStationConveniences,
            Optional<String> oilServiceAreaCode2,
            Optional<RestOilPriceEntity> oilPrice,
            List<RestFoodEntity> foods,
            List<RestThemeEntity> themes,
            List<RestEventEntity> events) {
        return new RestStopRelatedInfo(
                detail,
                highwayServiceAreaInfos,
                oilStationConveniences,
                oilServiceAreaCode2,
                oilPrice,
                foods,
                themes,
                events);
    }
}

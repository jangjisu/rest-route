package com.restroute.reststop.service.dto;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.SizeTier;

public record RestStopAggregate(
        RestStopEntity restStop,
        RestStopRelatedInfo relatedInfo,
        boolean hasEvCharger,
        boolean hasListImage,
        boolean hasTheme,
        boolean hasEvent,
        Integer maleToiletCount,
        Integer femaleToiletCount,
        boolean topTrafficTier,
        SizeTier sizeTier) {

    public static RestStopAggregate of(
            RestStopEntity restStop,
            RestStopRelatedInfo relatedInfo,
            boolean hasEvCharger,
            boolean hasListImage,
            boolean hasTheme,
            boolean hasEvent,
            Integer maleToiletCount,
            Integer femaleToiletCount,
            boolean topTrafficTier,
            SizeTier sizeTier) {
        return new RestStopAggregate(
                restStop,
                relatedInfo,
                hasEvCharger,
                hasListImage,
                hasTheme,
                hasEvent,
                maleToiletCount,
                femaleToiletCount,
                topTrafficTier,
                sizeTier);
    }
}

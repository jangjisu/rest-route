package com.restroute.reststop.service.dto;

import com.restroute.reststop.domain.RestStopEntity;

public record RestStopAggregate(
        RestStopEntity restStop,
        RestStopRelatedInfo relatedInfo,
        boolean hasEvCharger,
        boolean hasListImage,
        boolean hasTheme,
        boolean hasEvent,
        Integer maleToiletCount,
        Integer femaleToiletCount,
        boolean topTrafficTier) {

    public static RestStopAggregate of(
            RestStopEntity restStop,
            RestStopRelatedInfo relatedInfo,
            boolean hasEvCharger,
            boolean hasListImage,
            boolean hasTheme,
            boolean hasEvent,
            Integer maleToiletCount,
            Integer femaleToiletCount,
            boolean topTrafficTier) {
        return new RestStopAggregate(
                restStop,
                relatedInfo,
                hasEvCharger,
                hasListImage,
                hasTheme,
                hasEvent,
                maleToiletCount,
                femaleToiletCount,
                topTrafficTier);
    }
}

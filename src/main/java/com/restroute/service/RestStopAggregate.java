package com.restroute.service;

import com.restroute.domain.RestStopEntity;

public record RestStopAggregate(
        RestStopEntity restStop,
        RestStopRelatedInfo relatedInfo,
        boolean hasEvCharger,
        boolean hasListImage,
        boolean hasTheme,
        boolean hasEvent) {

    public static RestStopAggregate of(
            RestStopEntity restStop,
            RestStopRelatedInfo relatedInfo,
            boolean hasEvCharger,
            boolean hasListImage,
            boolean hasTheme,
            boolean hasEvent) {
        return new RestStopAggregate(restStop, relatedInfo, hasEvCharger, hasListImage, hasTheme, hasEvent);
    }
}

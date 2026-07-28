package com.restroute.controller.response;

import java.util.List;

public record RouteRestStopResponse(Destination destination, RouteSummary route, List<RouteRestStopItem> restStops) {

    public static RouteRestStopResponse of(
            Destination destination, RouteSummary route, List<RouteRestStopItem> restStops) {
        return new RouteRestStopResponse(destination, route, restStops);
    }

    public record Destination(String name, double latitude, double longitude) {

        public static Destination of(String name, double latitude, double longitude) {
            return new Destination(name, latitude, longitude);
        }
    }

    public record RouteSummary(long distanceMeters, long durationSeconds, List<List<Double>> path) {

        public static RouteSummary of(long distanceMeters, long durationSeconds, List<List<Double>> path) {
            return new RouteSummary(distanceMeters, durationSeconds, path);
        }
    }

    public record RouteRestStopItem(
            String serviceAreaCode,
            String unitName,
            String routeName,
            double latitude,
            double longitude,
            boolean hasDirectionAlternative,
            boolean hasEvCharger,
            boolean hasTheme,
            boolean hasEvent,
            long distanceFromRouteMeters,
            ComparisonSummary comparisonSummary,
            List<RecommendationTag> recommendationTags,
            String listImageUrl,
            NearbyTraffic nearbyTraffic) {

        public RouteRestStopItem(
                String serviceAreaCode,
                String unitName,
                String routeName,
                double latitude,
                double longitude,
                long distanceFromRouteMeters) {
            this(
                    serviceAreaCode,
                    unitName,
                    routeName,
                    latitude,
                    longitude,
                    false,
                    false,
                    false,
                    false,
                    distanceFromRouteMeters,
                    ComparisonSummary.empty(),
                    List.of(),
                    null,
                    null);
        }

        public static RouteRestStopItem of(
                String serviceAreaCode,
                String unitName,
                String routeName,
                double latitude,
                double longitude,
                long distanceFromRouteMeters) {
            return new RouteRestStopItem(
                    serviceAreaCode,
                    unitName,
                    routeName,
                    latitude,
                    longitude,
                    false,
                    false,
                    false,
                    false,
                    distanceFromRouteMeters,
                    ComparisonSummary.empty(),
                    List.of(),
                    null,
                    null);
        }

        public RouteRestStopItem withDirectionAlternative(boolean hasDirectionAlternative) {
            return new RouteRestStopItem(
                    serviceAreaCode,
                    unitName,
                    routeName,
                    latitude,
                    longitude,
                    hasDirectionAlternative,
                    hasEvCharger,
                    hasTheme,
                    hasEvent,
                    distanceFromRouteMeters,
                    comparisonSummary,
                    recommendationTags,
                    listImageUrl,
                    nearbyTraffic);
        }

        public RouteRestStopItem withComparison(
                ComparisonSummary comparisonSummary, List<RecommendationTag> recommendationTags) {
            return new RouteRestStopItem(
                    serviceAreaCode,
                    unitName,
                    routeName,
                    latitude,
                    longitude,
                    hasDirectionAlternative,
                    hasEvCharger,
                    hasTheme,
                    hasEvent,
                    distanceFromRouteMeters,
                    comparisonSummary,
                    List.copyOf(recommendationTags),
                    listImageUrl,
                    nearbyTraffic);
        }

        public RouteRestStopItem withEvCharger(boolean hasEvCharger) {
            return new RouteRestStopItem(
                    serviceAreaCode,
                    unitName,
                    routeName,
                    latitude,
                    longitude,
                    hasDirectionAlternative,
                    hasEvCharger,
                    hasTheme,
                    hasEvent,
                    distanceFromRouteMeters,
                    comparisonSummary,
                    recommendationTags,
                    listImageUrl,
                    nearbyTraffic);
        }

        public RouteRestStopItem withTheme(boolean hasTheme) {
            return new RouteRestStopItem(
                    serviceAreaCode,
                    unitName,
                    routeName,
                    latitude,
                    longitude,
                    hasDirectionAlternative,
                    hasEvCharger,
                    hasTheme,
                    hasEvent,
                    distanceFromRouteMeters,
                    comparisonSummary,
                    recommendationTags,
                    listImageUrl,
                    nearbyTraffic);
        }

        public RouteRestStopItem withEvent(boolean hasEvent) {
            return new RouteRestStopItem(
                    serviceAreaCode,
                    unitName,
                    routeName,
                    latitude,
                    longitude,
                    hasDirectionAlternative,
                    hasEvCharger,
                    hasTheme,
                    hasEvent,
                    distanceFromRouteMeters,
                    comparisonSummary,
                    recommendationTags,
                    listImageUrl,
                    nearbyTraffic);
        }

        public RouteRestStopItem withListImageUrl(String listImageUrl) {
            return new RouteRestStopItem(
                    serviceAreaCode,
                    unitName,
                    routeName,
                    latitude,
                    longitude,
                    hasDirectionAlternative,
                    hasEvCharger,
                    hasTheme,
                    hasEvent,
                    distanceFromRouteMeters,
                    comparisonSummary,
                    recommendationTags,
                    listImageUrl,
                    nearbyTraffic);
        }

        public RouteRestStopItem withNearbyTraffic(NearbyTraffic nearbyTraffic) {
            return new RouteRestStopItem(
                    serviceAreaCode,
                    unitName,
                    routeName,
                    latitude,
                    longitude,
                    hasDirectionAlternative,
                    hasEvCharger,
                    hasTheme,
                    hasEvent,
                    distanceFromRouteMeters,
                    comparisonSummary,
                    recommendationTags,
                    listImageUrl,
                    nearbyTraffic);
        }
    }

    public record ComparisonSummary(
            String gasolinePrice,
            String dieselPrice,
            String lpgPrice,
            Integer gasolinePriceDiffFromAverage,
            Integer dieselPriceDiffFromAverage,
            Integer lpgPriceDiffFromAverage,
            Integer totalParkingCount,
            int foodMenuCount,
            int facilityCount) {

        public static ComparisonSummary empty() {
            return new ComparisonSummary(null, null, null, null, null, null, null, 0, 0);
        }

        public static ComparisonSummary of(
                String gasolinePrice,
                String dieselPrice,
                String lpgPrice,
                Integer gasolinePriceDiffFromAverage,
                Integer dieselPriceDiffFromAverage,
                Integer lpgPriceDiffFromAverage,
                Integer totalParkingCount,
                int foodMenuCount,
                int facilityCount) {
            return new ComparisonSummary(
                    gasolinePrice,
                    dieselPrice,
                    lpgPrice,
                    gasolinePriceDiffFromAverage,
                    dieselPriceDiffFromAverage,
                    lpgPriceDiffFromAverage,
                    totalParkingCount,
                    foodMenuCount,
                    facilityCount);
        }
    }

    public record NationalOilPriceSummary(
            String tradeDate, AverageOilPrice gasoline, AverageOilPrice diesel, AverageOilPrice lpg) {

        public static NationalOilPriceSummary of(
                String tradeDate, AverageOilPrice gasoline, AverageOilPrice diesel, AverageOilPrice lpg) {
            return new NationalOilPriceSummary(tradeDate, gasoline, diesel, lpg);
        }
    }

    public record AverageOilPrice(String productCode, String productName, String price, String dailyDiff) {

        public static AverageOilPrice of(String productCode, String productName, String price, String dailyDiff) {
            return new AverageOilPrice(productCode, productName, price, dailyDiff);
        }
    }

    public record RecommendationTag(String key, String label) {

        public static RecommendationTag of(String key, String label) {
            return new RecommendationTag(key, label);
        }
    }

    public record NearbyTraffic(String key, String label) {

        public static NearbyTraffic of(String key, String label) {
            return new NearbyTraffic(key, label);
        }
    }
}

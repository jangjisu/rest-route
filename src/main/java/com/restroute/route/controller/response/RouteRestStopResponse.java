package com.restroute.route.controller.response;

import com.restroute.reststop.domain.SizeTier;
import com.restroute.route.dto.FuelType;
import java.util.List;

public record RouteRestStopResponse(Destination destination, List<RouteOption> routes) {

    public static RouteRestStopResponse of(Destination destination, List<RouteOption> routes) {
        return new RouteRestStopResponse(destination, routes);
    }

    public record Destination(String name, double latitude, double longitude) {

        public static Destination of(String name, double latitude, double longitude) {
            return new Destination(name, latitude, longitude);
        }
    }

    public record RouteOption(int routeIndex, RouteSummary summary, List<RouteRestStopItem> restStops) {

        public static RouteOption of(int routeIndex, RouteSummary summary, List<RouteRestStopItem> restStops) {
            return new RouteOption(routeIndex, summary, restStops);
        }
    }

    public record RouteSummary(long distanceMeters, long durationSeconds, long tollFareWon, List<List<Double>> path) {

        public static RouteSummary of(
                long distanceMeters, long durationSeconds, long tollFareWon, List<List<Double>> path) {
            return new RouteSummary(distanceMeters, durationSeconds, tollFareWon, path);
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
            NearbyTraffic nearbyTraffic,
            Integer maleToiletCount,
            Integer femaleToiletCount,
            boolean topTrafficTier,
            SizeTier sizeTier,
            FuelPriceTier fuelPriceTier) {

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
                    null,
                    null,
                    null,
                    false,
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
                    serviceAreaCode, unitName, routeName, latitude, longitude, distanceFromRouteMeters);
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
                    nearbyTraffic,
                    maleToiletCount,
                    femaleToiletCount,
                    topTrafficTier,
                    sizeTier,
                    fuelPriceTier);
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
                    nearbyTraffic,
                    maleToiletCount,
                    femaleToiletCount,
                    topTrafficTier,
                    sizeTier,
                    fuelPriceTier);
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
                    nearbyTraffic,
                    maleToiletCount,
                    femaleToiletCount,
                    topTrafficTier,
                    sizeTier,
                    fuelPriceTier);
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
                    nearbyTraffic,
                    maleToiletCount,
                    femaleToiletCount,
                    topTrafficTier,
                    sizeTier,
                    fuelPriceTier);
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
                    nearbyTraffic,
                    maleToiletCount,
                    femaleToiletCount,
                    topTrafficTier,
                    sizeTier,
                    fuelPriceTier);
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
                    nearbyTraffic,
                    maleToiletCount,
                    femaleToiletCount,
                    topTrafficTier,
                    sizeTier,
                    fuelPriceTier);
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
                    nearbyTraffic,
                    maleToiletCount,
                    femaleToiletCount,
                    topTrafficTier,
                    sizeTier,
                    fuelPriceTier);
        }

        /**
         * 화장실 남/여 변기 수 — {@code RestStopAggregate}에서 조회해 채운다. 매핑된 화장실 데이터가
         * 없으면 둘 다 null.
         */
        public RouteRestStopItem withRestroomCounts(Integer maleToiletCount, Integer femaleToiletCount) {
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
                    nearbyTraffic,
                    maleToiletCount,
                    femaleToiletCount,
                    topTrafficTier,
                    sizeTier,
                    fuelPriceTier);
        }

        public RouteRestStopItem withTopTrafficTier(boolean topTrafficTier) {
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
                    nearbyTraffic,
                    maleToiletCount,
                    femaleToiletCount,
                    topTrafficTier,
                    sizeTier,
                    fuelPriceTier);
        }

        public RouteRestStopItem withSizeTier(SizeTier sizeTier) {
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
                    nearbyTraffic,
                    maleToiletCount,
                    femaleToiletCount,
                    topTrafficTier,
                    sizeTier,
                    fuelPriceTier);
        }

        public RouteRestStopItem withFuelPriceTier(FuelPriceTier fuelPriceTier) {
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
                    nearbyTraffic,
                    maleToiletCount,
                    femaleToiletCount,
                    topTrafficTier,
                    sizeTier,
                    fuelPriceTier);
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

        public String getPriceByFuelType(FuelType fuelType) {
            return switch (fuelType) {
                case GASOLINE -> gasolinePrice;
                case DIESEL -> dieselPrice;
                case LPG -> lpgPrice;
                case EV -> null;
            };
        }
    }

    public record NationalOilPriceSummary(
            String tradeDate, AverageOilPrice gasoline, AverageOilPrice diesel, AverageOilPrice lpg) {

        public static NationalOilPriceSummary of(
                String tradeDate, AverageOilPrice gasoline, AverageOilPrice diesel, AverageOilPrice lpg) {
            return new NationalOilPriceSummary(tradeDate, gasoline, diesel, lpg);
        }

        public String getAveragePriceByFuelType(FuelType fuelType) {
            return switch (fuelType) {
                case GASOLINE -> gasoline.price();
                case DIESEL -> diesel.price();
                case LPG -> lpg.price();
                case EV -> null;
            };
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

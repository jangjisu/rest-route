package com.restroute.service.route;

import com.restroute.client.KakaoMapClient;
import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.client.response.KakaoLocalSearchResponse;
import com.restroute.controller.response.RouteRestStopResponse;
import com.restroute.controller.response.RouteRestStopResponse.Destination;
import com.restroute.controller.response.RouteRestStopResponse.NationalOilPriceSummary;
import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.controller.response.RouteRestStopResponse.RouteSummary;
import com.restroute.domain.RestStopEntity;
import com.restroute.service.NationalOilPriceService;
import com.restroute.service.RestStopEventQueryService;
import com.restroute.service.RestStopQueryService;
import com.restroute.service.RestStopRelatedInfo;
import com.restroute.service.RestStopRelatedInfoQueryService;
import com.restroute.service.RestThemeQueryService;
import com.restroute.service.evcharger.EvChargerQueryService;
import com.restroute.service.image.RestStopImageQueryService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteRestStopService {

    private static final int MAX_POLYLINE_POINTS = 300;
    private static final String LIST_IMAGE_URL_FORMAT = "/api/rest-stops/%s/images/list";

    private final KakaoMapClient kakaoMapClient;
    private final RestStopQueryService restStopQueryService;
    private final RouteRestStopComparisonSummaryService routeRestStopComparisonSummaryService;
    private final RouteRestStopRecommendationTagService routeRestStopRecommendationTagService;
    private final NationalOilPriceService nationalOilPriceService;
    private final EvChargerQueryService evChargerQueryService;
    private final RestStopImageQueryService restStopImageQueryService;
    private final RestThemeQueryService restThemeQueryService;
    private final RestStopEventQueryService restStopEventQueryService;
    private final RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;

    public RouteRestStopResponse findRouteRestStops(
            double originLatitude,
            double originLongitude,
            String destinationQuery,
            Double destinationLatitude,
            Double destinationLongitude,
            String destinationName,
            int radiusMeters) {
        Destination destination =
                resolveDestination(destinationQuery, destinationLatitude, destinationLongitude, destinationName);

        KakaoDirectionsResponse directions = kakaoMapClient.getDirections(
                coordinateParam(originLongitude, originLatitude),
                coordinateParam(destination.longitude(), destination.latitude()));
        if (directions.failedToRoute()) {
            KakaoDirectionsResponse.Route failedRoute = directions.firstRoute();
            throw new RouteRestStopNotFoundException(
                    routeFailureMessage(failedRoute == null ? null : failedRoute.resultCode()));
        }

        KakaoDirectionsResponse.Route route = directions.firstRoute();
        RoutePolyline polyline = RoutePolyline.fromRoute(route).downsample(MAX_POLYLINE_POINTS);
        if (polyline.isEmpty()) {
            throw new RouteRestStopNotFoundException("경로 좌표가 없습니다.");
        }

        Optional<NationalOilPriceSummary> nationalOilPriceSummary = nationalOilPriceService.getTodaySummary();
        List<RouteRestStopItem> restStops = restStopsOnRoute(polyline, radiusMeters, nationalOilPriceSummary);
        return RouteRestStopResponse.of(destination, routeSummary(route, polyline), restStops);
    }

    private String routeFailureMessage(Integer resultCode) {
        int code = resultCode == null ? -1 : resultCode;
        return switch (code) {
            case 101, 105 -> "출발지 주변에서 도로를 찾지 못했어요. 출발지를 도로에 가까운 위치로 바꿔주세요.";
            case 102, 106 -> "도착지 주변에서 도로를 찾지 못했어요. 도착지를 도로에 가까운 위치로 바꿔주세요.";
            case 104 -> "출발지와 도착지가 너무 가까워요. 좀 더 떨어진 위치를 선택해주세요.";
            default -> "경로를 찾지 못했어요. 출발지와 도착지를 다시 확인해주세요.";
        };
    }

    private Destination resolveDestination(
            String destinationQuery, Double destinationLatitude, Double destinationLongitude, String destinationName) {
        if (destinationLatitude != null && destinationLongitude != null) {
            String name = destinationName == null || destinationName.isBlank() ? "목적지" : destinationName;
            return Destination.of(name, destinationLatitude, destinationLongitude);
        }

        KakaoLocalSearchResponse search = kakaoMapClient.searchKeyword(destinationQuery);
        if (search.isEmpty()) {
            throw new RouteRestStopNotFoundException("목적지 검색 결과가 없습니다: " + destinationQuery);
        }

        KakaoLocalSearchResponse.Document document = search.first();
        Double longitude = parseCoordinate(document.x());
        Double latitude = parseCoordinate(document.y());
        if (longitude == null || latitude == null) {
            throw new RouteRestStopNotFoundException("목적지 좌표를 해석하지 못했습니다.");
        }

        return Destination.of(document.label(), latitude, longitude);
    }

    private List<RouteRestStopItem> restStopsOnRoute(
            RoutePolyline polyline, int radiusMeters, Optional<NationalOilPriceSummary> nationalOilPriceSummary) {
        List<RouteRestStopCandidate> candidates = new ArrayList<>();
        for (RestStopEntity restStop : restStopQueryService.findAll()) {
            Double latitude = parseCoordinate(restStop.getYValue());
            Double longitude = parseCoordinate(restStop.getXValue());
            if (latitude == null || longitude == null) {
                continue;
            }

            RoutePolyline.Nearest nearest = polyline.nearest(latitude, longitude);
            if (nearest.distanceMeters() > radiusMeters) {
                continue;
            }

            RouteRestStopItem item = RouteRestStopItem.of(
                            restStop.getServiceAreaCode(),
                            restStop.getUnitName(),
                            restStop.getRouteName(),
                            latitude,
                            longitude,
                            Math.round(nearest.distanceMeters()))
                    .withNearbyTraffic(nearbyTraffic(
                            polyline.coordinates().get(nearest.index()).trafficState()));
            candidates.add(RouteRestStopCandidate.of(restStop, item, nearest.index()));
        }

        Map<String, Long> groupCounts = candidates.stream()
                .filter(RouteRestStopCandidate::hasDirectionGroup)
                .map(RouteRestStopCandidate::groupKey)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<String> mappedServiceAreaCodes =
                evChargerQueryService.findChargerMappedServiceAreaCodes(candidates.stream()
                        .map(candidate -> candidate.restStop().getServiceAreaCode())
                        .toList());
        Set<String> imageServiceAreaCodes = restStopImageQueryService.findExistingServiceAreaCodes(candidates.stream()
                .map(candidate -> candidate.restStop().getServiceAreaCode())
                .toList());
        List<String> candidateServiceAreaCodes = candidates.stream()
                .map(candidate -> candidate.restStop().getServiceAreaCode())
                .toList();
        List<String> themeServiceAreaCodes =
                restThemeQueryService.findThemeMappedServiceAreaCodes(candidateServiceAreaCodes);
        List<String> activeEventServiceAreaCodes =
                restStopEventQueryService.findActiveEventMappedServiceAreaCodes(candidateServiceAreaCodes);
        Map<String, RestStopRelatedInfo> relatedInfoByServiceAreaCode =
                restStopRelatedInfoQueryService.findAllByRestStops(candidates.stream()
                        .map(RouteRestStopCandidate::restStop)
                        .toList());
        List<RouteRestStopComparison> comparisons = candidates.stream()
                .map(candidate -> RouteRestStopComparison.of(
                        candidate,
                        routeRestStopComparisonSummaryService.create(
                                relatedInfoByServiceAreaCode.get(
                                        candidate.restStop().getServiceAreaCode()),
                                nationalOilPriceSummary)))
                .toList();
        RouteRestStopRecommendationStandards recommendationStandards =
                routeRestStopRecommendationTagService.standards(comparisons);
        return comparisons.stream()
                .sorted(Comparator.comparingInt(
                        comparison -> comparison.candidate().routeIndex()))
                .map(comparison -> comparison
                        .candidate()
                        .item()
                        .withListImageUrl(listImageUrl(
                                comparison.candidate().restStop().getServiceAreaCode(), imageServiceAreaCodes))
                        .withEvCharger(mappedServiceAreaCodes.contains(
                                comparison.candidate().restStop().getServiceAreaCode()))
                        .withTheme(themeServiceAreaCodes.contains(
                                comparison.candidate().restStop().getServiceAreaCode()))
                        .withEvent(activeEventServiceAreaCodes.contains(
                                comparison.candidate().restStop().getServiceAreaCode()))
                        .withDirectionAlternative(
                                groupCounts.getOrDefault(comparison.candidate().groupKey(), 0L) > 1)
                        .withComparison(
                                comparison.summary(),
                                routeRestStopRecommendationTagService.create(comparison, recommendationStandards)))
                .toList();
    }

    private RouteRestStopResponse.NearbyTraffic nearbyTraffic(Integer trafficState) {
        return NearbyTrafficStatus.from(trafficState)
                .map(status -> RouteRestStopResponse.NearbyTraffic.of(status.key(), status.label()))
                .orElse(null);
    }

    private String listImageUrl(String serviceAreaCode, Set<String> imageServiceAreaCodes) {
        if (!imageServiceAreaCodes.contains(serviceAreaCode)) {
            return null;
        }
        return LIST_IMAGE_URL_FORMAT.formatted(serviceAreaCode);
    }

    private RouteSummary routeSummary(KakaoDirectionsResponse.Route route, RoutePolyline polyline) {
        long distance = summaryValue(route, true);
        long duration = summaryValue(route, false);
        List<List<Double>> path = polyline.coordinates().stream()
                .map(coordinate -> List.of(coordinate.longitude(), coordinate.latitude()))
                .toList();
        return RouteSummary.of(distance, duration, path);
    }

    private long summaryValue(KakaoDirectionsResponse.Route route, boolean distance) {
        KakaoDirectionsResponse.Summary summary = route.summary();
        if (summary == null) {
            return 0L;
        }
        Long value = distance ? summary.distance() : summary.duration();
        return value == null ? 0L : value;
    }

    private String coordinateParam(double longitude, double latitude) {
        return longitude + "," + latitude;
    }

    private Double parseCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

package com.restroute.service.route.dto;

import com.restroute.client.response.KakaoDirectionsResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 카카오 길찾기 경로 좌표열과 근접·방향 계산을 담당하는 순수 로직(일급 컬렉션).
 * 생성 시점(from())에 총 거리 기준으로 정점 수를 이미 줄여서 들고 있는다.
 */
public final class RoutePath {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final int MINIMUM_POINTS = 300;
    private static final int TARGET_SPACING_METERS = 200;

    /**
     * sideOfTravel 계산에서 진행방향 벡터를 구할 때 최근접 지점 기준 앞뒤로 보는 정점 "개수"(거리
     * 아님). 5는 실측 검증(안성(서울)/안성(부산) 사례)에서 잘 맞았던 경험적인 값이다.
     */
    private static final int DIRECTION_VECTOR_SPAN_POINTS = 5;

    private final List<PathPoint> points;
    private final List<Integer> trafficStates;

    private RoutePath(List<PathPoint> points, List<Integer> trafficStates) {
        this.points = points;
        this.trafficStates = trafficStates;
    }

    public static RoutePath from(KakaoDirectionsResponse.Route route) {
        List<PathPoint> rawPoints = new ArrayList<>();
        List<Integer> rawTrafficStates = new ArrayList<>();
        collect(route, rawPoints, rawTrafficStates);

        int targetCount = targetPointCount(totalDistanceMeters(route));
        return new RoutePath(downsample(rawPoints, targetCount), downsample(rawTrafficStates, targetCount));
    }

    private static int targetPointCount(long totalDistanceMeters) {
        int distanceBasedCount = (int) Math.ceil(totalDistanceMeters / (double) TARGET_SPACING_METERS);
        return Math.max(MINIMUM_POINTS, distanceBasedCount);
    }

    private static long totalDistanceMeters(KakaoDirectionsResponse.Route route) {
        if (route == null || route.summary() == null || route.summary().distance() == null) {
            return 0L;
        }
        return route.summary().distance();
    }

    private static void collect(
            KakaoDirectionsResponse.Route route, List<PathPoint> points, List<Integer> trafficStates) {
        if (route == null || route.sections() == null) {
            return;
        }
        for (KakaoDirectionsResponse.Section section : route.sections()) {
            if (section == null || section.roads() == null) {
                continue;
            }
            for (KakaoDirectionsResponse.Road road : section.roads()) {
                collectRoad(road, points, trafficStates);
            }
        }
    }

    private static void collectRoad(
            KakaoDirectionsResponse.Road road, List<PathPoint> points, List<Integer> trafficStates) {
        if (road == null || road.vertexes() == null) {
            return;
        }
        List<Double> vertexes = road.vertexes();
        Integer trafficState = road.trafficState();
        for (int i = 0; i + 1 < vertexes.size(); i += 2) {
            Double longitude = vertexes.get(i);
            Double latitude = vertexes.get(i + 1);
            if (longitude == null || latitude == null) {
                continue;
            }
            points.add(new PathPoint(longitude, latitude));
            trafficStates.add(trafficState);
        }
    }

    private static <T> List<T> downsample(List<T> source, int maxPoints) {
        if (source.size() <= maxPoints) {
            return source;
        }
        List<T> sampled = new ArrayList<>();
        double step = (double) (source.size() - 1) / (maxPoints - 1);
        for (int i = 0; i < maxPoints; i++) {
            int index = (int) Math.round(i * step);
            sampled.add(source.get(Math.min(index, source.size() - 1)));
        }
        return sampled;
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public List<PathPoint> points() {
        return List.copyOf(points);
    }

    public List<List<Double>> path() {
        return points.stream()
                .map(point -> List.of(point.longitude(), point.latitude()))
                .toList();
    }

    public Integer trafficStateAt(int index) {
        return trafficStates.get(index);
    }

    /**
     * 주어진 좌표에서 경로까지의 최단거리(m)와 가장 가까운 정점 인덱스.
     */
    public Nearest nearestTo(double latitude, double longitude) {
        double minDistance = Double.MAX_VALUE;
        int nearestIndex = -1;
        for (int i = 0; i < points.size(); i++) {
            PathPoint point = points.get(i);
            double distance = haversineMeters(latitude, longitude, point.latitude(), point.longitude());
            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }
        return new Nearest(minDistance, nearestIndex);
    }

    /**
     * nearestIndex 지점에서 진행방향 기준으로 (latitude, longitude)가 왼쪽/오른쪽 어느 쪽에
     * 있는지 판별한다. 우리나라는 우측통행이므로 RIGHT만 진행방향에서 실제로 진입 가능한 쪽이다.
     *
     * UNKNOWN은 진행방향 벡터 자체를 구할 수 없을 때만 반환한다 — 폴리라인 정점이 1개뿐이거나,
     * 최근접 지점 앞뒤로 잡은 두 정점의 좌표가 완전히 같은 경우(연속된 중복 정점)뿐이다. 대상
     * 좌표가 진행방향 직선에 거의 걸쳐 있는(외적이 0에 가까운) 경계 케이스는 별도로 UNKNOWN
     * 처리하지 않고 LEFT/RIGHT 중 하나로 결정된다 — 얼마나 가까워야 "거의 걸친 것"으로 볼지
     * 판단할 근거(GPS 오차 범위 등)가 아직 없어서다.
     */
    public Side sideOfTravel(int nearestIndex, double latitude, double longitude) {
        return directionAt(nearestIndex)
                .map(direction -> classifySide(direction, points.get(nearestIndex), latitude, longitude))
                .orElse(Side.UNKNOWN);
    }

    private Optional<Direction> directionAt(int nearestIndex) {
        int i0 = Math.max(0, nearestIndex - DIRECTION_VECTOR_SPAN_POINTS);
        int i1 = Math.min(points.size() - 1, nearestIndex + DIRECTION_VECTOR_SPAN_POINTS);
        PathPoint from = points.get(i0);
        PathPoint to = points.get(i1);

        double deltaLongitude = to.longitude() - from.longitude();
        double deltaLatitude = to.latitude() - from.latitude();
        if (deltaLongitude == 0.0 && deltaLatitude == 0.0) {
            return Optional.empty();
        }
        return Optional.of(new Direction(deltaLongitude, deltaLatitude));
    }

    private Side classifySide(Direction direction, PathPoint near, double latitude, double longitude) {
        double towardLng = longitude - near.longitude();
        double towardLat = latitude - near.latitude();
        double cross = direction.deltaLongitude() * towardLat - direction.deltaLatitude() * towardLng;
        return cross < 0 ? Side.RIGHT : Side.LEFT;
    }

    private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2)
                        * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    public record Nearest(double distanceMeters, int index) {}

    public enum Side {
        LEFT,
        RIGHT,
        UNKNOWN
    }
}

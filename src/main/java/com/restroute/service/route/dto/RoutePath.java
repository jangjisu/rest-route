package com.restroute.service.route.dto;

import java.util.List;
import java.util.Optional;

/**
 * 이미 축소된 경로 좌표열에 대한 근접·방향 계산을 담당하는 순수 로직(일급 컬렉션).
 * 좌표 축소(다운샘플링) 자체는 이 클래스의 책임이 아니다 — RouteCoordinateReductionService가
 * 정점 수를 줄인 뒤 of()로 넘겨준다.
 */
public final class RoutePath {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

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

    public static RoutePath of(List<PathPoint> points, List<Integer> trafficStates) {
        return new RoutePath(points, trafficStates);
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

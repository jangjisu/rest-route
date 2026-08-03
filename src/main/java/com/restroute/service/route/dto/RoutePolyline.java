package com.restroute.service.route.dto;

import com.restroute.client.response.KakaoDirectionsResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 카카오 길찾기 경로 좌표열(폴리라인)과 근접 계산을 담당하는 순수 로직.
 * vertexes 는 [경도, 위도, 경도, 위도, ...] 평탄 배열이다.
 */
public final class RoutePolyline {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final List<Coordinate> points;

    private RoutePolyline(List<Coordinate> points) {
        this.points = points;
    }

    public static RoutePolyline fromRoute(KakaoDirectionsResponse.Route route) {
        List<Coordinate> points = new ArrayList<>();
        if (route == null || route.sections() == null) {
            return new RoutePolyline(points);
        }

        for (KakaoDirectionsResponse.Section section : route.sections()) {
            if (section == null || section.roads() == null) {
                continue;
            }
            for (KakaoDirectionsResponse.Road road : section.roads()) {
                addRoadVertexes(points, road);
            }
        }
        return new RoutePolyline(points);
    }

    private static void addRoadVertexes(List<Coordinate> points, KakaoDirectionsResponse.Road road) {
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
            points.add(new Coordinate(longitude, latitude, trafficState));
        }
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public int size() {
        return points.size();
    }

    public List<Coordinate> coordinates() {
        return List.copyOf(points);
    }

    /**
     * 정점 수를 maxPoints 이하로 균등 축약한다(시작/끝 보존). 성능용.
     */
    public RoutePolyline downsample(int maxPoints) {
        if (maxPoints < 2 || points.size() <= maxPoints) {
            return this;
        }

        List<Coordinate> sampled = new ArrayList<>();
        double step = (double) (points.size() - 1) / (maxPoints - 1);
        for (int i = 0; i < maxPoints; i++) {
            int index = (int) Math.round(i * step);
            sampled.add(points.get(Math.min(index, points.size() - 1)));
        }
        return new RoutePolyline(sampled);
    }

    /**
     * 주어진 좌표에서 폴리라인까지의 최단거리(m)와 가장 가까운 정점 인덱스.
     */
    public Nearest nearest(double latitude, double longitude) {
        double minDistance = Double.MAX_VALUE;
        int nearestIndex = -1;
        for (int i = 0; i < points.size(); i++) {
            Coordinate point = points.get(i);
            double distance = haversineMeters(latitude, longitude, point.latitude(), point.longitude());
            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }
        return new Nearest(minDistance, nearestIndex);
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

    public record Coordinate(double longitude, double latitude, Integer trafficState) {}

    public record Nearest(double distanceMeters, int index) {}
}

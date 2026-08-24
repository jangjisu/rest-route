package com.restroute.service.route;

import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.service.route.dto.PathPoint;
import com.restroute.service.route.dto.ResolvedRoute.RouteGeometry;
import com.restroute.service.route.dto.RoutePath;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 카카오 원본 폴리라인은 총 거리에 비해 정점이 너무 많다 — 근접거리 계산(RoutePath.nearestTo)이
 * 매 휴게소마다 전체 정점을 순회하므로, 성능을 위해 여기서 정점 수를 줄인다.
 * 200m 간격 기준으로 목표 개수를 정하고(최소 300개), 원본에서 그 개수만큼 균등한 간격으로
 * 정점을 뽑아낸다(uniform sampling) — 압축이나 단순 절단이 아니다.
 * 의존성 없는 순수 알고리즘이라 서비스가 아닌 컴포넌트로 분류한다.
 */
@Component
public class RouteCoordinateReducer {

    private static final int MINIMUM_POINTS = 300;
    private static final int TARGET_SPACING_METERS = 200;

    public RouteGeometry reduce(KakaoDirectionsResponse.Route route) {
        List<PathPoint> rawPoints = new ArrayList<>();
        List<Integer> rawTrafficStates = new ArrayList<>();
        collect(route.sections(), rawPoints, rawTrafficStates);

        int targetCount = targetPointCount(totalDistanceMeters(route.summary()));
        RoutePath path = RoutePath.of(downsample(rawPoints, targetCount), downsample(rawTrafficStates, targetCount));
        return RouteGeometry.of(path, route.summary());
    }

    private int targetPointCount(long totalDistanceMeters) {
        int distanceBasedCount = (int) Math.ceil(totalDistanceMeters / (double) TARGET_SPACING_METERS);
        return Math.max(MINIMUM_POINTS, distanceBasedCount);
    }

    private long totalDistanceMeters(KakaoDirectionsResponse.Summary summary) {
        if (summary == null || summary.distance() == null) {
            return 0L;
        }
        return summary.distance();
    }

    private void collect(
            List<KakaoDirectionsResponse.Section> sections, List<PathPoint> points, List<Integer> trafficStates) {
        if (sections == null) {
            return;
        }
        for (KakaoDirectionsResponse.Section section : sections) {
            if (section == null || section.roads() == null) {
                continue;
            }
            for (KakaoDirectionsResponse.Road road : section.roads()) {
                collectRoad(road, points, trafficStates);
            }
        }
    }

    private void collectRoad(KakaoDirectionsResponse.Road road, List<PathPoint> points, List<Integer> trafficStates) {
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

    private <T> List<T> downsample(List<T> source, int maxPoints) {
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
}

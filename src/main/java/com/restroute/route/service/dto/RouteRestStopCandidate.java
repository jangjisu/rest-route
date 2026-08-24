package com.restroute.route.service.dto;

import java.util.List;

/**
 * 경로상 매칭 지점(routeIndex) 하나와, 거기 매칭된 휴게소 목록. 보통 1개지만, 서로 다른 휴게소가
 * 같은 지점에 매칭되면(드묾) 2개 이상일 수 있다.
 */
public record RouteRestStopCandidate(int routeIndex, List<MatchedRestStop> restStops) {

    public static RouteRestStopCandidate of(int routeIndex, List<MatchedRestStop> restStops) {
        return new RouteRestStopCandidate(routeIndex, restStops);
    }
}

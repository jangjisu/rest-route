package com.restroute.route.service.dto;

/**
 * groupKey로 그룹핑/방향판별할 때 필요한, 매칭된 휴게소와 그 경로상 인덱스의 조합.
 * RouteRestStopCandidate.restStops()를 펼칠 때 인덱스 정보를 잃지 않기 위해 쓴다.
 */
public record IndexedMatch(int routeIndex, MatchedRestStop matchedRestStop) {

    public static IndexedMatch of(int routeIndex, MatchedRestStop matchedRestStop) {
        return new IndexedMatch(routeIndex, matchedRestStop);
    }
}

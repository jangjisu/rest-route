package com.restroute.route.service.dto;

import com.restroute.reststop.domain.RestStopEntity;
import java.util.List;

/**
 * 경로별 후보 휴게소와, 그 후보를 찾는 데 쓴 휴게소 전체.
 *
 * <p>휴게소 전체를 함께 담는 건 후보에 담긴 {@code RouteRestStopItem}이 엔티티가 아니어서,
 * 뒤이어 집계를 조회하는 쪽이 코드로 엔티티를 다시 찾아야 하기 때문이다. 호출부가 같은 조회를
 * 반복하지 않도록 여기 함께 돌려준다.
 */
public record RouteCandidates(List<RouteCandidate> candidates, List<RestStopEntity> allRestStops) {

    /** 대안 경로를 쓰지 않는 호출부가 첫 경로만 고를 때 쓴다. */
    public RouteCandidate first() {
        return candidates.get(0);
    }
}

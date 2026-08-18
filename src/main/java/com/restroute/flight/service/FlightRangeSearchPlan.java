package com.restroute.flight.service;

import java.util.List;

/**
 * {@link FlightRangeSearchPlanner}가 정한 실제 Travelpayouts 조회 계획.
 *
 * <p>{@code destinations}가 비어있으면 destination 파라미터 자체를 생략하고 개월당 딱 한 번만
 * 부른다(그 경우 이 축은 조합에서 1로 친다) — 그 외에는 destinations × months × nightsWindows
 * 조합 하나당 호출을 한 번씩 한다.
 */
record FlightRangeSearchPlan(List<String> destinations, List<String> months, List<NightsWindow> nightsWindows) {

    record NightsWindow(int min, int max) {}
}

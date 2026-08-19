package com.restroute.flight.service;

import java.util.List;

/**
 * {@link FlightRangeSearchPlanner}가 정한 실제 Travelpayouts 조회 계획.
 *
 * <p>{@code destinations}가 비어있으면 destination 파라미터 자체를 생략하고 개월당 딱 한 번만
 * 부른다(그 경우 이 축은 조합에서 1로 친다). {@code destinations}에 {@code null}이 원소로
 * 들어있으면(sector로 여러 국가를 골랐을 때 예산이 허락해 "전체" 조회를 함께 얹은 경우) 그
 * 항목 하나가 destination 생략 호출이 된다 — 그 외 값들은 destinations × months ×
 * nightsWindows 조합 하나당 호출을 한 번씩 한다.
 */
record FlightRangeSearchPlan(List<String> destinations, List<String> months, List<NightsWindow> nightsWindows) {

    record NightsWindow(int min, int max) {}
}

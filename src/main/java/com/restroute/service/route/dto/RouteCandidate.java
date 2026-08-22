package com.restroute.service.route.dto;

import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.service.route.dto.ResolvedRoute.RouteGeometry;
import java.util.List;

/**
 * 대안 경로 하나의 좌표열(geometry)과, 그 경로에 방향 필터링까지 끝나고 매칭된 휴게소 목록.
 */
public record RouteCandidate(int routeIndex, RouteGeometry geometry, List<RouteRestStopItem> items) {}

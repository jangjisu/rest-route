package com.restroute.service.route.dto;

import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.domain.RestStopEntity;

/**
 * 경로상 한 지점(RouteRestStopCandidate)에 매칭된 휴게소 하나.
 * groupKey/hasDirectionGroup은 이름의 "(방향지명)" 패턴으로 상행/하행 페어를 판별하기 위한 값이다.
 */
public record MatchedRestStop(
        RestStopEntity restStop, String groupKey, boolean hasDirectionGroup, RouteRestStopItem item) {

    public static MatchedRestStop of(RestStopEntity restStop, RouteRestStopItem item) {
        String directionLabel = directionLabel(restStop.getUnitName());
        String groupKey = groupKey(restStop, directionLabel);
        return new MatchedRestStop(restStop, groupKey, directionLabel != null, item);
    }

    private static String groupKey(RestStopEntity restStop, String directionLabel) {
        if (directionLabel == null) {
            return restStop.getRouteName() + "|" + restStop.getUnitName() + "|" + restStop.getServiceAreaCode();
        }
        return restStop.getRouteName() + "|" + restStopBaseName(restStop.getUnitName());
    }

    private static String restStopBaseName(String unitName) {
        String name = unitName.substring(0, unitName.indexOf('('));
        return name.replace("휴게소", "").replaceAll("\\s+", "");
    }

    private static String directionLabel(String unitName) {
        if (unitName == null) {
            return null;
        }
        int start = unitName.indexOf('(');
        int end = unitName.indexOf(')', start + 1);
        if (start < 0 || end <= start + 1) {
            return null;
        }
        return unitName.substring(start + 1, end).replaceAll("\\s+", "");
    }
}

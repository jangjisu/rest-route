package com.restroute.service.route;

import com.restroute.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.domain.RestStopEntity;

record RouteRestStopCandidate(
        RestStopEntity restStop, String groupKey, boolean hasDirectionGroup, int routeIndex, RouteRestStopItem item) {

    static RouteRestStopCandidate of(RestStopEntity restStop, RouteRestStopItem item, int routeIndex) {
        String directionLabel = directionLabel(restStop.getUnitName());
        String groupKey = groupKey(restStop, directionLabel);
        return new RouteRestStopCandidate(restStop, groupKey, directionLabel != null, routeIndex, item);
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

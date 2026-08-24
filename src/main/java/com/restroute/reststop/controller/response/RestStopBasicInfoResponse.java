package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststopcontent.domain.RestThemeEntity;
import java.util.List;
import java.util.Optional;

public record RestStopBasicInfoResponse(
        String serviceAreaCode,
        String unitCode,
        String unitName,
        String routeNo,
        String routeName,
        String xValue,
        String yValue,
        String stdRestCd,
        String address,
        String telNo,
        String brand,
        int evChargerCount,
        String detailImageUrl,
        List<ThemeInfo> themes) {

    public static RestStopBasicInfoResponse of(
            RestStopEntity restStop,
            Optional<RestStopDetailEntity> detail,
            int evChargerCount,
            String detailImageUrl,
            List<RestThemeEntity> themes) {
        return new RestStopBasicInfoResponse(
                restStop.getServiceAreaCode(),
                restStop.getUnitCode(),
                restStop.getUnitName(),
                restStop.getRouteNo(),
                restStop.getRouteName(),
                restStop.getXValue(),
                restStop.getYValue(),
                restStop.getStdRestCd(),
                ResponseTextUtils.textOf(detail, RestStopDetailEntity::getSvarAddr),
                ResponseTextUtils.textOf(detail, RestStopDetailEntity::getTelNo),
                ResponseTextUtils.textOf(detail, RestStopDetailEntity::getBrand),
                evChargerCount,
                detailImageUrl,
                themes.stream().map(ThemeInfo::from).toList());
    }

    public record ThemeInfo(String name, String detail) {

        public static ThemeInfo from(RestThemeEntity theme) {
            return new ThemeInfo(theme.getItemNm(), theme.getDetail());
        }
    }
}

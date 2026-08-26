package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import com.restroute.reststop.domain.RestStopDetailEntity;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public record RestStopFacilityResponse(
        List<String> convenienceFacilities,
        Boolean hasMaintenance,
        Boolean allowsTruckParking,
        String direction,
        Integer compactCarParkingCount,
        Integer fullSizeCarParkingCount,
        Integer disabledParkingCount) {

    public static RestStopFacilityResponse of(
            Optional<RestStopDetailEntity> detail, List<HighwayServiceAreaInfoEntity> infos) {
        return new RestStopFacilityResponse(
                convenienceFacilities(detail),
                toBoolean(ResponseTextUtils.textOf(detail, RestStopDetailEntity::getMaintenanceYn)),
                toBoolean(ResponseTextUtils.textOf(detail, RestStopDetailEntity::getTruckSaYn)),
                minText(infos, HighwayServiceAreaInfoEntity::getDirectionTypeName),
                sumIntegerValues(infos, HighwayServiceAreaInfoEntity::getCompactCarParkingCount),
                sumIntegerValues(infos, HighwayServiceAreaInfoEntity::getFullSizeCarParkingCount),
                sumIntegerValues(infos, HighwayServiceAreaInfoEntity::getDisabledParkingCount));
    }

    private static List<String> convenienceFacilities(Optional<RestStopDetailEntity> detail) {
        String raw = ResponseTextUtils.textOf(detail, RestStopDetailEntity::getConvenience);
        if (raw == null) {
            return List.of();
        }

        return Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(ResponseTextUtils::hasText)
                .distinct()
                .toList();
    }

    private static final String YES_VALUE = "O";
    private static final String NO_VALUE = "X";

    private static Boolean toBoolean(String value) {
        if (YES_VALUE.equals(value)) {
            return true;
        }
        if (NO_VALUE.equals(value)) {
            return false;
        }
        return null;
    }

    private static <T> String minText(List<T> items, Function<T, String> getter) {
        return items.stream()
                .map(getter)
                .filter(ResponseTextUtils::hasText)
                .map(String::trim)
                .min(String::compareTo)
                .orElse(null);
    }

    private static Integer sumIntegerValues(
            List<HighwayServiceAreaInfoEntity> infos, Function<HighwayServiceAreaInfoEntity, String> getter) {
        List<Integer> values = infos.stream()
                .map(getter)
                .map(RestStopFacilityResponse::parseInteger)
                .filter(Objects::nonNull)
                .toList();

        if (values.isEmpty()) {
            return null;
        }

        return values.stream().mapToInt(Integer::intValue).sum();
    }

    private static Integer parseInteger(String value) {
        if (!ResponseTextUtils.hasText(value)) {
            return null;
        }

        return Integer.valueOf(value.trim());
    }
}

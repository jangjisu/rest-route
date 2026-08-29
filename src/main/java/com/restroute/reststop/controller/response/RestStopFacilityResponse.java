package com.restroute.reststop.controller.response;

import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.domain.RestStopRestroomEntity;
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
        Integer disabledParkingCount,
        Integer maleToiletCount,
        Integer femaleToiletCount) {

    public static RestStopFacilityResponse of(
            Optional<RestStopDetailEntity> detail,
            List<HighwayServiceAreaInfoEntity> infos,
            Optional<RestStopRestroomEntity> restroom) {
        Integer compactCount = sumIntegerValues(infos, HighwayServiceAreaInfoEntity::getCompactCarParkingCount);
        Integer fullSizeCount = sumIntegerValues(infos, HighwayServiceAreaInfoEntity::getFullSizeCarParkingCount);
        Integer disabledCount = sumIntegerValues(infos, HighwayServiceAreaInfoEntity::getDisabledParkingCount);
        return new RestStopFacilityResponse(
                convenienceFacilities(detail),
                toBoolean(ResponseTextUtils.textOf(detail, RestStopDetailEntity::getMaintenanceYn)),
                toBoolean(ResponseTextUtils.textOf(detail, RestStopDetailEntity::getTruckSaYn)),
                minText(infos, HighwayServiceAreaInfoEntity::getDirectionTypeName),
                compactCount,
                fullSizeCount,
                correctedDisabledCount(compactCount, fullSizeCount, disabledCount),
                restroom.map(RestStopRestroomEntity::getMaleToiletCount)
                        .map(RestStopFacilityResponse::parseInteger)
                        .orElse(null),
                restroom.map(RestStopRestroomEntity::getFemaleToiletCount)
                        .map(RestStopFacilityResponse::parseInteger)
                        .orElse(null));
    }

    // 도로공사 원천 데이터 일부가 장애인 주차대수 칸에 총 주차대수를 잘못 입력해 두는 경우가 있어(예: 소형133+대형67인데 장애인 204),
    // 장애인 수가 소형+대형 합계를 넘으면 그 차이만 실제 장애인 주차대수로 본다.
    private static Integer correctedDisabledCount(Integer compactCount, Integer fullSizeCount, Integer disabledCount) {
        if (disabledCount == null) {
            return null;
        }

        int otherTotal = orZero(compactCount) + orZero(fullSizeCount);
        return disabledCount > otherTotal ? disabledCount - otherTotal : disabledCount;
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
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

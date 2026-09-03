package com.restroute.route.service.dto;

import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum NearbyTrafficStatus {
    SMOOTH("smooth", "원활"),
    SLOW("slow", "서행"),
    JAM("jam", "정체"),
    ACCIDENT("accident", "사고");

    private final String key;
    private final String label;

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public static Optional<NearbyTrafficStatus> from(Integer trafficState) {
        if (trafficState == null) {
            return Optional.empty();
        }
        return switch (trafficState) {
            case 4 -> Optional.of(SMOOTH);
            case 3 -> Optional.of(SLOW);
            case 1, 2 -> Optional.of(JAM);
            case 6 -> Optional.of(ACCIDENT);
            default -> Optional.empty();
        };
    }
}

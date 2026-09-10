package com.restroute.route.service;

import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.domain.PopularDestination;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import org.springframework.stereotype.Component;

/**
 * 목적지를 정한다. 좌표가 함께 오면 그대로 쓰고, 이름만 오면 {@link PopularDestination}에서 찾는다.
 * 의존성 없는 순수 판정이라 서비스가 아닌 컴포넌트로 분류한다.
 */
@Component
public class DestinationResolver {

    private static final String UNNAMED_DESTINATION = "목적지";

    public Destination resolve(Double latitude, Double longitude, String destinationName) {
        if (hasCoordinates(latitude, longitude)) {
            return Destination.of(displayNameOrDefault(destinationName), latitude, longitude);
        }
        return PopularDestination.findByDisplayName(destinationName)
                .map(destination ->
                        Destination.of(destination.displayName(), destination.latitude(), destination.longitude()))
                .orElseThrow(() -> RouteRestStopNotFoundException.destinationNotFound(destinationName));
    }

    private boolean hasCoordinates(Double latitude, Double longitude) {
        return latitude != null && longitude != null;
    }

    private String displayNameOrDefault(String destinationName) {
        if (destinationName == null || destinationName.isBlank()) {
            return UNNAMED_DESTINATION;
        }
        return destinationName;
    }
}

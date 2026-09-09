package com.restroute.route.service;

import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.domain.PopularDestination;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import org.springframework.stereotype.Component;

/**
 * 외부 호출 없이 목적지를 정한다. 좌표가 함께 오면 그대로 쓰고, 이름만 오면
 * {@link PopularDestination}에서 찾는다 — 둘 중 어느 쪽도 지오코딩을 거치지 않는다.
 *
 * <p>지오코딩이 필요한 자유 검색어는 이 경로가 아니라 별도 엔드포인트
 * {@code /api/place-search}가 처리하고, 그 결과로 얻은 좌표가 여기 첫 번째 갈래로 들어온다.
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

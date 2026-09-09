package com.restroute.route.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * finder "목적지로 추천받기"의 목적지 칩이 가리키는 고정 목적지. 칩은 사용 로그 없이 손으로 고른
 * 소수의 역이라 값이 바뀌지 않으므로, 이름을 받을 때마다 외부 지오코딩으로 좌표를 알아내는 대신
 * 좌표를 여기 함께 둔다 — 그 덕에 경로 조회가 외부 API를 한 번(길찾기)만 부른다.
 *
 * <p>역 단위로만 두는 건 카카오 길찾기가 도로에 붙은 지점을 요구하기 때문이다. 도시명(예: "부산")은
 * 행정구역 중심점이 도로에서 떨어져 있을 수 있어 칩으로 쓰지 않는다.
 */
public enum PopularDestination {
    BUSAN_STATION("부산역", 35.11520, 129.04137),
    DAEJEON_STATION("대전역", 36.331331, 127.433019),
    GANGNEUNG_STATION("강릉역", 37.7637611, 128.8990861),
    GWANGJU_SONGJEONG_STATION("광주송정역", 35.1378444, 126.7902333);

    private final String displayName;
    private final double latitude;
    private final double longitude;

    PopularDestination(String displayName, double latitude, double longitude) {
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Optional<PopularDestination> findByDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(destination -> destination.displayName.equals(displayName))
                .findFirst();
    }

    public String displayName() {
        return displayName;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }
}

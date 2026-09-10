package com.restroute.support;

import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopRepository;

/**
 * 경로 인수 테스트가 쓰는 좌표와 휴게소 데이터. 좌표들끼리의 관계가 곧 테스트의 의미라
 * 흩어놓지 않고 여기 모은다 — {@link #ROUTE_VERTEXES}의 두 번째 정점과
 * {@link #ON_ROUTE_LONGITUDE}/{@link #ON_ROUTE_LATITUDE}가 같은 지점이어야 "반경 안"이
 * 성립하고, {@link #OFF_ROUTE_LONGITUDE}/{@link #OFF_ROUTE_LATITUDE}는 그 경로에서
 * 수십 km 떨어져 있어야 "반경 밖"이 성립한다.
 */
public final class RouteFixtures {

    public static final double ORIGIN_LONGITUDE = 126.9780;
    public static final double ORIGIN_LATITUDE = 37.5665;

    /** 경로 위 — 아래 경로의 두 번째 정점과 같은 지점이라 거리 0m다. */
    public static final String ON_ROUTE_LONGITUDE = "126.9880";

    public static final String ON_ROUTE_LATITUDE = "37.5565";

    /** 경로 밖 — 어떤 정점에서도 수십 km 떨어져 반경 1km에 절대 걸리지 않는다. */
    public static final String OFF_ROUTE_LONGITUDE = "127.5000";

    public static final String OFF_ROUTE_LATITUDE = "37.9000";

    public static final String ON_ROUTE_NAME = "경로위휴게소";
    public static final String OFF_ROUTE_NAME = "경로밖휴게소";

    /** 서버가 좌표를 알고 있는 목적지(칩). 이름만 넘겨도 좌표가 나와야 한다. */
    public static final String KNOWN_DESTINATION_NAME = "부산역";

    /** 칩 목록에 없는 이름. 서버가 좌표를 알 방법이 없으므로 실패해야 한다. */
    public static final String UNKNOWN_DESTINATION_NAME = "존재하지않는목적지";

    public static final double DESTINATION_LONGITUDE = 129.0403;
    public static final double DESTINATION_LATITUDE = 35.1148;

    public static final long ROUTE_DISTANCE_METERS = 1_500L;

    /** 가짜 카카오가 돌려줄 경로. [경도, 위도] 쌍 3개짜리 짧은 직선이고 첫 점이 출발지다. */
    public static final double[] ROUTE_VERTEXES = {
        ORIGIN_LONGITUDE, ORIGIN_LATITUDE, 126.9880, 37.5565, 126.9980, 37.5465
    };

    private RouteFixtures() {}

    /**
     * 휴게소 이름은 시나리오마다 서로 다르게 준다 — 같은 이름의 상·하행 페어가 잡히면
     * {@code RouteRestStopMatcher}의 진행방향 판정까지 끌려들어와, 실패했을 때 원인이
     * 배선인지 방향 로직인지 갈라내기 어려워진다.
     */
    public static void saveRestStop(RestStopRepository repository, String unitName, String longitude, String latitude) {
        repository.save(RestStopEntity.createByAdmin(unitName, "1", "경부고속도로", longitude, latitude));
    }

    public static void saveOnRouteRestStop(RestStopRepository repository) {
        saveRestStop(repository, ON_ROUTE_NAME, ON_ROUTE_LONGITUDE, ON_ROUTE_LATITUDE);
    }

    public static void saveOffRouteRestStop(RestStopRepository repository) {
        saveRestStop(repository, OFF_ROUTE_NAME, OFF_ROUTE_LONGITUDE, OFF_ROUTE_LATITUDE);
    }
}

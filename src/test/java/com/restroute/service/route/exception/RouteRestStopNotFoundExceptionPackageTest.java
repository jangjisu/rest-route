package com.restroute.service.route.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RouteRestStopNotFoundExceptionPackageTest {

    @Test
    @DisplayName("경로 추천 예외는 route 서비스의 exception 서브패키지에 속한다")
    void routeRestStopNotFoundException_belongsToRouteExceptionPackage() {
        assertThat(RouteRestStopNotFoundException.class.getPackageName())
                .isEqualTo("com.restroute.service.route.exception");
    }
}

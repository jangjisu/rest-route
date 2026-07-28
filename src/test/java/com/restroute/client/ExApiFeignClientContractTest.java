package com.restroute.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

class ExApiFeignClientContractTest {

    @Test
    @DisplayName("휴게소 위치 API 명세는 인터페이스 공통 상수를 사용한다")
    void getLocationInfoRest_usesInterfaceContractConstants() throws Exception {
        Method method = ExApiFeignClient.class.getMethod(
                "getLocationInfoRest", String.class, String.class, String.class, String.class);

        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly(ExApiFeignClient.LOCATION_INFO_REST_PATH);
        assertRequestParameterNames(method);
        assertThat(ExApiFeignClient.REST_STOP_NUM_OF_ROWS).isEqualTo("99");
    }

    @Test
    @DisplayName("휴게소 편의시설 API 명세는 인터페이스 공통 상수를 사용한다")
    void getConvenienceServiceArea_usesInterfaceContractConstants() throws Exception {
        Method method = ExApiFeignClient.class.getMethod(
                "getConvenienceServiceArea", String.class, String.class, String.class, String.class);

        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly(ExApiFeignClient.CONVENIENCE_SERVICE_AREA_PATH);
        assertRequestParameterNames(method);
    }

    @Test
    @DisplayName("고속도로 휴게소 정보 API 명세는 인터페이스 공통 상수를 사용한다")
    void getHighwayServiceAreaInfoList_usesInterfaceContractConstants() throws Exception {
        Method method = ExApiFeignClient.class.getMethod("getHighwayServiceAreaInfoList", String.class, String.class);

        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly(ExApiFeignClient.HIGHWAY_SERVICE_AREA_INFO_PATH);
        assertThat(requestParameterName(method, 0)).isEqualTo(ExApiFeignClient.KEY_PARAMETER);
        assertThat(requestParameterName(method, 1)).isEqualTo(ExApiFeignClient.TYPE_PARAMETER);
    }

    @Test
    @DisplayName("주유소 편의시설 API 명세는 인터페이스 공통 상수를 사용한다")
    void getRestOilList_usesInterfaceContractConstants() throws Exception {
        Method method = ExApiFeignClient.class.getMethod("getRestOilList", String.class, String.class);

        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly(ExApiFeignClient.REST_OIL_LIST_PATH);
        assertThat(requestParameterName(method, 0)).isEqualTo(ExApiFeignClient.KEY_PARAMETER);
        assertThat(requestParameterName(method, 1)).isEqualTo(ExApiFeignClient.TYPE_PARAMETER);
    }

    @Test
    @DisplayName("주유소 가격 API 명세는 인터페이스 공통 상수를 사용한다")
    void getCurStateStation_usesInterfaceContractConstants() throws Exception {
        Method method = ExApiFeignClient.class.getMethod(
                "getCurStateStation", String.class, String.class, String.class, String.class);

        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly(ExApiFeignClient.CUR_STATE_STATION_PATH);
        assertRequestParameterNames(method);
    }

    @Test
    @DisplayName("주유소 가격 단건 API 명세는 serviceAreaCode2 공통 상수를 사용한다")
    void getCurStateStationWithServiceAreaCode2_usesInterfaceContractConstants() throws Exception {
        Method method = ExApiFeignClient.class.getMethod(
                "getCurStateStation", String.class, String.class, String.class, String.class, String.class);

        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly(ExApiFeignClient.CUR_STATE_STATION_PATH);
        assertRequestParameterNames(method);
        assertThat(requestParameterName(method, 4)).isEqualTo(ExApiFeignClient.SERVICE_AREA_CODE2_PARAMETER);
    }

    @Test
    @DisplayName("휴게소 음식 메뉴 API 명세는 인터페이스 공통 상수를 사용한다")
    void getRestBestfoodList_usesInterfaceContractConstants() throws Exception {
        Method method = ExApiFeignClient.class.getMethod(
                "getRestBestfoodList", String.class, String.class, String.class, String.class);

        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly(ExApiFeignClient.REST_BESTFOOD_LIST_PATH);
        assertRequestParameterNames(method);
    }

    @Test
    @DisplayName("테마휴게소 API 명세는 인터페이스 공통 상수를 사용한다")
    void getRestThemeList_usesInterfaceContractConstants() throws Exception {
        Method method = ExApiFeignClient.class.getMethod("getRestThemeList", String.class, String.class);

        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly(ExApiFeignClient.REST_THEME_LIST_PATH);
        assertThat(requestParameterName(method, 0)).isEqualTo(ExApiFeignClient.KEY_PARAMETER);
        assertThat(requestParameterName(method, 1)).isEqualTo(ExApiFeignClient.TYPE_PARAMETER);
    }

    @Test
    @DisplayName("휴게소 이벤트 API 명세는 인터페이스 공통 상수를 사용한다")
    void getRestEventList_usesInterfaceContractConstants() throws Exception {
        Method method = ExApiFeignClient.class.getMethod("getRestEventList", String.class, String.class);

        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly(ExApiFeignClient.REST_EVENT_LIST_PATH);
        assertThat(requestParameterName(method, 0)).isEqualTo(ExApiFeignClient.KEY_PARAMETER);
        assertThat(requestParameterName(method, 1)).isEqualTo(ExApiFeignClient.TYPE_PARAMETER);
    }

    private void assertRequestParameterNames(Method method) {
        assertThat(requestParameterName(method, 0)).isEqualTo(ExApiFeignClient.KEY_PARAMETER);
        assertThat(requestParameterName(method, 1)).isEqualTo(ExApiFeignClient.TYPE_PARAMETER);
        assertThat(requestParameterName(method, 2)).isEqualTo(ExApiFeignClient.NUM_OF_ROWS_PARAMETER);
        assertThat(requestParameterName(method, 3)).isEqualTo(ExApiFeignClient.PAGE_NO_PARAMETER);
    }

    private String requestParameterName(Method method, int parameterIndex) {
        return method.getParameters()[parameterIndex]
                .getAnnotation(RequestParam.class)
                .value();
    }
}

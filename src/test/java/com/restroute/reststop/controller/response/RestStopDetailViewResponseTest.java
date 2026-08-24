package com.restroute.reststop.controller.response;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.reststop.domain.RestStopEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestStopDetailViewResponseTest {

    @Test
    @DisplayName("휴게소 기본 엔티티만으로 상세 진입용 최소 응답을 구성한다")
    void from_returnsMinimalResponse() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));

        RestStopDetailViewResponse response = RestStopDetailViewResponse.from(restStop);

        assertThat(response.serviceAreaCode()).isEqualTo("A00001");
        assertThat(response.unitCode()).isEqualTo("001");
        assertThat(response.unitName()).isEqualTo("서울만남(부산)휴게소");
        assertThat(response.routeNo()).isEqualTo("0010");
        assertThat(response.routeName()).isEqualTo("경부선");
        assertThat(response.xValue()).isEqualTo("127.042514");
        assertThat(response.yValue()).isEqualTo("37.459939");
        assertThat(response.stdRestCd()).isEqualTo("000001");
    }
}

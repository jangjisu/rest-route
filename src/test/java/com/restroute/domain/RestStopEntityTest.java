package com.restroute.domain;

import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.client.response.RestStopItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestStopEntityTest {

    @Test
    @DisplayName("외부 API 휴게소 item을 entity로 변환한다")
    void from_convertsRestStopItem() {
        RestStopItem item = restStopItem("001", "서울만남(부산)휴게소");

        RestStopEntity entity = RestStopEntity.from(item);

        assertThat(entity.getUnitCode()).isEqualTo("001");
        assertThat(entity.getUnitName()).isEqualTo("서울만남(부산)휴게소");
        assertThat(entity.getRouteNo()).isEqualTo("0010");
        assertThat(entity.getRouteName()).isEqualTo("경부선");
        assertThat(entity.getXValue()).isEqualTo("127.042514");
        assertThat(entity.getYValue()).isEqualTo("37.459939");
        assertThat(entity.getStdRestCd()).isEqualTo("000001");
        assertThat(entity.getServiceAreaCode()).isEqualTo("A00001");
    }

    @Test
    @DisplayName("관리자 편집을 적용하면 필드가 바뀌고 동기화 잠금 플래그가 켜진다")
    void applyAdminEdit_updatesFieldsAndSetsOverrideFlag() {
        RestStopEntity entity = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));

        entity.applyAdminEdit("이름수정됨", "0011", "노선수정됨", "128.0", "38.0");

        assertThat(entity.getUnitName()).isEqualTo("이름수정됨");
        assertThat(entity.getRouteNo()).isEqualTo("0011");
        assertThat(entity.getRouteName()).isEqualTo("노선수정됨");
        assertThat(entity.getXValue()).isEqualTo("128.0");
        assertThat(entity.getYValue()).isEqualTo("38.0");
        assertThat(entity.isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("관리자가 새로 등록하면 코드 3종을 ADMIN- 접두사로 발급하고 동기화 잠금 상태로 시작한다")
    void createByAdmin_issuesAdminPrefixedCodesAndStartsOverridden() {
        RestStopEntity entity = RestStopEntity.createByAdmin("가평휴게소", "0650", "서울양양선", "127.5", "37.8");

        assertThat(entity.getUnitName()).isEqualTo("가평휴게소");
        assertThat(entity.getRouteNo()).isEqualTo("0650");
        assertThat(entity.getRouteName()).isEqualTo("서울양양선");
        assertThat(entity.getXValue()).isEqualTo("127.5");
        assertThat(entity.getYValue()).isEqualTo("37.8");
        assertThat(entity.getServiceAreaCode()).startsWith("ADMIN-");
        assertThat(entity.getUnitCode()).startsWith("ADMIN-");
        assertThat(entity.getStdRestCd()).startsWith("ADMIN-");
        assertThat(entity.isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("관리자가 새로 등록할 때마다 서로 다른 코드가 발급된다")
    void createByAdmin_issuesDistinctCodesEachTime() {
        RestStopEntity first = RestStopEntity.createByAdmin("A휴게소", "0650", "서울양양선", "127.5", "37.8");
        RestStopEntity second = RestStopEntity.createByAdmin("B휴게소", "0650", "서울양양선", "127.6", "37.9");

        assertThat(first.getServiceAreaCode()).isNotEqualTo(second.getServiceAreaCode());
        assertThat(first.getUnitCode()).isNotEqualTo(second.getUnitCode());
        assertThat(first.getStdRestCd()).isNotEqualTo(second.getStdRestCd());
    }

    @Test
    @DisplayName("clearAdminOverride를 호출하면 잠금 플래그가 꺼진다")
    void clearAdminOverride_resetsOverrideFlag() {
        RestStopEntity entity = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        entity.applyAdminEdit("이름수정됨", "0011", "노선수정됨", "128.0", "38.0");

        entity.clearAdminOverride();

        assertThat(entity.isAdminOverridden()).isFalse();
    }
}

package com.restroute.reststop.domain;

import static com.restroute.support.RestStopTestFixtures.restStopDetailItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.reststop.client.response.RestStopDetailItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestStopDetailEntityTest {

    @Test
    @DisplayName("외부 API 휴게소 상세 item을 entity로 변환한다")
    void from_convertsRestStopDetailItem() {
        RestStopDetailItem item = restStopDetailItem("A00078", "건천(부산)휴게소");

        RestStopDetailEntity entity = RestStopDetailEntity.from(item);

        assertThat(entity.getRouteName()).isEqualTo("경부선");
        assertThat(entity.getServiceAreaCode()).isEqualTo("A00078");
        assertThat(entity.getServiceAreaName()).isEqualTo("건천(부산)휴게소");
        assertThat(entity.getTelNo()).isEqualTo("054-751-6890");
        assertThat(entity.getBrand()).isEqualTo("투썸플레이스");
        assertThat(entity.getRouteCode()).isEqualTo("0010");
        assertThat(entity.getServiceAreaCode2()).isEqualTo("000054");
        assertThat(entity.getSvarAddr()).isEqualTo("경북 경주시 건천읍방내리 14");
        assertThat(entity.getConvenience()).isEqualTo("수유실");
        assertThat(entity.getMaintenanceYn()).isEqualTo("X");
        assertThat(entity.getTruckSaYn()).isEqualTo("X");
    }

    @Test
    @DisplayName("관리자 편집을 적용하면 필드가 바뀌고 동기화 잠금 플래그가 켜진다")
    void applyAdminEdit_updatesFieldsAndSetsOverrideFlag() {
        RestStopDetailEntity entity = RestStopDetailEntity.from(restStopDetailItem("A00078", "건천(부산)휴게소"));

        entity.applyAdminEdit("031-000-0000", "브랜드수정", "0011", "주소수정됨", "샤워실", "O", "O");

        assertThat(entity.getTelNo()).isEqualTo("031-000-0000");
        assertThat(entity.getBrand()).isEqualTo("브랜드수정");
        assertThat(entity.getRouteCode()).isEqualTo("0011");
        assertThat(entity.getSvarAddr()).isEqualTo("주소수정됨");
        assertThat(entity.getConvenience()).isEqualTo("샤워실");
        assertThat(entity.getMaintenanceYn()).isEqualTo("O");
        assertThat(entity.getTruckSaYn()).isEqualTo("O");
        assertThat(entity.isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("createEmpty는 serviceAreaCode만 채운 빈 엔티티를 만든다")
    void createEmpty_createsEntityWithOnlyServiceAreaCode() {
        RestStopDetailEntity entity = RestStopDetailEntity.createEmpty("A00099");

        assertThat(entity.getServiceAreaCode()).isEqualTo("A00099");
        assertThat(entity.getTelNo()).isNull();
        assertThat(entity.getBrand()).isNull();
        assertThat(entity.isAdminOverridden()).isFalse();
    }

    @Test
    @DisplayName("clearAdminOverride를 호출하면 잠금 플래그가 꺼진다")
    void clearAdminOverride_resetsOverrideFlag() {
        RestStopDetailEntity entity = RestStopDetailEntity.from(restStopDetailItem("A00078", "건천(부산)휴게소"));
        entity.applyAdminEdit("031-000-0000", "브랜드수정", "0011", "주소수정됨", "샤워실", "O", "O");

        entity.clearAdminOverride();

        assertThat(entity.isAdminOverridden()).isFalse();
    }
}

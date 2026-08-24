package com.restroute.oilprice.domain;

import static com.restroute.support.RestStopTestFixtures.restOilItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.oilprice.client.response.RestOilItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RestOilEntityTest {

    @Test
    @DisplayName("주유소 API 원본 필드와 정규화된 시설명을 저장한다")
    void from_mapsOriginalFieldsAndNormalizedStationName() {
        RestOilItem item = restOilItem("000002", " 서울만남 (부산) 주유소 ");

        RestOilEntity entity = RestOilEntity.from(item);

        assertThat(entity.getStandardRestCode()).isEqualTo("000002");
        assertThat(entity.getStandardRestName()).isEqualTo(" 서울만남 (부산) 주유소 ");
        assertThat(entity.getRouteCode()).isEqualTo("0010");
        assertThat(entity.getNormalizedStationName()).isEqualTo("서울만남(부산)");
        assertThat(entity.getStartTime()).isEqualTo("00:00");
        assertThat(entity.getEndTime()).isEqualTo("24:00");
        assertThat(entity.getConvenienceName()).isEqualTo("쉼터");
        assertThat(entity.getConvenienceDescription()).isEqualTo("고객쉼터");
    }

    @Test
    @DisplayName("시설명이 null이면 정규화 이름도 null이다")
    void from_keepsNormalizedStationNameNullWhenNameMissing() {
        RestOilItem item = restOilItem("000002", "서울만남(부산)주유소");
        ReflectionTestUtils.setField(item, "standardRestName", null);

        RestOilEntity entity = RestOilEntity.from(item);

        assertThat(entity.getNormalizedStationName()).isNull();
    }

    @Test
    @DisplayName("휴게소와 주유소 이름을 동일한 조회 키로 정규화한다")
    void normalizeStationName_removesFacilityTypeAndSpaces() {
        assertThat(RestOilEntity.normalizeStationName("서울만남 (부산) 휴게소")).isEqualTo("서울만남(부산)");
        assertThat(RestOilEntity.normalizeStationName("서울만남(부산)주유소")).isEqualTo("서울만남(부산)");
    }

    @Test
    @DisplayName("applyAdminLink는 연결 대상 휴게소를 설정하고 잠금 플래그를 켠다")
    void applyAdminLink_setsRestStopAndLocksRow() {
        RestOilEntity entity = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));

        entity.applyAdminLink("A00001");

        assertThat(entity.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(entity.isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("clearAdminLink는 연결을 비우고 잠금 플래그를 켠다")
    void clearAdminLink_clearsRestStopAndLocksRow() {
        RestOilEntity entity = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        entity.applyAdminLink("A00001");

        entity.clearAdminLink();

        assertThat(entity.getRestStopServiceAreaCode()).isNull();
        assertThat(entity.isAdminOverridden()).isTrue();
    }

    @Test
    @DisplayName("releaseToAutoMatching은 잠금만 풀고 현재 연결 값은 그대로 둔다")
    void releaseToAutoMatching_unlocksRowWithoutChangingLink() {
        RestOilEntity entity = RestOilEntity.from(restOilItem("000002", "서울만남(부산)주유소"));
        entity.applyAdminLink("A00001");

        entity.releaseToAutoMatching();

        assertThat(entity.getRestStopServiceAreaCode()).isEqualTo("A00001");
        assertThat(entity.isAdminOverridden()).isFalse();
    }
}

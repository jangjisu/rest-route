package com.restroute.domain;

import static com.restroute.support.RestStopTestFixtures.restOilPriceItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restroute.client.response.RestOilPriceItem;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestOilPriceEntityTest {

    @Test
    @DisplayName("주유소 가격 API 원본 필드를 문자열 그대로 저장한다")
    void from_mapsOriginalFields() {
        RestOilPriceItem item = restOilPriceItem("000002", "서울만남(부산)주유소");

        RestOilPriceEntity entity = RestOilPriceEntity.from(item);

        assertThat(entity.getRouteCode()).isEqualTo("0010");
        assertThat(entity.getServiceAreaCode()).isEqualTo("B00001");
        assertThat(entity.getRouteName()).isEqualTo("경부선");
        assertThat(entity.getDirection()).isEqualTo("부산");
        assertThat(entity.getOilCompany()).isEqualTo("AD");
        assertThat(entity.getLpgYn()).isEqualTo("Y");
        assertThat(entity.getServiceAreaName()).isEqualTo("서울만남(부산)주유소");
        assertThat(entity.getTelNo()).isEqualTo("02-573-7430");
        assertThat(entity.getGasolinePrice()).isEqualTo("1,999원");
        assertThat(entity.getDieselPrice()).isEqualTo("1,997원");
        assertThat(entity.getLpgPrice()).isEqualTo("1,157원");
        assertThat(entity.getNumOfRows()).isNull();
        assertThat(entity.getPageNo()).isNull();
        assertThat(entity.getServiceAreaCode2()).isEqualTo("000002");
        assertThat(entity.getServiceAreaAddress()).isEqualTo("서울시 서초구 원지동10-16");
    }

    @Test
    @DisplayName("기존 주유소 가격 정보를 API 원본 필드로 갱신한다")
    void updateFrom_updatesOriginalFields() {
        RestOilPriceEntity entity = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        RestOilPriceItem changed = restOilPriceItem("000002", "서울만남(부산)주유소");
        org.springframework.test.util.ReflectionTestUtils.setField(changed, "oilCompany", "SK");
        org.springframework.test.util.ReflectionTestUtils.setField(changed, "gasolinePrice", "1,888원");
        org.springframework.test.util.ReflectionTestUtils.setField(changed, "dieselPrice", "1,777원");
        org.springframework.test.util.ReflectionTestUtils.setField(changed, "lpgPrice", "X");
        org.springframework.test.util.ReflectionTestUtils.setField(changed, "telNo", "02-000-0000");

        entity.updateFrom(changed);

        assertThat(entity.getOilCompany()).isEqualTo("SK");
        assertThat(entity.getGasolinePrice()).isEqualTo("1,888원");
        assertThat(entity.getDieselPrice()).isEqualTo("1,777원");
        assertThat(entity.getLpgPrice()).isEqualTo("X");
        assertThat(entity.getTelNo()).isEqualTo("02-000-0000");
    }

    @Test
    @DisplayName("주유소 가격 생성 시 갱신 시각을 저장한다")
    void from_setsLastRefreshedAt() {
        LocalDateTime refreshedAt = LocalDateTime.of(2026, 6, 16, 7, 30);

        RestOilPriceEntity entity = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"), refreshedAt);

        assertThat(entity.getLastRefreshedAt()).isEqualTo(refreshedAt);
    }

    @Test
    @DisplayName("주유소 가격 갱신 시 갱신 시각도 바꾼다")
    void updateFrom_updatesLastRefreshedAt() {
        RestOilPriceEntity entity = RestOilPriceEntity.from(restOilPriceItem("000002", "서울만남(부산)주유소"));
        RestOilPriceItem changed = restOilPriceItem("000002", "서울만남(부산)주유소");
        LocalDateTime refreshedAt = LocalDateTime.of(2026, 6, 16, 7, 40);

        entity.updateFrom(changed, refreshedAt);

        assertThat(entity.getLastRefreshedAt()).isEqualTo(refreshedAt);
    }

    @Test
    @DisplayName("주유 가격 행은 관리자 잠금 상태를 별도로 소유하지 않는다")
    void doesNotOwnAdminOverrideState() {
        assertThatThrownBy(() -> RestOilPriceEntity.class.getDeclaredField("adminOverridden"))
                .isInstanceOf(NoSuchFieldException.class);
    }
}

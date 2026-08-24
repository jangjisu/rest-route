package com.restroute.reststop.domain;

import static com.restroute.support.RestStopTestFixtures.highwayServiceAreaInfoItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.reststop.client.response.HighwayServiceAreaInfoItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HighwayServiceAreaInfoEntityTest {

    @Test
    @DisplayName("외부 API 고속도로 휴게소 정보 item을 entity로 변환한다")
    void from_convertsHighwayServiceAreaInfoItem() {
        HighwayServiceAreaInfoItem item = highwayServiceAreaInfoItem("000561", "북대전(논산)졸음쉼터");

        HighwayServiceAreaInfoEntity entity = HighwayServiceAreaInfoEntity.from(item);

        assertThat(entity.getServiceAreaCode()).isEqualTo("000561");
        assertThat(entity.getServiceAreaName()).isEqualTo("북대전(논산)졸음쉼터");
        assertThat(entity.getRouteCode()).isEqualTo("2510");
        assertThat(entity.getRouteName()).isEqualTo("호남선의 지선");
        assertThat(entity.getHeadquartersCode()).isEqualTo("400000");
        assertThat(entity.getHeadquartersName()).isEqualTo("대전충남본부");
        assertThat(entity.getBranchOfficeCode()).isEqualTo("410200");
        assertThat(entity.getBranchOfficeName()).isEqualTo("대전");
        assertThat(entity.getFacilityTypeCode()).isEqualTo("0");
        assertThat(entity.getFacilityTypeName()).isEqualTo("휴게소");
        assertThat(entity.getDirectionTypeCode()).isEqualTo("1");
        assertThat(entity.getDirectionTypeName()).isEqualTo("하행");
        assertThat(entity.getPostalCode()).isEqualTo("30535 ");
        assertThat(entity.getServiceAreaAddress()).isEqualTo("대전광역시 유성구 방현동 86");
        assertThat(entity.getCompactCarParkingCount()).isEqualTo("0");
        assertThat(entity.getFullSizeCarParkingCount()).isEqualTo("0");
        assertThat(entity.getDisabledParkingCount()).isEqualTo("0");
        assertThat(entity.getBusinessFacilityCode()).isEqualTo("A00282");
        assertThat(entity.getRepresentativeTelNo()).isEqualTo("0420000000");
    }
}

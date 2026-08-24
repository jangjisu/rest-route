package com.restroute.reststop.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HighwayServiceAreaInfoItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("고속도로 휴게소 정보 API 응답 필드를 item으로 매핑한다")
    void readValue_mapsHighwayServiceAreaInfoFields() throws Exception {
        String json = """
                {
                  "routeCd": "2510",
                  "svarAddr": "대전광역시 유성구 방현동 86 북대전IC(논산)졸음쉼터",
                  "routeNm": "호남선의 지선",
                  "hdqrNm": "대전충남본부",
                  "mtnofNm": "대전",
                  "svarCd": "000561",
                  "svarNm": "북대전(논산)졸음쉼터",
                  "hdqrCd": "400000",
                  "mtnofCd": "410200",
                  "svarGsstClssCd": "0",
                  "svarGsstClssNm": "휴게소",
                  "gudClssCd": "1",
                  "gudClssNm": "하행",
                  "pstnoCd": "30535 ",
                  "cocrPrkgTrcn": "0",
                  "fscarPrkgTrcn": "0",
                  "dspnPrkgTrcn": "0",
                  "bsopAdtnlFcltCd": "A00282",
                  "rprsTelNo": "0420000000"
                }
                """;

        HighwayServiceAreaInfoItem item = objectMapper.readValue(json, HighwayServiceAreaInfoItem.class);

        assertThat(item.getServiceAreaCode()).isEqualTo("000561");
        assertThat(item.getServiceAreaName()).isEqualTo("북대전(논산)졸음쉼터");
        assertThat(item.getRouteCode()).isEqualTo("2510");
        assertThat(item.getRouteName()).isEqualTo("호남선의 지선");
        assertThat(item.getHeadquartersCode()).isEqualTo("400000");
        assertThat(item.getHeadquartersName()).isEqualTo("대전충남본부");
        assertThat(item.getBranchOfficeCode()).isEqualTo("410200");
        assertThat(item.getBranchOfficeName()).isEqualTo("대전");
        assertThat(item.getFacilityTypeCode()).isEqualTo("0");
        assertThat(item.getFacilityTypeName()).isEqualTo("휴게소");
        assertThat(item.getDirectionTypeCode()).isEqualTo("1");
        assertThat(item.getDirectionTypeName()).isEqualTo("하행");
        assertThat(item.getPostalCode()).isEqualTo("30535 ");
        assertThat(item.getServiceAreaAddress()).isEqualTo("대전광역시 유성구 방현동 86 북대전IC(논산)졸음쉼터");
        assertThat(item.getCompactCarParkingCount()).isEqualTo("0");
        assertThat(item.getFullSizeCarParkingCount()).isEqualTo("0");
        assertThat(item.getDisabledParkingCount()).isEqualTo("0");
        assertThat(item.getBusinessFacilityCode()).isEqualTo("A00282");
        assertThat(item.getRepresentativeTelNo()).isEqualTo("0420000000");
    }
}

package com.restroute.oilprice.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestOilPriceItemTest {

    @Test
    @DisplayName("curStateStation list 항목을 원본 문자열 필드로 매핑한다")
    void readValue_mapsRestOilPriceFields() throws Exception {
        String json = """
                {
                  "routeCode": "0010",
                  "serviceAreaCode": "B00001",
                  "routeName": "경부선",
                  "direction": "부산",
                  "oilCompany": "AD",
                  "lpgYn": "Y",
                  "serviceAreaName": "서울만남(부산)주유소",
                  "telNo": "02-573-7430",
                  "gasolinePrice": "1,999원",
                  "diselPrice": "1,997원",
                  "lpgPrice": "1,157원",
                  "numOfRows": null,
                  "pageNo": null,
                  "serviceAreaCode2": "000002",
                  "svarAddr": "서울시 서초구 원지동10-16"
                }
                """;

        RestOilPriceItem item = new ObjectMapper().readValue(json, RestOilPriceItem.class);

        assertThat(item.getRouteCode()).isEqualTo("0010");
        assertThat(item.getServiceAreaCode()).isEqualTo("B00001");
        assertThat(item.getRouteName()).isEqualTo("경부선");
        assertThat(item.getDirection()).isEqualTo("부산");
        assertThat(item.getOilCompany()).isEqualTo("AD");
        assertThat(item.getLpgYn()).isEqualTo("Y");
        assertThat(item.getServiceAreaName()).isEqualTo("서울만남(부산)주유소");
        assertThat(item.getTelNo()).isEqualTo("02-573-7430");
        assertThat(item.getGasolinePrice()).isEqualTo("1,999원");
        assertThat(item.getDieselPrice()).isEqualTo("1,997원");
        assertThat(item.getLpgPrice()).isEqualTo("1,157원");
        assertThat(item.getNumOfRows()).isNull();
        assertThat(item.getPageNo()).isNull();
        assertThat(item.getServiceAreaCode2()).isEqualTo("000002");
        assertThat(item.getServiceAreaAddress()).isEqualTo("서울시 서초구 원지동10-16");
    }
}

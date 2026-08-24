package com.restroute.reststop.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestStopDetailItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("편의시설 API 응답 필드를 휴게소 상세 item으로 매핑한다")
    void readValue_mapsRestStopDetailFields() throws Exception {
        String json = """
                {
                  "direction": null,
                  "pageNo": null,
                  "numOfRows": null,
                  "routeName": "세종포천선",
                  "serviceAreaCode": "A00315",
                  "serviceAreaName": "처인휴게소",
                  "telNo": "031-323-5325",
                  "brand": "스타벅스 등",
                  "routeCode": "0290",
                  "serviceAreaCode2": "000605",
                  "svarAddr": "경기 용인시 처인구 모현면동림리(곡현로619번길) 36",
                  "convenience": "수유실|수면실|샤워실|세탁실|쉼터",
                  "maintenanceYn": "X",
                  "truckSaYn": "O"
                }
                """;

        RestStopDetailItem item = objectMapper.readValue(json, RestStopDetailItem.class);

        assertThat(item.getRouteName()).isEqualTo("세종포천선");
        assertThat(item.getServiceAreaCode()).isEqualTo("A00315");
        assertThat(item.getServiceAreaName()).isEqualTo("처인휴게소");
        assertThat(item.getTelNo()).isEqualTo("031-323-5325");
        assertThat(item.getBrand()).isEqualTo("스타벅스 등");
        assertThat(item.getRouteCode()).isEqualTo("0290");
        assertThat(item.getServiceAreaCode2()).isEqualTo("000605");
        assertThat(item.getSvarAddr()).isEqualTo("경기 용인시 처인구 모현면동림리(곡현로619번길) 36");
        assertThat(item.getConvenience()).isEqualTo("수유실|수면실|샤워실|세탁실|쉼터");
        assertThat(item.getMaintenanceYn()).isEqualTo("X");
        assertThat(item.getTruckSaYn()).isEqualTo("O");
    }
}

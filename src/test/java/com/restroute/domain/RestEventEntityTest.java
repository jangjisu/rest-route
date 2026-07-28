package com.restroute.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.client.response.RestEventItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestEventEntityTest {

    @Test
    @DisplayName("휴게소 이벤트 item을 엔티티로 매핑한다")
    void from_mapsRestEventItemFields() throws Exception {
        RestEventItem item = sampleItem("000001", "1665", "TEN+1 이벤트");

        RestEventEntity entity = RestEventEntity.from(item);

        assertThat(entity.getStdRestCd()).isEqualTo("000001");
        assertThat(entity.getStdRestNm()).isEqualTo("서울만남(부산)휴게소");
        assertThat(entity.getEventSeq()).isEqualTo("1665");
        assertThat(entity.getEventNm()).isEqualTo("TEN+1 이벤트");
        assertThat(entity.getEventDetail()).isEqualTo("한식당 식사 10번 이용 시 1번 무료");
        assertThat(entity.getStime()).isEqualTo("2020-01-01");
        assertThat(entity.getEtime()).isEqualTo("2027-12-31");
        assertThat(entity.getRouteNm()).isEqualTo("경부선");
    }

    @Test
    @DisplayName("updateFrom은 이벤트 내용만 갱신하고 자연키(stdRestCd/eventSeq)는 그대로 둔다")
    void updateFrom_updatesEventFieldsOnly() throws Exception {
        RestEventEntity entity = RestEventEntity.from(sampleItem("000001", "1665", "TEN+1 이벤트"));

        entity.updateFrom(sampleItem("000001", "1665", "TEN+2 이벤트로 개편"));

        assertThat(entity.getStdRestCd()).isEqualTo("000001");
        assertThat(entity.getEventSeq()).isEqualTo("1665");
        assertThat(entity.getEventNm()).isEqualTo("TEN+2 이벤트로 개편");
    }

    @Test
    @DisplayName("updateRestStopServiceAreaCode는 휴게소 코드를 연결한다")
    void updateRestStopServiceAreaCode_updatesLinkedCode() throws Exception {
        RestEventEntity entity = RestEventEntity.from(sampleItem("000001", "1665", "TEN+1 이벤트"));

        entity.updateRestStopServiceAreaCode("A00001");

        assertThat(entity.getRestStopServiceAreaCode()).isEqualTo("A00001");
    }

    private RestEventItem sampleItem(String stdRestCd, String eventSeq, String eventNm) throws Exception {
        String json = """
                {
                  "stdRestCd": "%s",
                  "stdRestNm": "서울만남(부산)휴게소",
                  "eventSeq": "%s",
                  "eventNm": "%s",
                  "eventDetail": "한식당 식사 10번 이용 시 1번 무료",
                  "stime": "2020-01-01",
                  "etime": "2027-12-31",
                  "routeCd": "0010",
                  "routeNm": "경부선",
                  "svarAddr": "서울 서초구 원지동10-16"
                }
                """.formatted(stdRestCd, eventSeq, eventNm);
        return new ObjectMapper().readValue(json, RestEventItem.class);
    }
}

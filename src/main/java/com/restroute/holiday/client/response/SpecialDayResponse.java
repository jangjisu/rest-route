package com.restroute.flight.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 공공데이터포털 "한국천문연구원_특일 정보"(getRestDeInfo) 응답. 이 서비스의 모든 오퍼레이션이
 * 공통으로 쓰는 {response:{header,body}} 봉투 구조를 그대로 반영한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpecialDayResponse(Response response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {

        private static final String SUCCESS_CODE = "00";

        public boolean isSuccess() {
            return SUCCESS_CODE.equals(resultCode);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Items items) {

        public List<Item> itemsOrEmpty() {
            return items == null || items.item() == null ? List.of() : items.item();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<Item> item) {}

    /** locdate는 YYYYMMDD 형식(예: "20260815")이다. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String locdate, String dateName, String isHoliday) {

        private static final String HOLIDAY_FLAG = "Y";

        public boolean isActualHoliday() {
            return HOLIDAY_FLAG.equals(isHoliday);
        }
    }
}

package com.restroute.holiday.client;

import com.restroute.holiday.client.response.SpecialDayResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 공공데이터포털 "한국천문연구원_특일 정보" Feign Client (apis.data.go.kr).
 */
@FeignClient(name = "special-day", url = "${special-day.api.url}")
public interface SpecialDayFeignClient {

    String REST_DE_INFO_PATH = "/getRestDeInfo";

    /** 공휴일(실제 쉬는 날, 대체공휴일 포함) 정보를 연 단위로 조회한다. */
    @GetMapping(REST_DE_INFO_PATH)
    SpecialDayResponse getRestDeInfo(
            @RequestParam("solYear") String solYear,
            @RequestParam("numOfRows") int numOfRows,
            @RequestParam("pageNo") int pageNo,
            @RequestParam("_type") String type,
            @RequestParam("ServiceKey") String serviceKey);
}

package com.restroute.flight.client;

import com.restroute.flight.client.response.IncheonAirlineApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 인천국제공항공사_취항 항공사 현황 조회 Feign Client (data.go.kr, apis.data.go.kr/B551177/StatusOfSrvAirlines).
 */
@FeignClient(name = "incheon-airport", url = "${incheon.api.url}")
public interface IncheonFeignClient {

    String SERVICE_AIRLINE_INFO_PATH = "/getServiceAirlineInfo";

    @GetMapping(SERVICE_AIRLINE_INFO_PATH)
    IncheonAirlineApiResponse getServiceAirlineInfo(
            @RequestParam("serviceKey") String serviceKey,
            @RequestParam("type") String type,
            @RequestParam("numOfRows") int numOfRows);
}

package com.restroute.client;

import com.restroute.client.response.KakaoDirectionsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 카카오모빌리티 자동차 길찾기 Feign Client (apis-navi.kakaomobility.com)
 */
@FeignClient(name = "kakao-navi", url = "${kakao.navi.url}")
public interface KakaoNaviFeignClient {

    String DIRECTIONS_PATH = "/v1/directions";

    @GetMapping(DIRECTIONS_PATH)
    KakaoDirectionsResponse getDirections(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam("priority") String priority,
            @RequestParam("road_details") boolean roadDetails);
}

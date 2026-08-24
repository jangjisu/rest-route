package com.restroute.common.client;

import com.restroute.common.client.response.KakaoLocalSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 카카오 로컬(주소/장소 검색) Feign Client (dapi.kakao.com)
 */
@FeignClient(name = "kakao-local", url = "${kakao.local.url}")
public interface KakaoLocalFeignClient {

    String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";

    @GetMapping(KEYWORD_SEARCH_PATH)
    KakaoLocalSearchResponse searchKeyword(
            @RequestHeader("Authorization") String authorization, @RequestParam("query") String query);
}

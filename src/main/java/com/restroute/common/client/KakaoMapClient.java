package com.restroute.common.client;

import com.restroute.common.client.exception.KakaoApiException;
import com.restroute.common.client.response.KakaoDirectionsResponse;
import com.restroute.common.client.response.KakaoLocalSearchResponse;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KakaoMapClient {

    private static final String DEFAULT_PRIORITY = "RECOMMEND";
    private static final boolean ROAD_DETAILS = true;
    private static final boolean ALTERNATIVES = true;
    private static final String API_NAME = "KAKAO";

    private final KakaoNaviFeignClient kakaoNaviFeignClient;
    private final KakaoLocalFeignClient kakaoLocalFeignClient;
    private final String apiKey;

    public KakaoMapClient(
            KakaoNaviFeignClient kakaoNaviFeignClient,
            KakaoLocalFeignClient kakaoLocalFeignClient,
            @Value("${kakao.rest-api-key:}") String apiKey) {
        this.kakaoNaviFeignClient = kakaoNaviFeignClient;
        this.kakaoLocalFeignClient = kakaoLocalFeignClient;
        this.apiKey = apiKey;
    }

    public KakaoLocalSearchResponse searchKeyword(String query) {
        return fetch("local keyword search", () -> kakaoLocalFeignClient.searchKeyword(authorization(), query));
    }

    public KakaoDirectionsResponse getDirections(String origin, String destination) {
        return fetch(
                "directions",
                () -> kakaoNaviFeignClient.getDirections(
                        authorization(), origin, destination, DEFAULT_PRIORITY, ROAD_DETAILS, ALTERNATIVES));
    }

    private String authorization() {
        return "KakaoAK " + apiKey;
    }

    private <T> T fetch(String requestDescription, Supplier<T> request) {
        log.info("External API request started. api={}, endpoint={}", API_NAME, requestDescription);
        try {
            T response = request.get();
            if (response == null) {
                throw new KakaoApiException(requestDescription, "empty response");
            }
            log.info("External API request succeeded. api={}, endpoint={}", API_NAME, requestDescription);
            return response;
        } catch (KakaoApiException e) {
            log.warn(
                    "External API request failed. api={}, endpoint={}, message={}",
                    API_NAME,
                    requestDescription,
                    e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.warn(
                    "External API request failed. api={}, endpoint={}, message={}",
                    API_NAME,
                    requestDescription,
                    e.getMessage());
            throw new KakaoApiException(requestDescription, e.getMessage(), e);
        }
    }
}

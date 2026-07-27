package com.restroute.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.restroute.client.exception.KakaoApiException;
import com.restroute.client.response.KakaoDirectionsResponse;
import com.restroute.client.response.KakaoLocalSearchResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KakaoMapClientTest {

    @Mock
    private KakaoNaviFeignClient kakaoNaviFeignClient;

    @Mock
    private KakaoLocalFeignClient kakaoLocalFeignClient;

    private KakaoMapClient kakaoMapClient;

    @BeforeEach
    void setUp() {
        kakaoMapClient = new KakaoMapClient(kakaoNaviFeignClient, kakaoLocalFeignClient, "test-key");
    }

    @Test
    @DisplayName("searchKeyword는 KakaoAK 인증 헤더로 로컬 검색을 호출한다")
    void searchKeyword_passesAuthorization() {
        KakaoLocalSearchResponse response = new KakaoLocalSearchResponse(List.of());
        when(kakaoLocalFeignClient.searchKeyword("KakaoAK test-key", "해운대")).thenReturn(response);

        KakaoLocalSearchResponse result = kakaoMapClient.searchKeyword("해운대");

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("getDirections는 KakaoAK 헤더와 RECOMMEND 우선순위, road_details=true로 길찾기를 호출한다")
    void getDirections_passesAuthorizationAndPriority() {
        KakaoDirectionsResponse response = new KakaoDirectionsResponse(List.of());
        when(kakaoNaviFeignClient.getDirections(
                        eq("KakaoAK test-key"), eq("127.0,37.0"), eq("129.0,35.0"), eq("RECOMMEND"), eq(true)))
                .thenReturn(response);

        KakaoDirectionsResponse result = kakaoMapClient.getDirections("127.0,37.0", "129.0,35.0");

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("응답이 null이면 KakaoApiException을 던진다")
    void fetch_throwsOnNull() {
        when(kakaoLocalFeignClient.searchKeyword("KakaoAK test-key", "x")).thenReturn(null);

        assertThatThrownBy(() -> kakaoMapClient.searchKeyword("x"))
                .isInstanceOf(KakaoApiException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    @DisplayName("호출이 런타임 예외를 던지면 KakaoApiException으로 감싼다")
    void fetch_wrapsRuntimeException() {
        when(kakaoNaviFeignClient.getDirections("KakaoAK test-key", "a", "b", "RECOMMEND", true))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> kakaoMapClient.getDirections("a", "b"))
                .isInstanceOf(KakaoApiException.class)
                .hasMessageContaining("boom");
    }
}

package com.restroute.client;

import static com.restroute.support.RestStopTestFixtures.highwayServiceAreaInfoResponse;
import static com.restroute.support.RestStopTestFixtures.restEventResponse;
import static com.restroute.support.RestStopTestFixtures.restStopDetailResponse;
import static com.restroute.support.RestStopTestFixtures.restStopResponse;
import static com.restroute.support.RestStopTestFixtures.restThemeResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.client.exception.ExApiException;
import com.restroute.client.response.HighwayServiceAreaInfoResponse;
import com.restroute.client.response.RestStopDetailResponse;
import com.restroute.client.response.RestStopResponse;
import com.restroute.oilprice.client.response.RestOilPriceResponse;
import com.restroute.oilprice.client.response.RestOilResponse;
import com.restroute.reststopcontent.client.response.RestBestfoodResponse;
import com.restroute.reststopcontent.client.response.RestEventResponse;
import com.restroute.reststopcontent.client.response.RestThemeResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExApiClientTest {

    private static final String API_URL = "https://data.ex.co.kr";

    @Mock
    private ExApiFeignClient exApiFeignClient;

    private ExApiClient exApiClient;

    @BeforeEach
    void setUp() {
        exApiClient = new ExApiClient(exApiFeignClient, API_URL, "test-key");
    }

    @Test
    @DisplayName("휴게소 위치 API 호출 시 공통 인증키와 JSON 포맷을 적용한다")
    void getLocationInfoRest_appliesDefaultParameters() {
        RestStopResponse response = restStopResponse("SUCCESS", "1", List.of());
        when(exApiFeignClient.getLocationInfoRest("test-key", "json", "99", "2"))
                .thenReturn(response);

        RestStopResponse result = exApiClient.getLocationInfoRest(2);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("휴게소 편의시설 API 호출 시 공통 인증키와 JSON 포맷을 적용한다")
    void getConvenienceServiceArea_appliesDefaultParameters() {
        RestStopDetailResponse response = restStopDetailResponse("SUCCESS", "1", List.of());
        when(exApiFeignClient.getConvenienceServiceArea("test-key", "json", "99", "2"))
                .thenReturn(response);

        RestStopDetailResponse result = exApiClient.getConvenienceServiceArea(2);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("고속도로 휴게소 정보 API 호출 시 공통 인증키와 JSON 포맷을 적용한다")
    void getHighwayServiceAreaInfoList_appliesDefaultParameters() {
        HighwayServiceAreaInfoResponse response = highwayServiceAreaInfoResponse("SUCCESS", List.of());
        when(exApiFeignClient.getHighwayServiceAreaInfoList("test-key", "json")).thenReturn(response);

        HighwayServiceAreaInfoResponse result = exApiClient.getHighwayServiceAreaInfoList();

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("테마휴게소 API 호출 시 공통 인증키와 JSON 포맷을 적용한다")
    void getRestThemeList_appliesDefaultParameters() {
        RestThemeResponse response = restThemeResponse("SUCCESS", List.of());
        when(exApiFeignClient.getRestThemeList("test-key", "json")).thenReturn(response);

        RestThemeResponse result = exApiClient.getRestThemeList();

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("휴게소 이벤트 API 호출 시 공통 인증키와 JSON 포맷을 적용한다")
    void getRestEventList_appliesDefaultParameters() {
        RestEventResponse response = restEventResponse("SUCCESS", List.of());
        when(exApiFeignClient.getRestEventList("test-key", "json")).thenReturn(response);

        RestEventResponse result = exApiClient.getRestEventList();

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("주유소 편의시설 API 호출 시 공통 인증키와 JSON 포맷을 적용한다")
    void getRestOilList_appliesDefaultParameters() throws Exception {
        RestOilResponse response = new ObjectMapper().readValue("{\"code\":\"SUCCESS\"}", RestOilResponse.class);
        when(exApiFeignClient.getRestOilList("test-key", "json")).thenReturn(response);

        RestOilResponse result = exApiClient.getRestOilList();

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("주유소 가격 API 호출 시 공통 인증키와 페이지 파라미터를 적용한다")
    void getCurStateStation_appliesDefaultParameters() throws Exception {
        RestOilPriceResponse response =
                new ObjectMapper().readValue("{\"code\":\"SUCCESS\"}", RestOilPriceResponse.class);
        when(exApiFeignClient.getCurStateStation("test-key", "json", "99", "2")).thenReturn(response);

        RestOilPriceResponse result = exApiClient.getCurStateStation(2);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("주유소 가격 단건 API 호출 시 주유소 코드를 적용한다")
    void getCurStateStationByServiceAreaCode2_appliesServiceAreaCode2() throws Exception {
        RestOilPriceResponse response =
                new ObjectMapper().readValue("{\"code\":\"SUCCESS\"}", RestOilPriceResponse.class);
        when(exApiFeignClient.getCurStateStation("test-key", "json", "99", "1", "000002"))
                .thenReturn(response);

        RestOilPriceResponse result = exApiClient.getCurStateStationByServiceAreaCode2("000002");

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("휴게소 음식 메뉴 API 호출 시 공통 인증키와 페이지 파라미터를 적용한다")
    void getRestBestfoodList_appliesDefaultParameters() throws Exception {
        RestBestfoodResponse response =
                new ObjectMapper().readValue("{\"code\":\"SUCCESS\"}", RestBestfoodResponse.class);
        when(exApiFeignClient.getRestBestfoodList("test-key", "json", "99", "2"))
                .thenReturn(response);

        RestBestfoodResponse result = exApiClient.getRestBestfoodList(2);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("휴게소 음식 메뉴 API 실패에 실제 요청 URL과 오류 메시지를 포함한다")
    void getRestBestfoodList_includesRequestUrlAndResponseMessageWhenApiFails() throws Exception {
        RestBestfoodResponse response = new ObjectMapper()
                .readValue("{\"code\":\"ERROR\",\"message\":\"인증키가 유효하지 않습니다.\"}", RestBestfoodResponse.class);
        when(exApiFeignClient.getRestBestfoodList("test-key", "json", "99", "2"))
                .thenReturn(response);

        assertThatThrownBy(() -> exApiClient.getRestBestfoodList(2))
                .isInstanceOf(ExApiException.class)
                .hasMessage(
                        "Failed to fetch API. requestUrl=https://data.ex.co.kr/openapi/restinfo/restBestfoodList?key=<redacted>&type=json&numOfRows=99&pageNo=2, message=인증키가 유효하지 않습니다.");
    }

    @Test
    @DisplayName("휴게소 위치 API 실패에 실제 요청 URL과 일반 오류 메시지를 포함한다")
    void getLocationInfoRest_includesRequestUrlAndResponseMessageWhenApiFails() {
        RestStopResponse response = restStopResponse("ERROR", "1", List.of());
        ReflectionTestUtils.setField(response, "message", "인증키가 유효하지 않습니다.");
        when(exApiFeignClient.getLocationInfoRest("test-key", "json", "99", "2"))
                .thenReturn(response);

        assertThatThrownBy(() -> exApiClient.getLocationInfoRest(2))
                .isInstanceOf(ExApiException.class)
                .hasMessage(
                        "Failed to fetch API. requestUrl=https://data.ex.co.kr/openapi/locationinfo/locationinfoRest?key=<redacted>&type=json&numOfRows=99&pageNo=2, message=인증키가 유효하지 않습니다.");
    }

    @Test
    @DisplayName("휴게소 편의시설 API 실패에 실제 요청 URL과 upstream 오류 메시지를 포함한다")
    void getConvenienceServiceArea_includesRequestUrlAndUpstreamMessageWhenApiFails() throws Exception {
        String json = """
                {
                  "exception": {
                    "message": "For input string: \\"\\""
                  }
                }
                """;
        RestStopDetailResponse response = new ObjectMapper().readValue(json, RestStopDetailResponse.class);
        when(exApiFeignClient.getConvenienceServiceArea("test-key", "json", "99", "2"))
                .thenReturn(response);

        assertThatThrownBy(() -> exApiClient.getConvenienceServiceArea(2))
                .isInstanceOf(ExApiException.class)
                .hasMessage(
                        "Failed to fetch API. requestUrl=https://data.ex.co.kr/openapi/business/conveniServiceArea?key=<redacted>&type=json&numOfRows=99&pageNo=2, message=For input string: \"\"");
    }

    @Test
    @DisplayName("고속도로 휴게소 정보 API 빈 응답에 실제 요청 URL을 포함한다")
    void getHighwayServiceAreaInfoList_includesRequestUrlWhenResponseIsEmpty() {
        when(exApiFeignClient.getHighwayServiceAreaInfoList("test-key", "json")).thenReturn(null);

        assertThatThrownBy(exApiClient::getHighwayServiceAreaInfoList)
                .isInstanceOf(ExApiException.class)
                .hasMessage(
                        "Failed to fetch API. requestUrl=https://data.ex.co.kr/openapi/restinfo/hiwaySvarInfoList?key=<redacted>&type=json, message=empty response");
    }

    @Test
    @DisplayName("주유소 편의시설 API 실패에 실제 요청 URL과 오류 메시지를 포함한다")
    void getRestOilList_includesRequestUrlAndResponseMessageWhenApiFails() throws Exception {
        RestOilResponse response = new ObjectMapper()
                .readValue("{\"code\":\"ERROR\",\"message\":\"인증키가 유효하지 않습니다.\"}", RestOilResponse.class);
        when(exApiFeignClient.getRestOilList("test-key", "json")).thenReturn(response);

        assertThatThrownBy(exApiClient::getRestOilList)
                .isInstanceOf(ExApiException.class)
                .hasMessage(
                        "Failed to fetch API. requestUrl=https://data.ex.co.kr/openapi/restinfo/restOilList?key=<redacted>&type=json, message=인증키가 유효하지 않습니다.");
    }

    @Test
    @DisplayName("주유소 가격 API 실패에 실제 요청 URL과 오류 메시지를 포함한다")
    void getCurStateStation_includesRequestUrlAndResponseMessageWhenApiFails() throws Exception {
        RestOilPriceResponse response = new ObjectMapper()
                .readValue("{\"code\":\"ERROR\",\"message\":\"인증키가 유효하지 않습니다.\"}", RestOilPriceResponse.class);
        when(exApiFeignClient.getCurStateStation("test-key", "json", "99", "2")).thenReturn(response);

        assertThatThrownBy(() -> exApiClient.getCurStateStation(2))
                .isInstanceOf(ExApiException.class)
                .hasMessage(
                        "Failed to fetch API. requestUrl=https://data.ex.co.kr/openapi/business/curStateStation?key=<redacted>&type=json&numOfRows=99&pageNo=2, message=인증키가 유효하지 않습니다.");
    }

    @Test
    @DisplayName("주유소 가격 단건 API 실패에 실제 요청 URL과 오류 메시지를 포함한다")
    void getCurStateStationByServiceAreaCode2_includesRequestUrlWhenApiFails() throws Exception {
        RestOilPriceResponse response = new ObjectMapper()
                .readValue("{\"code\":\"ERROR\",\"message\":\"인증키가 유효하지 않습니다.\"}", RestOilPriceResponse.class);
        when(exApiFeignClient.getCurStateStation("test-key", "json", "99", "1", "000002"))
                .thenReturn(response);

        assertThatThrownBy(() -> exApiClient.getCurStateStationByServiceAreaCode2("000002"))
                .isInstanceOf(ExApiException.class)
                .hasMessage(
                        "Failed to fetch API. requestUrl=https://data.ex.co.kr/openapi/business/curStateStation?key=<redacted>&type=json&numOfRows=99&pageNo=1&serviceAreaCode2=000002, message=인증키가 유효하지 않습니다.");
    }

    @Test
    @DisplayName("Feign 호출 예외에 실제 요청 URL과 원인 메시지를 포함한다")
    void getLocationInfoRest_includesRequestUrlAndCauseWhenFeignCallFails() {
        when(exApiFeignClient.getLocationInfoRest("test-key", "json", "99", "2"))
                .thenThrow(new IllegalStateException("Request Blocked"));

        assertThatThrownBy(() -> exApiClient.getLocationInfoRest(2))
                .isInstanceOf(ExApiException.class)
                .hasMessage(
                        "Failed to fetch API. requestUrl=https://data.ex.co.kr/openapi/locationinfo/locationinfoRest?key=<redacted>&type=json&numOfRows=99&pageNo=2, message=Request Blocked")
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}

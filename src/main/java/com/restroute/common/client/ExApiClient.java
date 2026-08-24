package com.restroute.common.client;

import static com.restroute.common.client.ExApiFeignClient.CONVENIENCE_SERVICE_AREA_PATH;
import static com.restroute.common.client.ExApiFeignClient.CUR_STATE_STATION_PATH;
import static com.restroute.common.client.ExApiFeignClient.HIGHWAY_SERVICE_AREA_INFO_PATH;
import static com.restroute.common.client.ExApiFeignClient.KEY_PARAMETER;
import static com.restroute.common.client.ExApiFeignClient.LOCATION_INFO_REST_PATH;
import static com.restroute.common.client.ExApiFeignClient.NUM_OF_ROWS_PARAMETER;
import static com.restroute.common.client.ExApiFeignClient.PAGE_NO_PARAMETER;
import static com.restroute.common.client.ExApiFeignClient.REST_BESTFOOD_LIST_PATH;
import static com.restroute.common.client.ExApiFeignClient.REST_EVENT_LIST_PATH;
import static com.restroute.common.client.ExApiFeignClient.REST_OIL_LIST_PATH;
import static com.restroute.common.client.ExApiFeignClient.REST_STOP_NUM_OF_ROWS;
import static com.restroute.common.client.ExApiFeignClient.REST_THEME_LIST_PATH;
import static com.restroute.common.client.ExApiFeignClient.SERVICE_AREA_CODE2_PARAMETER;
import static com.restroute.common.client.ExApiFeignClient.TYPE_PARAMETER;

import com.restroute.common.client.exception.ExApiException;
import com.restroute.common.client.response.ExApiResponse;
import com.restroute.oilprice.client.response.RestOilPriceResponse;
import com.restroute.oilprice.client.response.RestOilResponse;
import com.restroute.reststop.client.response.HighwayServiceAreaInfoResponse;
import com.restroute.reststop.client.response.RestStopDetailResponse;
import com.restroute.reststop.client.response.RestStopResponse;
import com.restroute.reststopcontent.client.response.RestBestfoodResponse;
import com.restroute.reststopcontent.client.response.RestEventResponse;
import com.restroute.reststopcontent.client.response.RestThemeResponse;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class ExApiClient {

    private static final String API_NAME = "DATA_EX";

    private final ExApiFeignClient exApiFeignClient;
    private final String apiUrl;
    private final String apiKey;

    public ExApiClient(
            ExApiFeignClient exApiFeignClient,
            @Value("${ex.api.url}") String apiUrl,
            @Value("${ex.api.key}") String apiKey) {
        this.exApiFeignClient = exApiFeignClient;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public RestStopResponse getLocationInfoRest(int pageNo) {
        String pageNumber = String.valueOf(pageNo);
        String requestUrl = requestUrl(LOCATION_INFO_REST_PATH)
                .queryParam(NUM_OF_ROWS_PARAMETER, REST_STOP_NUM_OF_ROWS)
                .queryParam(PAGE_NO_PARAMETER, pageNumber)
                .build()
                .encode()
                .toUriString();

        return fetch(
                requestUrl,
                () -> exApiFeignClient.getLocationInfoRest(
                        apiKey, ExApiResponseFormat.JSON.value(), REST_STOP_NUM_OF_ROWS, pageNumber));
    }

    public RestStopDetailResponse getConvenienceServiceArea(int pageNo) {
        String pageNumber = String.valueOf(pageNo);
        String requestUrl = requestUrl(CONVENIENCE_SERVICE_AREA_PATH)
                .queryParam(NUM_OF_ROWS_PARAMETER, REST_STOP_NUM_OF_ROWS)
                .queryParam(PAGE_NO_PARAMETER, pageNumber)
                .build()
                .encode()
                .toUriString();

        return fetch(
                requestUrl,
                () -> exApiFeignClient.getConvenienceServiceArea(
                        apiKey, ExApiResponseFormat.JSON.value(), REST_STOP_NUM_OF_ROWS, pageNumber));
    }

    public HighwayServiceAreaInfoResponse getHighwayServiceAreaInfoList() {
        String requestUrl =
                requestUrl(HIGHWAY_SERVICE_AREA_INFO_PATH).build().encode().toUriString();

        return fetch(
                requestUrl,
                () -> exApiFeignClient.getHighwayServiceAreaInfoList(apiKey, ExApiResponseFormat.JSON.value()));
    }

    public RestOilResponse getRestOilList() {
        String requestUrl = requestUrl(REST_OIL_LIST_PATH).build().encode().toUriString();

        return fetch(requestUrl, () -> exApiFeignClient.getRestOilList(apiKey, ExApiResponseFormat.JSON.value()));
    }

    public RestOilPriceResponse getCurStateStation(int pageNo) {
        String pageNumber = String.valueOf(pageNo);
        String requestUrl = requestUrl(CUR_STATE_STATION_PATH)
                .queryParam(NUM_OF_ROWS_PARAMETER, REST_STOP_NUM_OF_ROWS)
                .queryParam(PAGE_NO_PARAMETER, pageNumber)
                .build()
                .encode()
                .toUriString();

        return fetch(
                requestUrl,
                () -> exApiFeignClient.getCurStateStation(
                        apiKey, ExApiResponseFormat.JSON.value(), REST_STOP_NUM_OF_ROWS, pageNumber));
    }

    public RestOilPriceResponse getCurStateStationByServiceAreaCode2(String serviceAreaCode2) {
        String pageNumber = "1";
        String requestUrl = requestUrl(CUR_STATE_STATION_PATH)
                .queryParam(NUM_OF_ROWS_PARAMETER, REST_STOP_NUM_OF_ROWS)
                .queryParam(PAGE_NO_PARAMETER, pageNumber)
                .queryParam(SERVICE_AREA_CODE2_PARAMETER, serviceAreaCode2)
                .build()
                .encode()
                .toUriString();

        return fetch(
                requestUrl,
                () -> exApiFeignClient.getCurStateStation(
                        apiKey, ExApiResponseFormat.JSON.value(), REST_STOP_NUM_OF_ROWS, pageNumber, serviceAreaCode2));
    }

    public RestBestfoodResponse getRestBestfoodList(int pageNo) {
        String pageNumber = String.valueOf(pageNo);
        String requestUrl = requestUrl(REST_BESTFOOD_LIST_PATH)
                .queryParam(NUM_OF_ROWS_PARAMETER, REST_STOP_NUM_OF_ROWS)
                .queryParam(PAGE_NO_PARAMETER, pageNumber)
                .build()
                .encode()
                .toUriString();

        return fetch(
                requestUrl,
                () -> exApiFeignClient.getRestBestfoodList(
                        apiKey, ExApiResponseFormat.JSON.value(), REST_STOP_NUM_OF_ROWS, pageNumber));
    }

    public RestThemeResponse getRestThemeList() {
        String requestUrl = requestUrl(REST_THEME_LIST_PATH).build().encode().toUriString();

        return fetch(requestUrl, () -> exApiFeignClient.getRestThemeList(apiKey, ExApiResponseFormat.JSON.value()));
    }

    public RestEventResponse getRestEventList() {
        String requestUrl = requestUrl(REST_EVENT_LIST_PATH).build().encode().toUriString();

        return fetch(requestUrl, () -> exApiFeignClient.getRestEventList(apiKey, ExApiResponseFormat.JSON.value()));
    }

    private UriComponentsBuilder requestUrl(String path) {
        return UriComponentsBuilder.fromUriString(apiUrl)
                .path(path)
                .queryParam(KEY_PARAMETER, apiKey)
                .queryParam(TYPE_PARAMETER, ExApiResponseFormat.JSON.value());
    }

    private <T extends ExApiResponse> T fetch(String requestUrl, Supplier<T> request) {
        String safeRequestUrl = ExternalApiRequestLog.sanitizeUrl(requestUrl);
        log.info("External API request started. api={}, requestUrl={}", API_NAME, safeRequestUrl);
        try {
            T response = request.get();
            if (response == null) {
                throw new ExApiException(requestUrl, "empty response");
            }

            if (!response.isSuccess()) {
                throw new ExApiException(requestUrl, response.getErrorMessage());
            }

            log.info("External API request succeeded. api={}, requestUrl={}", API_NAME, safeRequestUrl);
            return response;
        } catch (ExApiException e) {
            log.warn(
                    "External API request failed. api={}, requestUrl={}, message={}",
                    API_NAME,
                    safeRequestUrl,
                    e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.warn(
                    "External API request failed. api={}, requestUrl={}, message={}",
                    API_NAME,
                    safeRequestUrl,
                    e.getMessage());
            throw new ExApiException(requestUrl, e.getMessage(), e);
        }
    }
}

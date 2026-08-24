package com.restroute.evcharger.client;

import com.restroute.client.ExternalApiRequestLog;
import com.restroute.client.exception.ExApiException;
import com.restroute.evcharger.client.response.EvChargerResponse;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class EvChargerApiClient {

    private static final String API_NAME = "EV_CHARGER";

    private final EvChargerFeignClient evChargerFeignClient;
    private final String apiUrl;
    private final String apiKey;

    public EvChargerApiClient(
            EvChargerFeignClient evChargerFeignClient,
            @Value("${ev.api.url}") String apiUrl,
            @Value("${ev.api.key}") String apiKey) {
        this.evChargerFeignClient = evChargerFeignClient;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public EvChargerResponse getChargerInfo(int pageNo) {
        String requestUrl = UriComponentsBuilder.fromUriString(apiUrl)
                .path(EvChargerFeignClient.CHARGER_INFO_PATH)
                .queryParam(EvChargerFeignClient.SERVICE_KEY_PARAMETER, apiKey)
                .queryParam(EvChargerFeignClient.PAGE_NO_PARAMETER, pageNo)
                .queryParam(EvChargerFeignClient.NUM_OF_ROWS_PARAMETER, EvChargerFeignClient.CHARGER_NUM_OF_ROWS)
                .queryParam(EvChargerFeignClient.DATA_TYPE_PARAMETER, EvChargerFeignClient.JSON_DATA_TYPE)
                .queryParam(EvChargerFeignClient.KIND_PARAMETER, EvChargerFeignClient.REST_FACILITY_KIND)
                .build()
                .encode()
                .toUriString();

        return fetch(
                requestUrl,
                () -> evChargerFeignClient.getChargerInfo(
                        apiKey,
                        pageNo,
                        EvChargerFeignClient.CHARGER_NUM_OF_ROWS,
                        EvChargerFeignClient.JSON_DATA_TYPE,
                        EvChargerFeignClient.REST_FACILITY_KIND));
    }

    private <T extends com.restroute.client.response.ExApiResponse> T fetch(String requestUrl, Supplier<T> request) {
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
                    ExternalApiRequestLog.sanitizeUrl(e.getMessage()));
            throw e;
        } catch (RuntimeException e) {
            log.warn(
                    "External API request failed. api={}, requestUrl={}, message={}",
                    API_NAME,
                    safeRequestUrl,
                    ExternalApiRequestLog.sanitizeUrl(e.getMessage()));
            throw new ExApiException(requestUrl, e.getMessage(), e);
        }
    }
}

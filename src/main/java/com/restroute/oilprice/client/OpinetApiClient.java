package com.restroute.oilprice.client;

import static com.restroute.oilprice.client.OpinetFeignClient.AVERAGE_ALL_PRICE_PATH;
import static com.restroute.oilprice.client.OpinetFeignClient.CODE_PARAMETER;
import static com.restroute.oilprice.client.OpinetFeignClient.OUT_PARAMETER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.common.client.ExternalApiRequestLog;
import com.restroute.common.client.exception.ExApiException;
import com.restroute.oilprice.client.response.OpinetAverageOilPriceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class OpinetApiClient {

    private static final String FORMAT_JSON = "json";
    private static final String API_NAME = "OPINET";

    private final OpinetFeignClient opinetFeignClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;

    public OpinetApiClient(
            OpinetFeignClient opinetFeignClient,
            ObjectMapper objectMapper,
            @Value("${opinet.api.url}") String apiUrl,
            @Value("${opinet.api.key}") String apiKey) {
        this.opinetFeignClient = opinetFeignClient;
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public OpinetAverageOilPriceResponse getAverageOilPrices() {
        String requestUrl = UriComponentsBuilder.fromUriString(apiUrl)
                .path(AVERAGE_ALL_PRICE_PATH)
                .queryParam(OUT_PARAMETER, FORMAT_JSON)
                .queryParam(CODE_PARAMETER, apiKey)
                .build()
                .encode()
                .toUriString();

        String safeRequestUrl = ExternalApiRequestLog.sanitizeUrl(requestUrl);
        log.info("External API request started. api={}, requestUrl={}", API_NAME, safeRequestUrl);
        try {
            String responseBody = opinetFeignClient.getAverageOilPrices(FORMAT_JSON, apiKey);
            OpinetAverageOilPriceResponse response = parseResponse(requestUrl, responseBody);
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

    private OpinetAverageOilPriceResponse parseResponse(String requestUrl, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(responseBody, OpinetAverageOilPriceResponse.class);
        } catch (JsonProcessingException e) {
            throw new ExApiException(requestUrl, "invalid JSON response", e);
        }
    }
}

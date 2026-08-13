package com.restroute.flight.client;

import com.restroute.flight.client.exception.IncheonApiException;
import com.restroute.flight.client.response.IncheonAirlineItem;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IncheonClient {

    private static final String API_NAME = "INCHEON_AIRPORT";
    private static final String RESPONSE_TYPE_JSON = "json";
    private static final int MAX_ROWS = 500;

    private final IncheonFeignClient incheonFeignClient;
    private final String serviceKey;

    public IncheonClient(
            IncheonFeignClient incheonFeignClient, @Value("${incheon.api.service-key:}") String serviceKey) {
        this.incheonFeignClient = incheonFeignClient;
        this.serviceKey = serviceKey;
    }

    public List<IncheonAirlineItem> serviceAirlines() {
        return fetch(
                        "service airlines",
                        () -> incheonFeignClient.getServiceAirlineInfo(serviceKey, RESPONSE_TYPE_JSON, MAX_ROWS))
                .itemsOrEmpty();
    }

    private <T> T fetch(String requestDescription, Supplier<T> request) {
        log.info("External API request started. api={}, endpoint={}", API_NAME, requestDescription);
        try {
            T response = request.get();
            if (response == null) {
                throw new IncheonApiException(requestDescription, "빈 응답");
            }
            log.info("External API request succeeded. api={}, endpoint={}", API_NAME, requestDescription);
            return response;
        } catch (IncheonApiException e) {
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
            throw new IncheonApiException(requestDescription, e.getMessage(), e);
        }
    }
}

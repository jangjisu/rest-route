package com.restroute.flight.client;

import com.restroute.flight.client.exception.TravelpayoutsApiException;
import com.restroute.flight.client.response.TravelpayoutsCityItem;
import com.restroute.flight.client.response.TravelpayoutsGroupedPricesResponse;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TravelpayoutsClient {

    private static final String API_NAME = "TRAVELPAYOUTS";
    private static final String CURRENCY_KRW = "krw";

    private final TravelpayoutsFeignClient travelpayoutsFeignClient;
    private final String apiToken;

    public TravelpayoutsClient(
            TravelpayoutsFeignClient travelpayoutsFeignClient, @Value("${travelpayouts.api.token:}") String apiToken) {
        this.travelpayoutsFeignClient = travelpayoutsFeignClient;
        this.apiToken = apiToken;
    }

    public TravelpayoutsGroupedPricesResponse groupedPrices(
            String origin,
            String destination,
            String departureAtYearMonth,
            Integer minTripDuration,
            Integer maxTripDuration) {
        return fetch(
                "grouped prices",
                () -> travelpayoutsFeignClient.groupedPrices(
                        origin,
                        destination,
                        departureAtYearMonth,
                        minTripDuration,
                        maxTripDuration,
                        CURRENCY_KRW,
                        apiToken));
    }

    public List<TravelpayoutsCityItem> citiesData() {
        return fetch("cities data", travelpayoutsFeignClient::citiesData);
    }

    private <T> T fetch(String requestDescription, Supplier<T> request) {
        log.info("External API request started. api={}, endpoint={}", API_NAME, requestDescription);
        try {
            T response = request.get();
            if (response == null) {
                throw new TravelpayoutsApiException(requestDescription, "empty response");
            }
            log.info("External API request succeeded. api={}, endpoint={}", API_NAME, requestDescription);
            return response;
        } catch (TravelpayoutsApiException e) {
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
            throw new TravelpayoutsApiException(requestDescription, e.getMessage(), e);
        }
    }
}

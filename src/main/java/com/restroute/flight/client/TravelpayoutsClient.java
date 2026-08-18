package com.restroute.flight.client;

import com.restroute.flight.client.exception.TravelpayoutsApiException;
import com.restroute.flight.client.response.TravelpayoutsGroupedPricesResponse;
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

    /** RANGE 검색용 — 달 단위(departureAtYearMonth)로 그 달 전체를, nights는 min/max 범위로 좁혀서 받는다. */
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
                        null,
                        minTripDuration,
                        maxTripDuration,
                        CURRENCY_KRW,
                        apiToken));
    }

    /**
     * FIXED 검색용 — 출발일·귀국일을 정확한 날짜로 그대로 넘긴다. min/max_trip_duration으로
     * 박수 범위를 흉내 내지 않는다 — 그 날짜 조합이 실제 인벤토리에 없으면 빈 응답이 오는 게
     * 맞는 동작이다(억지로 넓혀서 다른 날짜 조합을 보여주지 않는다).
     */
    public TravelpayoutsGroupedPricesResponse groupedPricesForExactDates(
            String origin, String destination, String departureAt, String returnAt) {
        return fetch(
                "grouped prices (exact dates)",
                () -> travelpayoutsFeignClient.groupedPrices(
                        origin, destination, departureAt, returnAt, null, null, CURRENCY_KRW, apiToken));
    }

    private <T> T fetch(String requestDescription, Supplier<T> request) {
        log.info("External API request started. api={}, endpoint={}", API_NAME, requestDescription);
        try {
            T response = request.get();
            if (response == null) {
                throw new TravelpayoutsApiException(requestDescription, "빈 응답");
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

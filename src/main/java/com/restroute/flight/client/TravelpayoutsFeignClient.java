package com.restroute.flight.client;

import com.restroute.flight.client.response.TravelpayoutsGroupedPricesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Travelpayouts Data API (Aviasales) Feign Client (api.travelpayouts.com)
 */
@FeignClient(name = "travelpayouts", url = "${travelpayouts.api.url}", configuration = TravelpayoutsFeignConfig.class)
public interface TravelpayoutsFeignClient {

    String GROUPED_PRICES_PATH = "/aviasales/v3/grouped_prices";

    @GetMapping(GROUPED_PRICES_PATH)
    TravelpayoutsGroupedPricesResponse groupedPrices(
            @RequestParam("origin") String origin,
            @RequestParam(value = "destination", required = false) String destination,
            @RequestParam("departure_at") String departureAt,
            @RequestParam(value = "return_at", required = false) String returnAt,
            @RequestParam(value = "min_trip_duration", required = false) Integer minTripDuration,
            @RequestParam(value = "max_trip_duration", required = false) Integer maxTripDuration,
            @RequestParam("currency") String currency,
            @RequestParam("token") String token);
}

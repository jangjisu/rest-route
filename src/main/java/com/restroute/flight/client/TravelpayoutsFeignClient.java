package com.restroute.flight.client;

import com.restroute.flight.client.response.TravelpayoutsAirlineItem;
import com.restroute.flight.client.response.TravelpayoutsCityItem;
import com.restroute.flight.client.response.TravelpayoutsCountryItem;
import com.restroute.flight.client.response.TravelpayoutsGroupedPricesResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Travelpayouts Data API (Aviasales) Feign Client (api.travelpayouts.com)
 */
@FeignClient(name = "travelpayouts", url = "${travelpayouts.api.url}")
public interface TravelpayoutsFeignClient {

    String GROUPED_PRICES_PATH = "/aviasales/v3/grouped_prices";
    String CITIES_DATA_PATH = "/data/ko/cities.json";
    String COUNTRIES_DATA_PATH = "/data/ko/countries.json";
    String AIRLINES_DATA_PATH = "/data/airlines.json";

    @GetMapping(GROUPED_PRICES_PATH)
    TravelpayoutsGroupedPricesResponse groupedPrices(
            @RequestParam("origin") String origin,
            @RequestParam(value = "destination", required = false) String destination,
            @RequestParam("departure_at") String departureAt,
            @RequestParam(value = "min_trip_duration", required = false) Integer minTripDuration,
            @RequestParam(value = "max_trip_duration", required = false) Integer maxTripDuration,
            @RequestParam("currency") String currency,
            @RequestParam("token") String token);

    @GetMapping(CITIES_DATA_PATH)
    List<TravelpayoutsCityItem> citiesData();

    @GetMapping(COUNTRIES_DATA_PATH)
    List<TravelpayoutsCountryItem> countriesData();

    @GetMapping(AIRLINES_DATA_PATH)
    List<TravelpayoutsAirlineItem> airlinesData();
}

package com.restroute.flight.service;

import com.restroute.flight.client.TravelpayoutsClient;
import com.restroute.flight.client.response.TravelpayoutsCityItem;
import com.restroute.flight.domain.FlightCityEntity;
import com.restroute.flight.repository.FlightCityRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Travelpayouts cities.json 전체를 받아 취항 공항이 있는 도시만 flight_city 테이블에 동기화한다.
 * fetch에 실패하면 기존 데이터를 그대로 두고 다음 스케줄을 기다린다(테이블을 함부로 비우지 않음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlightCitySyncService {

    private final TravelpayoutsClient travelpayoutsClient;
    private final FlightCityRepository flightCityRepository;
    private final TransactionTemplate transactionTemplate;

    public int initializeFlightCitiesIfEmpty() {
        if (flightCityRepository.count() > 0) {
            return 0;
        }

        return refreshFlightCities();
    }

    public int refreshFlightCities() {
        List<TravelpayoutsCityItem> items;
        try {
            items = travelpayoutsClient.citiesData();
        } catch (RuntimeException e) {
            log.warn("Flight city data fetch failed, keeping existing data. cause={}", e.getMessage(), e);
            return 0;
        }

        List<FlightCityEntity> flightableCities = items.stream()
                .filter(TravelpayoutsCityItem::hasFlightableAirport)
                .map(item -> new FlightCityEntity(item.code(), item.name(), item.countryCode()))
                .toList();

        transactionTemplate.executeWithoutResult(status -> {
            flightCityRepository.deleteAllInBatch();
            flightCityRepository.saveAll(flightableCities);
        });

        return flightableCities.size();
    }
}

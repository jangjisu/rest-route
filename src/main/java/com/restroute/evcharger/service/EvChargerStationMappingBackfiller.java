package com.restroute.evcharger.service;

import com.restroute.domain.RestStopEntity;
import com.restroute.evcharger.domain.EvChargerStationMappingEntity;
import com.restroute.evcharger.repository.EvChargerRepository;
import com.restroute.evcharger.repository.EvChargerStationMappingRepository;
import com.restroute.evcharger.service.mapping.EvChargerStationMappingCalculator;
import com.restroute.repository.RestStopDetailRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvChargerStationMappingBackfiller {

    private final EvChargerStationMappingCalculator evChargerStationMappingCalculator;
    private final RestStopDetailRepository restStopDetailRepository;
    private final EvChargerRepository evChargerRepository;
    private final EvChargerStationMappingRepository evChargerStationMappingRepository;

    public int backfill(List<RestStopEntity> restStops) {
        List<EvChargerStationMappingEntity> mappingsToSave = evChargerStationMappingCalculator.calculate(
                restStops, restStopDetailRepository.findAll(), evChargerRepository.findAllByDelYn("N"));
        evChargerStationMappingRepository.deleteAllInBatch();
        evChargerStationMappingRepository.saveAll(mappingsToSave);
        return mappingsToSave.size();
    }
}

package com.restroute.service.backfill;

import com.restroute.domain.EvChargerStationMappingEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.EvChargerRepository;
import com.restroute.repository.EvChargerStationMappingRepository;
import com.restroute.repository.RestStopDetailRepository;
import com.restroute.service.evcharger.mapping.EvChargerStationMappingCalculator;
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

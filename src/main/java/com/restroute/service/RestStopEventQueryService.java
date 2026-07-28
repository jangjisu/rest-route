package com.restroute.service;

import com.restroute.controller.response.RestStopEventResponse;
import com.restroute.domain.RestEventEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestEventRepository;
import com.restroute.repository.RestStopRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RestStopEventQueryService {

    private static final DateTimeFormatter SOURCE_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestStopRepository restStopRepository;
    private final RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;
    private final RestEventRepository restEventRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Optional<RestStopEventResponse> findByServiceAreaCode(String serviceAreaCode) {
        return restStopRepository.findByServiceAreaCode(serviceAreaCode).map(this::findByRestStop);
    }

    @Transactional(readOnly = true)
    public List<String> findActiveEventMappedServiceAreaCodes(Collection<String> serviceAreaCodes) {
        List<String> validServiceAreaCodes = serviceAreaCodes.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (validServiceAreaCodes.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now(clock);
        return restEventRepository.findAllByRestStopServiceAreaCodeIn(validServiceAreaCodes).stream()
                .filter(event -> isActiveOn(event, today))
                .map(RestEventEntity::getRestStopServiceAreaCode)
                .distinct()
                .toList();
    }

    private RestStopEventResponse findByRestStop(RestStopEntity restStop) {
        RestStopRelatedInfo relatedInfo = restStopRelatedInfoQueryService.findByRestStop(restStop);
        LocalDate today = LocalDate.now(clock);
        List<RestEventEntity> activeEvents = relatedInfo.events().stream()
                .filter(event -> isActiveOn(event, today))
                .toList();
        return RestStopEventResponse.from(activeEvents);
    }

    private boolean isActiveOn(RestEventEntity event, LocalDate today) {
        try {
            LocalDate start = LocalDate.parse(event.getStime(), SOURCE_DATE_FORMAT);
            LocalDate end = LocalDate.parse(event.getEtime(), SOURCE_DATE_FORMAT);
            return !today.isBefore(start) && !today.isAfter(end);
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}

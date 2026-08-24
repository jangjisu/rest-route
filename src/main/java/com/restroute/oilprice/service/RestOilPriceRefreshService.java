package com.restroute.oilprice.service;

import com.restroute.client.ExApiClient;
import com.restroute.domain.RestStopEntity;
import com.restroute.oilprice.client.response.RestOilPriceItem;
import com.restroute.oilprice.client.response.RestOilPriceResponse;
import com.restroute.oilprice.controller.response.OilInfoResponse;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.repository.RestOilPriceRepository;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.RestStopRelatedInfoQueryService;
import com.restroute.service.dto.RestStopRelatedInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class RestOilPriceRefreshService {

    private static final Duration REFRESH_TTL = Duration.ofMinutes(10);

    private final RestStopRepository restStopRepository;
    private final RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;
    private final RestOilPriceRepository restOilPriceRepository;
    private final ExApiClient exApiClient;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public Optional<OilInfoResponse> refreshByServiceAreaCode(String serviceAreaCode) {
        return restStopRepository.findByServiceAreaCode(serviceAreaCode).flatMap(this::refreshByRestStop);
    }

    private Optional<OilInfoResponse> refreshByRestStop(RestStopEntity restStop) {
        RestStopRelatedInfo relatedInfo = restStopRelatedInfoQueryService.findByRestStop(restStop);
        List<RestOilEntity> conveniences = relatedInfo.oilStationConveniences();
        Optional<String> serviceAreaCode2 = relatedInfo.oilServiceAreaCode2();
        if (serviceAreaCode2.isEmpty()) {
            return Optional.empty();
        }

        Optional<RestOilPriceEntity> cachedOilPrice = relatedInfo.oilPrice();
        if (cachedOilPrice.filter(this::isFresh).isPresent()) {
            return Optional.of(OilInfoResponse.from(cachedOilPrice, conveniences));
        }

        Optional<RestOilPriceItem> fetchedItem = fetchOilPrice(serviceAreaCode2.get());
        if (fetchedItem.isEmpty()) {
            return Optional.empty();
        }

        RestOilPriceEntity oilPrice =
                upsertOilPrice(restStop.getServiceAreaCode(), serviceAreaCode2.get(), fetchedItem.get());
        return Optional.of(OilInfoResponse.from(Optional.of(oilPrice), conveniences));
    }

    private Optional<RestOilPriceItem> fetchOilPrice(String serviceAreaCode2) {
        RestOilPriceResponse response = exApiClient.getCurStateStationByServiceAreaCode2(serviceAreaCode2);
        if (response.getList() == null) {
            return Optional.empty();
        }

        return response.getList().stream().findFirst();
    }

    private RestOilPriceEntity upsertOilPrice(
            String restStopServiceAreaCode, String serviceAreaCode2, RestOilPriceItem item) {
        LocalDateTime refreshedAt = LocalDateTime.now(clock);
        return transactionTemplate.execute(status -> restOilPriceRepository
                .findByServiceAreaCode2(serviceAreaCode2)
                .map(entity -> updateOilPrice(entity, item, refreshedAt, restStopServiceAreaCode))
                .orElseGet(
                        () -> restOilPriceRepository.save(createOilPrice(item, refreshedAt, restStopServiceAreaCode))));
    }

    private RestOilPriceEntity updateOilPrice(
            RestOilPriceEntity entity,
            RestOilPriceItem item,
            LocalDateTime refreshedAt,
            String restStopServiceAreaCode) {
        entity.updateFrom(item, refreshedAt);
        entity.updateRestStopServiceAreaCode(restStopServiceAreaCode);
        return entity;
    }

    private RestOilPriceEntity createOilPrice(
            RestOilPriceItem item, LocalDateTime refreshedAt, String restStopServiceAreaCode) {
        RestOilPriceEntity oilPrice = RestOilPriceEntity.from(item, refreshedAt);
        oilPrice.updateRestStopServiceAreaCode(restStopServiceAreaCode);
        return oilPrice;
    }

    private boolean isFresh(RestOilPriceEntity oilPrice) {
        if (oilPrice.getLastRefreshedAt() == null) {
            return false;
        }

        return !oilPrice.getLastRefreshedAt().isBefore(LocalDateTime.now(clock).minus(REFRESH_TTL));
    }
}

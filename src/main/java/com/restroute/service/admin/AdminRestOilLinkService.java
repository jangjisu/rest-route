package com.restroute.service.admin;

import com.restroute.controller.response.AdminOilStationLinkResponse;
import com.restroute.controller.response.AdminOilStationSearchResponse;
import com.restroute.controller.response.AdminRestOilLinkSummaryResponse;
import com.restroute.domain.RestOilEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestOilPriceRepository;
import com.restroute.repository.RestOilRepository;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.image.RestStopNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminRestOilLinkService {

    private final RestStopRepository restStopRepository;
    private final RestOilRepository restOilRepository;
    private final RestOilPriceRepository restOilPriceRepository;

    @Transactional(readOnly = true)
    public List<AdminRestOilLinkSummaryResponse> findAll() {
        List<RestStopEntity> restStops = restStopRepository.findAll();
        Map<String, RestOilPriceEntity> oilPriceByServiceAreaCode = new HashMap<>();
        for (RestOilPriceEntity oilPrice : restOilPriceRepository.findAll()) {
            if (!StringUtils.hasText(oilPrice.getRestStopServiceAreaCode())) {
                continue;
            }
            oilPriceByServiceAreaCode.put(oilPrice.getRestStopServiceAreaCode(), oilPrice);
        }
        return restStops.stream()
                .map(restStop -> AdminRestOilLinkSummaryResponse.from(
                        restStop, oilPriceByServiceAreaCode.get(restStop.getServiceAreaCode())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminOilStationSearchResponse> search(String name) {
        if (!StringUtils.hasText(name)) {
            return List.of();
        }
        return restOilPriceRepository.findAllByServiceAreaNameContainingIgnoreCaseOrderByIdAsc(name).stream()
                .map(oilPrice -> AdminOilStationSearchResponse.from(oilPrice, linkedRestStopName(oilPrice)))
                .toList();
    }

    @Transactional
    public AdminOilStationLinkResponse link(Long oilPriceId, String serviceAreaCode) {
        RestOilPriceEntity oilPrice = requireOilPrice(oilPriceId);
        restStopRepository
                .findByServiceAreaCode(serviceAreaCode)
                .orElseThrow(() -> RestStopNotFoundException.forServiceAreaCode(serviceAreaCode));
        oilPrice.applyAdminLink(serviceAreaCode);
        cascadeToRestOil(oilPrice, oil -> oil.applyAdminLink(serviceAreaCode));
        return AdminOilStationLinkResponse.from(oilPrice, linkedRestStopName(oilPrice));
    }

    @Transactional
    public AdminOilStationLinkResponse unlink(Long oilPriceId) {
        RestOilPriceEntity oilPrice = requireOilPrice(oilPriceId);
        oilPrice.clearAdminLink();
        cascadeToRestOil(oilPrice, RestOilEntity::clearAdminLink);
        return AdminOilStationLinkResponse.from(oilPrice, linkedRestStopName(oilPrice));
    }

    @Transactional
    public AdminOilStationLinkResponse clearOverride(Long oilPriceId) {
        RestOilPriceEntity oilPrice = requireOilPrice(oilPriceId);
        oilPrice.releaseToAutoMatching();
        cascadeToRestOil(oilPrice, RestOilEntity::releaseToAutoMatching);
        return AdminOilStationLinkResponse.from(oilPrice, linkedRestStopName(oilPrice));
    }

    private void cascadeToRestOil(RestOilPriceEntity oilPrice, Consumer<RestOilEntity> action) {
        if (!StringUtils.hasText(oilPrice.getServiceAreaCode2())) {
            return;
        }
        restOilRepository
                .findAllByStandardRestCodeOrderByIdAsc(oilPrice.getServiceAreaCode2())
                .forEach(action);
    }

    private String linkedRestStopName(RestOilPriceEntity oilPrice) {
        if (!StringUtils.hasText(oilPrice.getRestStopServiceAreaCode())) {
            return null;
        }
        return restStopRepository
                .findByServiceAreaCode(oilPrice.getRestStopServiceAreaCode())
                .map(RestStopEntity::getUnitName)
                .orElse(null);
    }

    private RestOilPriceEntity requireOilPrice(Long oilPriceId) {
        return restOilPriceRepository
                .findById(oilPriceId)
                .orElseThrow(() -> RestOilNotFoundException.forId(oilPriceId));
    }
}

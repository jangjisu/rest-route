package com.restroute.oilprice.service.admin;

import com.restroute.domain.RestStopEntity;
import com.restroute.oilprice.controller.response.AdminOilStationLinkResponse;
import com.restroute.oilprice.controller.response.AdminOilStationSearchResponse;
import com.restroute.oilprice.controller.response.AdminRestOilLinkSummaryResponse;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.repository.RestOilPriceRepository;
import com.restroute.oilprice.repository.RestOilRepository;
import com.restroute.oilprice.service.admin.exception.RestOilNotFoundException;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.image.exception.RestStopNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
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
        Map<String, Boolean> overriddenByStandardRestCode = overriddenByStandardRestCode();
        Map<String, RestOilPriceEntity> oilPriceByServiceAreaCode = restOilPriceRepository.findAll().stream()
                .filter(oilPrice -> StringUtils.hasText(oilPrice.getRestStopServiceAreaCode()))
                .collect(Collectors.toMap(
                        RestOilPriceEntity::getRestStopServiceAreaCode,
                        Function.identity(),
                        (first, second) -> second));
        return restStops.stream()
                .map(restStop -> AdminRestOilLinkSummaryResponse.from(
                        restStop,
                        oilPriceByServiceAreaCode.get(restStop.getServiceAreaCode()),
                        isAdminOverridden(
                                oilPriceByServiceAreaCode.get(restStop.getServiceAreaCode()),
                                overriddenByStandardRestCode)))
                .toList();
    }

    private Map<String, Boolean> overriddenByStandardRestCode() {
        return restOilRepository.findAll().stream()
                .filter(oil -> StringUtils.hasText(oil.getStandardRestCode()))
                .collect(Collectors.toMap(
                        RestOilEntity::getStandardRestCode, RestOilEntity::isAdminOverridden, Boolean::logicalOr));
    }

    private boolean isAdminOverridden(RestOilPriceEntity oilPrice, Map<String, Boolean> overriddenByStandardRestCode) {
        return oilPrice != null && overriddenByStandardRestCode.getOrDefault(oilPrice.getServiceAreaCode2(), false);
    }

    @Transactional(readOnly = true)
    public List<AdminOilStationSearchResponse> search(String name, String routeName) {
        List<RestOilPriceEntity> matches = findMatches(name, routeName);
        Map<String, String> restStopNameByServiceAreaCode = linkedRestStopNames(matches);
        return matches.stream()
                .map(oilPrice -> AdminOilStationSearchResponse.from(
                        oilPrice, restStopNameByServiceAreaCode.get(oilPrice.getRestStopServiceAreaCode())))
                .toList();
    }

    private Map<String, String> linkedRestStopNames(List<RestOilPriceEntity> oilPrices) {
        List<String> serviceAreaCodes = oilPrices.stream()
                .map(RestOilPriceEntity::getRestStopServiceAreaCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (serviceAreaCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return restStopRepository.findAllByServiceAreaCodeIn(serviceAreaCodes).stream()
                .collect(Collectors.toMap(RestStopEntity::getServiceAreaCode, RestStopEntity::getUnitName));
    }

    private List<RestOilPriceEntity> findMatches(String name, String routeName) {
        boolean hasName = StringUtils.hasText(name);
        boolean hasRoute = StringUtils.hasText(routeName);
        if (hasName && hasRoute) {
            return restOilPriceRepository.findAllByRouteNameAndServiceAreaNameContainingIgnoreCaseOrderByIdAsc(
                    routeName, name);
        }
        if (hasRoute) {
            return restOilPriceRepository.findAllByRouteNameOrderByIdAsc(routeName);
        }
        if (hasName) {
            return restOilPriceRepository.findAllByServiceAreaNameContainingIgnoreCaseOrderByIdAsc(name);
        }
        return List.of();
    }

    @Transactional
    public AdminOilStationLinkResponse link(Long oilPriceId, String serviceAreaCode) {
        RestOilPriceEntity oilPrice = requireOilPrice(oilPriceId);
        restStopRepository
                .findByServiceAreaCode(serviceAreaCode)
                .orElseThrow(() -> RestStopNotFoundException.forServiceAreaCode(serviceAreaCode));
        oilPrice.updateRestStopServiceAreaCode(serviceAreaCode);
        List<RestOilEntity> oils = cascadeToRestOil(oilPrice, oil -> oil.applyAdminLink(serviceAreaCode));
        return AdminOilStationLinkResponse.from(oilPrice, linkedRestStopName(oilPrice), isAdminOverridden(oils));
    }

    @Transactional
    public AdminOilStationLinkResponse unlink(Long oilPriceId) {
        RestOilPriceEntity oilPrice = requireOilPrice(oilPriceId);
        oilPrice.updateRestStopServiceAreaCode(null);
        List<RestOilEntity> oils = cascadeToRestOil(oilPrice, RestOilEntity::clearAdminLink);
        return AdminOilStationLinkResponse.from(oilPrice, linkedRestStopName(oilPrice), isAdminOverridden(oils));
    }

    @Transactional
    public AdminOilStationLinkResponse clearOverride(Long oilPriceId) {
        RestOilPriceEntity oilPrice = requireOilPrice(oilPriceId);
        List<RestOilEntity> oils = cascadeToRestOil(oilPrice, RestOilEntity::releaseToAutoMatching);
        return AdminOilStationLinkResponse.from(oilPrice, linkedRestStopName(oilPrice), isAdminOverridden(oils));
    }

    private List<RestOilEntity> cascadeToRestOil(RestOilPriceEntity oilPrice, Consumer<RestOilEntity> action) {
        if (!StringUtils.hasText(oilPrice.getServiceAreaCode2())) {
            return List.of();
        }
        List<RestOilEntity> oils =
                restOilRepository.findAllByStandardRestCodeOrderByIdAsc(oilPrice.getServiceAreaCode2());
        oils.forEach(action);
        return oils;
    }

    private boolean isAdminOverridden(List<RestOilEntity> oils) {
        return oils.stream().anyMatch(RestOilEntity::isAdminOverridden);
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

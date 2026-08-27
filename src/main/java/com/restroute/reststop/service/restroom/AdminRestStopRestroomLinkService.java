package com.restroute.reststop.service.restroom;

import com.restroute.reststop.controller.response.AdminRestStopRestroomLinkSummaryResponse;
import com.restroute.reststop.controller.response.AdminRestroomLinkResponse;
import com.restroute.reststop.controller.response.AdminRestroomSearchResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopRestroomEntity;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.repository.RestStopRestroomRepository;
import com.restroute.reststop.service.image.exception.RestStopNotFoundException;
import com.restroute.reststop.service.restroom.exception.RestStopRestroomNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminRestStopRestroomLinkService {

    private final RestStopRepository restStopRepository;
    private final RestStopRestroomRepository restStopRestroomRepository;

    @Transactional(readOnly = true)
    public List<AdminRestStopRestroomLinkSummaryResponse> findAll() {
        List<RestStopEntity> restStops = restStopRepository.findAll();
        Map<String, RestStopRestroomEntity> restroomByServiceAreaCode = restStopRestroomRepository.findAll().stream()
                .filter(restroom -> StringUtils.hasText(restroom.getRestStopServiceAreaCode()))
                .collect(Collectors.toMap(
                        RestStopRestroomEntity::getRestStopServiceAreaCode,
                        Function.identity(),
                        (first, second) -> second));
        return restStops.stream()
                .map(restStop -> AdminRestStopRestroomLinkSummaryResponse.from(
                        restStop, restroomByServiceAreaCode.get(restStop.getServiceAreaCode())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminRestroomSearchResponse> search(String name, String routeName) {
        List<RestStopRestroomEntity> matches = findMatches(name, routeName);
        Map<String, String> restStopNameByServiceAreaCode = linkedRestStopNames(matches);
        return matches.stream()
                .map(restroom -> AdminRestroomSearchResponse.from(
                        restroom, restStopNameByServiceAreaCode.get(restroom.getRestStopServiceAreaCode())))
                .toList();
    }

    private Map<String, String> linkedRestStopNames(List<RestStopRestroomEntity> restrooms) {
        List<String> serviceAreaCodes = restrooms.stream()
                .map(RestStopRestroomEntity::getRestStopServiceAreaCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (serviceAreaCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return restStopRepository.findAllByServiceAreaCodeIn(serviceAreaCodes).stream()
                .collect(Collectors.toMap(RestStopEntity::getServiceAreaCode, RestStopEntity::getUnitName));
    }

    private List<RestStopRestroomEntity> findMatches(String name, String routeName) {
        boolean hasName = StringUtils.hasText(name);
        boolean hasRoute = StringUtils.hasText(routeName);
        if (hasName && hasRoute) {
            return restStopRestroomRepository.findAllByRouteNameAndSourceRestStopNameContainingIgnoreCaseOrderByIdAsc(
                    routeName, name);
        }
        if (hasRoute) {
            return restStopRestroomRepository.findAllByRouteNameOrderByIdAsc(routeName);
        }
        if (hasName) {
            return restStopRestroomRepository.findAllBySourceRestStopNameContainingIgnoreCaseOrderByIdAsc(name);
        }
        return List.of();
    }

    @Transactional
    public AdminRestroomLinkResponse link(Long restroomId, String serviceAreaCode) {
        RestStopRestroomEntity restroom = requireRestroom(restroomId);
        RestStopEntity restStop = restStopRepository
                .findByServiceAreaCode(serviceAreaCode)
                .orElseThrow(() -> RestStopNotFoundException.forServiceAreaCode(serviceAreaCode));
        restroom.updateRestStopServiceAreaCode(serviceAreaCode);
        return AdminRestroomLinkResponse.from(restroom, restStop.getUnitName());
    }

    @Transactional
    public AdminRestroomLinkResponse unlink(Long restroomId) {
        RestStopRestroomEntity restroom = requireRestroom(restroomId);
        restroom.updateRestStopServiceAreaCode("");
        return AdminRestroomLinkResponse.from(restroom, null);
    }

    private RestStopRestroomEntity requireRestroom(Long restroomId) {
        return restStopRestroomRepository
                .findById(restroomId)
                .orElseThrow(() -> RestStopRestroomNotFoundException.forId(restroomId));
    }
}

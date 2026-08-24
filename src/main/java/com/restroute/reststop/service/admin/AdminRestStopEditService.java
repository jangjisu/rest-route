package com.restroute.reststop.service.admin;

import com.restroute.reststop.controller.request.AdminRestStopUpdateRequest;
import com.restroute.reststop.controller.response.AdminRestStopEditableResponse;
import com.restroute.reststop.domain.RestStopDetailEntity;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.repository.RestStopDetailRepository;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.service.admin.exception.InvalidRestStopEditException;
import com.restroute.reststop.service.image.exception.RestStopNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRestStopEditService {

    private final RestStopRepository restStopRepository;
    private final RestStopDetailRepository restStopDetailRepository;

    @Transactional(readOnly = true)
    public Optional<AdminRestStopEditableResponse> findEditable(String serviceAreaCode) {
        return restStopRepository
                .findByServiceAreaCode(serviceAreaCode)
                .map(restStop -> AdminRestStopEditableResponse.of(
                        restStop,
                        restStopDetailRepository
                                .findByServiceAreaCode(serviceAreaCode)
                                .orElse(null)));
    }

    @Transactional
    public AdminRestStopEditableResponse update(String serviceAreaCode, AdminRestStopUpdateRequest request) {
        RestStopEntity restStop = findRestStopOrThrow(serviceAreaCode);
        RestStopDetailEntity detail = restStopDetailRepository
                .findByServiceAreaCode(serviceAreaCode)
                .orElseGet(() -> restStopDetailRepository.save(RestStopDetailEntity.createEmpty(serviceAreaCode)));

        validateCoordinate(request.xValue());
        validateCoordinate(request.yValue());

        restStop.applyAdminEdit(
                request.unitName(), request.routeNo(), request.routeName(), request.xValue(), request.yValue());
        detail.applyAdminEdit(
                request.telNo(),
                request.brand(),
                request.routeCode(),
                request.svarAddr(),
                request.convenience(),
                request.maintenanceYn(),
                request.truckSaYn());

        return AdminRestStopEditableResponse.of(restStop, detail);
    }

    @Transactional
    public AdminRestStopEditableResponse create(AdminRestStopUpdateRequest request) {
        validateCoordinate(request.xValue());
        validateCoordinate(request.yValue());

        RestStopEntity restStop = restStopRepository.save(RestStopEntity.createByAdmin(
                request.unitName(), request.routeNo(), request.routeName(), request.xValue(), request.yValue()));
        RestStopDetailEntity detail = RestStopDetailEntity.createEmpty(restStop.getServiceAreaCode());
        detail.applyAdminEdit(
                request.telNo(),
                request.brand(),
                request.routeCode(),
                request.svarAddr(),
                request.convenience(),
                request.maintenanceYn(),
                request.truckSaYn());
        restStopDetailRepository.save(detail);

        return AdminRestStopEditableResponse.of(restStop, detail);
    }

    @Transactional
    public AdminRestStopEditableResponse clearOverride(String serviceAreaCode) {
        RestStopEntity restStop = findRestStopOrThrow(serviceAreaCode);
        RestStopDetailEntity detail = findDetailOrThrow(serviceAreaCode);

        restStop.clearAdminOverride();
        detail.clearAdminOverride();

        return AdminRestStopEditableResponse.of(restStop, detail);
    }

    private RestStopEntity findRestStopOrThrow(String serviceAreaCode) {
        return restStopRepository
                .findByServiceAreaCode(serviceAreaCode)
                .orElseThrow(() -> RestStopNotFoundException.forServiceAreaCode(serviceAreaCode));
    }

    private RestStopDetailEntity findDetailOrThrow(String serviceAreaCode) {
        return restStopDetailRepository
                .findByServiceAreaCode(serviceAreaCode)
                .orElseThrow(() -> RestStopNotFoundException.forServiceAreaCode(serviceAreaCode));
    }

    private void validateCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw InvalidRestStopEditException.forInvalidCoordinate(value, e);
        }
    }
}

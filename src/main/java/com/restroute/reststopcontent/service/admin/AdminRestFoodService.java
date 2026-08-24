package com.restroute.reststopcontent.service.admin;

import com.restroute.controller.request.AdminRestFoodRequest;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestStopRepository;
import com.restroute.reststopcontent.controller.response.AdminRestFoodResponse;
import com.restroute.reststopcontent.domain.RestFoodEntity;
import com.restroute.reststopcontent.repository.RestFoodImageRepository;
import com.restroute.reststopcontent.repository.RestFoodRepository;
import com.restroute.reststopcontent.service.admin.exception.InvalidRestFoodEditException;
import com.restroute.reststopcontent.service.admin.exception.RestFoodNotFoundException;
import com.restroute.service.image.exception.RestStopNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRestFoodService {

    private final RestFoodRepository restFoodRepository;
    private final RestStopRepository restStopRepository;
    private final RestFoodImageRepository restFoodImageRepository;

    @Transactional(readOnly = true)
    public List<AdminRestFoodResponse> findByServiceAreaCode(String serviceAreaCode) {
        List<RestFoodEntity> foods = restFoodRepository.findAllByRestStopServiceAreaCodeOrderByIdAsc(serviceAreaCode);
        List<Long> ids = foods.stream()
                .map(RestFoodEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        Set<Long> withImage =
                ids.isEmpty() ? new HashSet<>() : new HashSet<>(restFoodImageRepository.findAllFoodIdsIn(ids));
        return foods.stream()
                .map(food -> AdminRestFoodResponse.from(food, withImage.contains(food.getId())))
                .toList();
    }

    @Transactional
    public AdminRestFoodResponse create(String serviceAreaCode, AdminRestFoodRequest request) {
        RestStopEntity restStop = restStopRepository
                .findByServiceAreaCode(serviceAreaCode)
                .orElseThrow(() -> RestStopNotFoundException.forServiceAreaCode(serviceAreaCode));

        RestFoodEntity created = RestFoodEntity.createByAdmin(
                serviceAreaCode,
                restStop.getStdRestCd(),
                request.foodName(),
                request.foodCost(),
                request.description());
        RestFoodEntity saved = restFoodRepository.save(created);
        return AdminRestFoodResponse.from(saved, hasImage(saved.getId()));
    }

    @Transactional
    public AdminRestFoodResponse update(String serviceAreaCode, Long foodId, AdminRestFoodRequest request) {
        RestFoodEntity entity = requireFood(serviceAreaCode, foodId);
        entity.applyAdminEdit(request.foodName(), request.foodCost(), request.description());
        return AdminRestFoodResponse.from(entity, hasImage(entity.getId()));
    }

    @Transactional
    public AdminRestFoodResponse clearOverride(String serviceAreaCode, Long foodId) {
        RestFoodEntity entity = requireFood(serviceAreaCode, foodId);
        entity.clearAdminOverride();
        return AdminRestFoodResponse.from(entity, hasImage(entity.getId()));
    }

    @Transactional
    public void delete(String serviceAreaCode, Long foodId) {
        RestFoodEntity entity = requireFood(serviceAreaCode, foodId);
        if (entity.isSyncedFood()) {
            throw InvalidRestFoodEditException.forSyncedFoodDeletion(foodId);
        }
        restFoodRepository.delete(entity);
    }

    private boolean hasImage(Long foodId) {
        return foodId != null && restFoodImageRepository.existsById(foodId);
    }

    private RestFoodEntity requireFood(String serviceAreaCode, Long foodId) {
        return restFoodRepository
                .findByIdAndRestStopServiceAreaCode(foodId, serviceAreaCode)
                .orElseThrow(() -> RestFoodNotFoundException.forId(foodId));
    }
}

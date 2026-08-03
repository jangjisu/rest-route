package com.restroute.service.admin;

import com.restroute.repository.RestFoodImageRepository;
import com.restroute.repository.RestFoodRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRestFoodImageQueryService {

    private final RestFoodRepository restFoodRepository;
    private final RestFoodImageRepository restFoodImageRepository;

    @Transactional(readOnly = true)
    public Optional<byte[]> findListImage(String serviceAreaCode, Long foodId) {
        requireFood(serviceAreaCode, foodId);
        return restFoodImageRepository.findListImageDataByFoodId(foodId);
    }

    private void requireFood(String serviceAreaCode, Long foodId) {
        if (restFoodRepository
                .findByIdAndRestStopServiceAreaCode(foodId, serviceAreaCode)
                .isEmpty()) {
            throw RestFoodNotFoundException.forId(foodId);
        }
    }
}

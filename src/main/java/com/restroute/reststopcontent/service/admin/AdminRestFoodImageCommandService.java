package com.restroute.reststopcontent.service.admin;

import com.restroute.reststop.service.image.dto.RestStopImageData;
import com.restroute.reststop.service.image.util.RestStopImageProcessor;
import com.restroute.reststopcontent.domain.RestFoodImageEntity;
import com.restroute.reststopcontent.repository.RestFoodImageRepository;
import com.restroute.reststopcontent.repository.RestFoodRepository;
import com.restroute.reststopcontent.service.admin.exception.RestFoodNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AdminRestFoodImageCommandService {

    private final RestFoodRepository restFoodRepository;
    private final RestFoodImageRepository restFoodImageRepository;
    private final RestStopImageProcessor processor;

    @Transactional
    public void save(String serviceAreaCode, Long foodId, MultipartFile file) {
        requireFood(serviceAreaCode, foodId);
        RestStopImageData imageData = processor.process(file);
        restFoodImageRepository.save(
                RestFoodImageEntity.of(foodId, imageData.detailImageData(), imageData.listImageData()));
    }

    @Transactional
    public void delete(String serviceAreaCode, Long foodId) {
        requireFood(serviceAreaCode, foodId);
        restFoodImageRepository.deleteById(foodId);
    }

    private void requireFood(String serviceAreaCode, Long foodId) {
        if (restFoodRepository
                .findByIdAndRestStopServiceAreaCode(foodId, serviceAreaCode)
                .isEmpty()) {
            throw RestFoodNotFoundException.forId(foodId);
        }
    }
}

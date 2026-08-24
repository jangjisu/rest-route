package com.restroute.reststop.service.image;

import com.restroute.reststop.domain.RestStopImageEntity;
import com.restroute.reststop.repository.RestStopImageRepository;
import com.restroute.reststop.repository.RestStopRepository;
import com.restroute.reststop.service.image.dto.RestStopImageData;
import com.restroute.reststop.service.image.exception.RestStopNotFoundException;
import com.restroute.reststop.service.image.util.RestStopImageProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RestStopImageCommandService {

    private final RestStopRepository restStopRepository;
    private final RestStopImageRepository restStopImageRepository;
    private final RestStopImageProcessor processor;

    @Transactional
    public void save(String serviceAreaCode, MultipartFile file) {
        requireRestStop(serviceAreaCode);
        RestStopImageData imageData = processor.process(file);
        restStopImageRepository.save(
                RestStopImageEntity.of(serviceAreaCode, imageData.detailImageData(), imageData.listImageData()));
    }

    @Transactional
    public void delete(String serviceAreaCode) {
        requireRestStop(serviceAreaCode);
        restStopImageRepository.deleteById(serviceAreaCode);
    }

    private void requireRestStop(String serviceAreaCode) {
        if (!restStopRepository.existsByServiceAreaCode(serviceAreaCode)) {
            throw RestStopNotFoundException.forServiceAreaCode(serviceAreaCode);
        }
    }
}

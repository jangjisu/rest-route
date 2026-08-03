package com.restroute.service.image;

import com.restroute.repository.RestStopImageRepository;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.image.exception.RestStopNotFoundException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestStopImageQueryService {

    private static final String DETAIL_IMAGE_URL_FORMAT = "/api/rest-stops/%s/images/detail";

    private final RestStopRepository restStopRepository;
    private final RestStopImageRepository restStopImageRepository;

    @Transactional(readOnly = true)
    public Optional<byte[]> findDetailImage(String serviceAreaCode) {
        requireRestStop(serviceAreaCode);
        return restStopImageRepository.findDetailImageDataByServiceAreaCode(serviceAreaCode);
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> findListImage(String serviceAreaCode) {
        requireRestStop(serviceAreaCode);
        return restStopImageRepository.findListImageDataByServiceAreaCode(serviceAreaCode);
    }

    @Transactional(readOnly = true)
    public String findDetailImageUrl(String serviceAreaCode) {
        if (!restStopImageRepository.existsById(serviceAreaCode)) {
            return null;
        }
        return DETAIL_IMAGE_URL_FORMAT.formatted(serviceAreaCode);
    }

    @Transactional(readOnly = true)
    public Set<String> findExistingServiceAreaCodes(Collection<String> serviceAreaCodes) {
        if (serviceAreaCodes.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(restStopImageRepository.findServiceAreaCodesIn(serviceAreaCodes));
    }

    private void requireRestStop(String serviceAreaCode) {
        if (!restStopRepository.existsByServiceAreaCode(serviceAreaCode)) {
            throw RestStopNotFoundException.forServiceAreaCode(serviceAreaCode);
        }
    }
}

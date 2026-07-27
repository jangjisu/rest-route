package com.restroute.service.compare;

import com.restroute.controller.response.RestStopCompareResponse;
import com.restroute.controller.response.RestStopCompareResponse.RestStopCompareResult;
import com.restroute.controller.response.RestStopCompareResponse.RestStopCompareSide;
import com.restroute.domain.HighwayServiceAreaInfoEntity;
import com.restroute.domain.RestOilPriceEntity;
import com.restroute.domain.RestStopDetailEntity;
import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestStopRepository;
import com.restroute.service.RestStopRelatedInfo;
import com.restroute.service.RestStopRelatedInfoQueryService;
import com.restroute.service.image.RestStopImageQueryService;
import com.restroute.service.image.RestStopNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RestStopCompareService {

    private static final String LIST_IMAGE_URL_FORMAT = "/api/rest-stops/%s/images/list";
    private static final String SIDE_A = "A";
    private static final String SIDE_B = "B";

    private final RestStopRepository restStopRepository;
    private final RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;
    private final RestStopImageQueryService restStopImageQueryService;

    @Transactional(readOnly = true)
    public RestStopCompareResponse compare(String serviceAreaCodeA, String serviceAreaCodeB) {
        if (serviceAreaCodeA.equals(serviceAreaCodeB)) {
            throw InvalidRestStopCompareException.forSameServiceAreaCode(serviceAreaCodeA);
        }

        RestStopEntity restStopA = findRestStop(serviceAreaCodeA);
        RestStopEntity restStopB = findRestStop(serviceAreaCodeB);
        Set<String> imageServiceAreaCodes =
                restStopImageQueryService.findExistingServiceAreaCodes(Set.of(serviceAreaCodeA, serviceAreaCodeB));

        RestStopCompareSide sideA = toSide(restStopA, imageServiceAreaCodes);
        RestStopCompareSide sideB = toSide(restStopB, imageServiceAreaCodes);

        return RestStopCompareResponse.of(sideA, sideB, computeResult(sideA, sideB));
    }

    private RestStopEntity findRestStop(String serviceAreaCode) {
        return restStopRepository
                .findByServiceAreaCode(serviceAreaCode)
                .orElseThrow(() -> RestStopNotFoundException.forServiceAreaCode(serviceAreaCode));
    }

    private RestStopCompareSide toSide(RestStopEntity restStop, Set<String> imageServiceAreaCodes) {
        RestStopRelatedInfo relatedInfo = restStopRelatedInfoQueryService.findByRestStop(restStop);
        Optional<RestOilPriceEntity> oilPrice = relatedInfo.oilPrice();
        String serviceAreaCode = restStop.getServiceAreaCode();
        return RestStopCompareSide.of(
                serviceAreaCode,
                restStop.getUnitName(),
                restStop.getRouteName(),
                listImageUrl(serviceAreaCode, imageServiceAreaCodes),
                oilPrice.map(RestOilPriceEntity::getGasolinePrice).orElse(null),
                oilPrice.map(RestOilPriceEntity::getDieselPrice).orElse(null),
                oilPrice.map(RestOilPriceEntity::getLpgPrice).orElse(null),
                totalParkingCount(relatedInfo.highwayServiceAreaInfos()),
                facilities(relatedInfo.detail()));
    }

    private String listImageUrl(String serviceAreaCode, Set<String> imageServiceAreaCodes) {
        if (!imageServiceAreaCodes.contains(serviceAreaCode)) {
            return null;
        }
        return LIST_IMAGE_URL_FORMAT.formatted(serviceAreaCode);
    }

    private Integer totalParkingCount(List<HighwayServiceAreaInfoEntity> infos) {
        int total = infos.stream()
                .mapToInt(info -> parseCount(info.getCompactCarParkingCount())
                        + parseCount(info.getFullSizeCarParkingCount())
                        + parseCount(info.getDisabledParkingCount()))
                .sum();
        return total == 0 ? null : total;
    }

    private List<String> facilities(Optional<RestStopDetailEntity> detail) {
        return detail.map(RestStopDetailEntity::getConvenience)
                .filter(StringUtils::hasText)
                .map(convenience -> Arrays.stream(convenience.split("\\|"))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList())
                .orElse(List.of());
    }

    private RestStopCompareResult computeResult(RestStopCompareSide sideA, RestStopCompareSide sideB) {
        String gasolineWinner = priceWinner(sideA.gasolinePrice(), sideB.gasolinePrice());
        String dieselWinner = priceWinner(sideA.dieselPrice(), sideB.dieselPrice());
        String lpgWinner = priceWinner(sideA.lpgPrice(), sideB.lpgPrice());
        String parkingWinner = parkingWinner(sideA.parkingCount(), sideB.parkingCount());
        String facilityWinner = facilityWinner(sideA.facilities(), sideB.facilities());
        String recommendedSide =
                recommendedSide(Arrays.asList(gasolineWinner, dieselWinner, lpgWinner, parkingWinner, facilityWinner));
        return RestStopCompareResult.of(
                gasolineWinner, dieselWinner, lpgWinner, parkingWinner, facilityWinner, recommendedSide);
    }

    private String priceWinner(String priceA, String priceB) {
        Optional<Integer> a = parsePrice(priceA);
        Optional<Integer> b = parsePrice(priceB);
        if (a.isEmpty() || b.isEmpty() || a.get().equals(b.get())) {
            return null;
        }
        return a.get() < b.get() ? SIDE_A : SIDE_B;
    }

    private String parkingWinner(Integer countA, Integer countB) {
        if (countA == null || countB == null || countA.equals(countB)) {
            return null;
        }
        return countA > countB ? SIDE_A : SIDE_B;
    }

    private String facilityWinner(List<String> facilitiesA, List<String> facilitiesB) {
        if (facilitiesA.size() == facilitiesB.size()) {
            return null;
        }
        return facilitiesA.size() > facilitiesB.size() ? SIDE_A : SIDE_B;
    }

    private String recommendedSide(List<String> winners) {
        long aWins = winners.stream().filter(SIDE_A::equals).count();
        long bWins = winners.stream().filter(SIDE_B::equals).count();
        if (aWins == bWins) {
            return null;
        }
        return aWins > bWins ? SIDE_A : SIDE_B;
    }

    private Optional<Integer> parsePrice(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(digits));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private int parseCount(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

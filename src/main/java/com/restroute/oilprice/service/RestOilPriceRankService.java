package com.restroute.oilprice.service;

import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.oilprice.repository.RestOilPriceRepository;
import com.restroute.oilprice.service.dto.NationalCheapestOilPrice;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * "제일 저렴" 배지 판정에 쓰는 전국 최저가를 구한다. 유종별 diff-from-average(전국 평균 대비)와는
 * 달리, 우리가 실제로 추적 중인 휴게소 주유소 가격들 사이에서의 최저가다.
 */
@Service
@RequiredArgsConstructor
public class RestOilPriceRankService {

    private final RestOilPriceRepository restOilPriceRepository;

    public NationalCheapestOilPrice findNationalCheapestPrices() {
        List<RestOilPriceEntity> all = restOilPriceRepository.findAll();
        return NationalCheapestOilPrice.of(
                minPrice(all, RestOilPriceEntity::getGasolinePrice),
                minPrice(all, RestOilPriceEntity::getDieselPrice),
                minPrice(all, RestOilPriceEntity::getLpgPrice));
    }

    private Integer minPrice(List<RestOilPriceEntity> all, Function<RestOilPriceEntity, String> priceGetter) {
        return all.stream()
                .map(priceGetter)
                .map(this::parsePrice)
                .filter(price -> price != null)
                .min(Integer::compareTo)
                .orElse(null);
    }

    private Integer parsePrice(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (!StringUtils.hasText(digits)) {
            return null;
        }
        try {
            return Integer.valueOf(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

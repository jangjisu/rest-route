package com.restroute.reststop.service.dto;

import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import java.util.List;
import java.util.Optional;

/**
 * 주유 편의시설·가격 조회는 셋이 서로 의존한다(가격을 조회하려면 편의시설에서 얻은
 * oilServiceAreaCode2가 있어야 한다) 묶어서 하나로 반환한다.
 */
public record RestStopOilInfo(
        List<RestOilEntity> oilStationConveniences,
        Optional<String> oilServiceAreaCode2,
        Optional<RestOilPriceEntity> oilPrice) {

    public static RestStopOilInfo of(
            List<RestOilEntity> oilStationConveniences,
            Optional<String> oilServiceAreaCode2,
            Optional<RestOilPriceEntity> oilPrice) {
        return new RestStopOilInfo(oilStationConveniences, oilServiceAreaCode2, oilPrice);
    }
}

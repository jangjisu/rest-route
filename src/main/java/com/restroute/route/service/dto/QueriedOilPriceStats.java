package com.restroute.route.service.dto;

import com.restroute.route.dto.FuelType;

/**
 * 이번 요청에서 실제로 조회된(경로에 매칭된) 휴게소들만 대상으로 한 유종별 최저가. "제일 저렴" 배지는
 * 전국 전체 추적 데이터가 아니라 지금 사용자가 보고 있는 목록 안에서의 최저가를 기준으로 한다.
 * "평균보다 저렴"은 이 최저가와 별개로, DB에 있는 오늘자 전국 평균(NationalOilPriceSummary)과 비교한다.
 */
public record QueriedOilPriceStats(Integer gasolineMin, Integer dieselMin, Integer lpgMin) {

    public static QueriedOilPriceStats empty() {
        return new QueriedOilPriceStats(null, null, null);
    }

    public Integer minByFuelType(FuelType fuelType) {
        return switch (fuelType) {
            case GASOLINE -> gasolineMin;
            case DIESEL -> dieselMin;
            case LPG -> lpgMin;
            case EV -> null;
        };
    }
}

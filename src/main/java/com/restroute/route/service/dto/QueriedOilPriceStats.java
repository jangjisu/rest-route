package com.restroute.route.service.dto;

/**
 * 이번 요청에서 실제로 조회된(경로에 매칭된) 휴게소들만 대상으로 한 유종별 최저가/평균가.
 * 전국 전체 추적 데이터가 아니라 지금 사용자가 보고 있는 목록 안에서의 순위라서, "제일 저렴"/"평균보다
 * 저렴" 배지가 이 목록 밖의 휴게소와는 무관하다.
 */
public record QueriedOilPriceStats(
        Integer gasolineMin,
        Integer gasolineAverage,
        Integer dieselMin,
        Integer dieselAverage,
        Integer lpgMin,
        Integer lpgAverage) {

    public static QueriedOilPriceStats empty() {
        return new QueriedOilPriceStats(null, null, null, null, null, null);
    }
}

package com.restroute.flight.controller.dto;

/**
 * 검색 결과 정렬 기준. 지정하지 않으면 {@link #PRICE}(최저가순)가 기본값이다.
 */
public enum FlightDealSort {
    PRICE,
    DATE
}

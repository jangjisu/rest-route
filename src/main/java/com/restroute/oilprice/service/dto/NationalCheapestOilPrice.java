package com.restroute.oilprice.service.dto;

/**
 * 저장된 전체 rest_oil_price 중 유종별 최저가. 값을 숫자로 해석할 수 있는 행이 하나도 없으면
 * 그 유종은 null.
 */
public record NationalCheapestOilPrice(Integer gasoline, Integer diesel, Integer lpg) {

    public static NationalCheapestOilPrice of(Integer gasoline, Integer diesel, Integer lpg) {
        return new NationalCheapestOilPrice(gasoline, diesel, lpg);
    }
}

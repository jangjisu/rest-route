package com.restroute.oilprice.dto;

/**
 * 요청에 실린 유종 선택 — 선택 안 함(null)/EV/가격 있는 유종(GASOLINE·DIESEL·LPG) 세 가지 의미를
 * 하나로 감싼다. 호출부가 fuelType == null이나 == FuelType.EV를 직접 판정하지 않고 이 타입에
 * wantsFuelPriceInfo()/wantsEvChargerInfo()로 물어보게 한다.
 */
public record FuelTypeSelection(FuelType fuelType) {

    public static final FuelTypeSelection NONE = new FuelTypeSelection(null);

    public static FuelTypeSelection of(FuelType fuelType) {
        return fuelType == null ? NONE : new FuelTypeSelection(fuelType);
    }

    public boolean wantsFuelPriceInfo() {
        return fuelType != null && fuelType.havePriceInfo();
    }

    public boolean wantsEvChargerInfo() {
        return fuelType == FuelType.EV;
    }
}

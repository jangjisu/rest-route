package com.restroute.oilprice.dto;

public record NationalOilPriceSummary(
        String tradeDate, AverageOilPrice gasoline, AverageOilPrice diesel, AverageOilPrice lpg) {

    public static NationalOilPriceSummary of(
            String tradeDate, AverageOilPrice gasoline, AverageOilPrice diesel, AverageOilPrice lpg) {
        return new NationalOilPriceSummary(tradeDate, gasoline, diesel, lpg);
    }

    public String getAveragePriceByFuelType(FuelType fuelType) {
        return switch (fuelType) {
            case GASOLINE -> gasoline.price();
            case DIESEL -> diesel.price();
            case LPG -> lpg.price();
            case EV -> null;
        };
    }
}

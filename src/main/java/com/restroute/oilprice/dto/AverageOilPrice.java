package com.restroute.oilprice.dto;

public record AverageOilPrice(String productCode, String productName, String price, String dailyDiff) {

    public static AverageOilPrice of(String productCode, String productName, String price, String dailyDiff) {
        return new AverageOilPrice(productCode, productName, price, dailyDiff);
    }
}

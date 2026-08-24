package com.restroute.oilprice.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OpinetAverageOilPriceItem {

    @JsonProperty("TRADE_DT")
    private String tradeDate;

    @JsonProperty("PRODCD")
    private String productCode;

    @JsonProperty("PRODNM")
    private String productName;

    @JsonProperty("PRICE")
    private String price;

    @JsonProperty("DIFF")
    private String diff;
}

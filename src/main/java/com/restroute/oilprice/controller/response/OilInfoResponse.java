package com.restroute.oilprice.controller.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.domain.RestOilPriceEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record OilInfoResponse(
        String oilCompany,
        String gasolinePrice,
        String dieselPrice,
        String lpgPrice,
        String telNo,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime lastRefreshedAt,
        List<OilStationConvenienceResponse> oilStationConveniences) {

    public static OilInfoResponse from(
            Optional<RestOilPriceEntity> oilPrice, List<RestOilEntity> oilStationConveniences) {
        return new OilInfoResponse(
                oilPrice.map(RestOilPriceEntity::getOilCompany).orElse(null),
                oilPrice.map(RestOilPriceEntity::getGasolinePrice).orElse(null),
                oilPrice.map(RestOilPriceEntity::getDieselPrice).orElse(null),
                oilPrice.map(RestOilPriceEntity::getLpgPrice).orElse(null),
                oilPrice.map(RestOilPriceEntity::getTelNo).orElse(null),
                oilPrice.map(RestOilPriceEntity::getLastRefreshedAt).orElse(null),
                oilStationConveniences.stream()
                        .map(OilStationConvenienceResponse::from)
                        .toList());
    }
}

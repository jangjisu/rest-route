package com.restroute.route.service;

import com.restroute.oilprice.domain.RestOilPriceEntity;
import com.restroute.reststop.service.dto.RestStopAggregate;
import com.restroute.route.service.dto.QueriedOilPriceStats;
import com.restroute.route.service.util.RouteRestStopNumberParser;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * 이번 요청에 매칭된 휴게소들만 대상으로 유종별 최저가/평균가를 계산한다. 요청당 한 번만 호출한다.
 */
@Component
public class QueriedOilPriceStatsCalculator {

    public QueriedOilPriceStats calculate(Collection<RestStopAggregate> aggregates) {
        List<RestOilPriceEntity> oilPrices = aggregates.stream()
                .map(aggregate -> aggregate.relatedInfo().oilPrice())
                .flatMap(Optional::stream)
                .toList();
        if (oilPrices.isEmpty()) {
            return QueriedOilPriceStats.empty();
        }

        return new QueriedOilPriceStats(
                min(oilPrices, RestOilPriceEntity::getGasolinePrice),
                average(oilPrices, RestOilPriceEntity::getGasolinePrice),
                min(oilPrices, RestOilPriceEntity::getDieselPrice),
                average(oilPrices, RestOilPriceEntity::getDieselPrice),
                min(oilPrices, RestOilPriceEntity::getLpgPrice),
                average(oilPrices, RestOilPriceEntity::getLpgPrice));
    }

    private Integer min(List<RestOilPriceEntity> oilPrices, Function<RestOilPriceEntity, String> priceGetter) {
        return parsedPrices(oilPrices, priceGetter).min(Integer::compareTo).orElse(null);
    }

    private Integer average(List<RestOilPriceEntity> oilPrices, Function<RestOilPriceEntity, String> priceGetter) {
        List<Integer> prices = parsedPrices(oilPrices, priceGetter).toList();
        if (prices.isEmpty()) {
            return null;
        }
        return (int)
                Math.round(prices.stream().mapToInt(Integer::intValue).average().orElseThrow());
    }

    private Stream<Integer> parsedPrices(
            List<RestOilPriceEntity> oilPrices, Function<RestOilPriceEntity, String> priceGetter) {
        return oilPrices.stream()
                .map(priceGetter)
                .map(RouteRestStopNumberParser::parsePrice)
                .flatMap(Optional::stream);
    }
}

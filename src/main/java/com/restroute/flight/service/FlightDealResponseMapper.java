package com.restroute.flight.service;

import com.restroute.flight.cache.FlightAirlineNameCache;
import com.restroute.flight.cache.FlightAirportNameCache;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.service.util.FlightDealResponses;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * {@link TravelpayoutsPriceItem}(실 연동 원본, RANGE/FIXED 공통)을 {@link FlightDealResponse}(응답
 * 계약)로 바꾼다.
 *
 * <p>{@code id}와 {@code holidays}는 여기서 채우지 않는다 — id는 세션 토큰을 알아야 하고,
 * holidays는 어떤 딜이 최종 응답에 남는지 알아야 해서 둘 다 뒤 단계의 몫이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class FlightDealResponseMapper {

    private static final String PENDING_ID = "";

    private final FlightAirportNameCache airportNameCache;
    private final FlightAirlineNameCache airlineNameCache;

    List<FlightDealResponse> mapAll(List<TravelpayoutsPriceItem> items) {
        return items.stream()
                .filter(FlightDealResponseMapper::hasDepartureAndReturnDates)
                .map(this::mapOne)
                .toList();
    }

    /** Travelpayouts가 간혹 departure_at/return_at 없이 항목을 내려줄 때가 있다 — 그런 항목은 조용히 뺀다. */
    private static boolean hasDepartureAndReturnDates(TravelpayoutsPriceItem item) {
        boolean hasDates = StringUtils.hasText(item.departureAt()) && StringUtils.hasText(item.returnAt());
        if (!hasDates) {
            log.warn(
                    "Travelpayouts item missing departure_at/return_at, dropping. destinationAirport={}",
                    item.destinationAirport());
        }
        return hasDates;
    }

    /**
     * 목적지 이름은 도시코드가 아니라 공항코드({@code destinationAirport}) 기준으로 찾는다 — 응답
     * 계약이 공항코드로 확정돼 있다. {@code id}는 {@link FlightDealSessionStore}가, {@code holidays}는
     * {@link FlightDealPostFilter} 이후 {@link FlightDealHolidayEnricher}가 채운다.
     */
    private FlightDealResponse mapOne(TravelpayoutsPriceItem item) {
        OffsetDateTime departureAt = OffsetDateTime.parse(item.departureAt());
        OffsetDateTime returnAt = OffsetDateTime.parse(item.returnAt());
        int nights = (int) ChronoUnit.DAYS.between(departureAt.toLocalDate(), returnAt.toLocalDate());

        return new FlightDealResponse(
                PENDING_ID,
                new FlightDealResponse.Destination(
                        item.destinationAirport(), airportNameCache.findName(item.destinationAirport())),
                legOf(departureAt, item.durationTo(), item.transfers()),
                legOf(returnAt, item.durationBack(), item.returnTransfers()),
                nights,
                FlightDealResponses.NO_HOLIDAYS,
                new FlightDealResponse.Airline(
                        item.airline(),
                        airlineNameCache.findName(item.airline()),
                        airlineNameCache.isLowCost(item.airline())),
                new FlightDealResponse.Price(item.price(), "KRW"),
                false,
                item.gate(),
                item.link(),
                null);
    }

    private static FlightDealResponse.Leg legOf(OffsetDateTime departAt, int durationMinutes, int transferCount) {
        return FlightDealResponses.legOf(
                departAt, departAt.plusMinutes(durationMinutes), durationMinutes, transferCount);
    }
}

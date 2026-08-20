package com.restroute.flight.service;

import com.restroute.flight.cache.FlightAirlineNameCache;
import com.restroute.flight.cache.FlightAirportNameCache;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.service.util.FlightDealResponses;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * {@link TravelpayoutsPriceItem}(실 연동 원본)을 {@link FlightDealResponse}(응답 계약)로 바꾼다.
 *
 * <p>목적지는 도시코드가 아니라 공항코드({@code destinationAirport})를 기준으로 이름을 채운다 —
 * mock이 이미 공항코드 스타일(FUK/KIX/OKA 등)을 써왔던 것과 응답 계약을 맞추기 위해서다.
 *
 * <p>{@code isLowCost}는 {@link FlightAirlineNameCache#isLowCost}로 채운다 — Travelpayouts
 * {@code /data/airlines.json}의 {@code is_lowcost}를 시딩 단계에서 그대로 가져온 값이다.
 *
 * <p>{@code holidays}는 여기서는 항상 빈 목록이다 — 필터({@link FlightDealPostFilter}) 이후
 * {@link FlightDealHolidayEnricher}가 실제 값을 채운다(필터를 다 거친 뒤라야 어떤 딜이 최종
 * 응답에 남는지 알 수 있어서, 굳이 필터 전인 여기서 미리 계산할 이유가 없다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class FlightRangeSearchResponseMapper {

    private final FlightAirportNameCache airportNameCache;
    private final FlightAirlineNameCache airlineNameCache;

    /**
     * id는 세션 토큰 + 순번으로 매긴다(mock과 동일한 형식, {@link FlightDealResponses#idOf}
     * 공유). 전체 최저가 표시({@link FlightDealResponses#markLowestInRange})는 여기서 하지
     * 않는다 — 이후 필터(주말/공휴일 제외 등)를 거치면서 최저가였던 항목이 빠질 수 있어, 필터까지
     * 다 적용한 다음에 표시해야 한다.
     */
    List<FlightDealResponse> mapAll(List<TravelpayoutsPriceItem> items, String sessionToken) {
        List<TravelpayoutsPriceItem> withDates = items.stream()
                .filter(FlightRangeSearchResponseMapper::hasDepartureAndReturnDates)
                .toList();
        return IntStream.range(0, withDates.size())
                .mapToObj(index -> mapOne(withDates.get(index), FlightDealResponses.idOf(sessionToken, index)))
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

    private FlightDealResponse mapOne(TravelpayoutsPriceItem item, String id) {
        OffsetDateTime departureAt = OffsetDateTime.parse(item.departureAt());
        OffsetDateTime returnAt = OffsetDateTime.parse(item.returnAt());
        int nights = (int) ChronoUnit.DAYS.between(departureAt.toLocalDate(), returnAt.toLocalDate());

        return new FlightDealResponse(
                id,
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

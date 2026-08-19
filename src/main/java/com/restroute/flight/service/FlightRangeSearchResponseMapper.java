package com.restroute.flight.service;

import com.restroute.flight.cache.FlightAirlineNameCache;
import com.restroute.flight.cache.FlightAirportNameCache;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.response.FlightDealResponse;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link TravelpayoutsPriceItem}(실 연동 원본)을 {@link FlightDealResponse}(응답 계약)로 바꾼다.
 *
 * <p>목적지는 도시코드가 아니라 공항코드({@code destinationAirport})를 기준으로 이름을 채운다 —
 * mock이 이미 공항코드 스타일(FUK/KIX/OKA 등)을 써왔던 것과 응답 계약을 맞추기 위해서다.
 *
 * <p>{@code isLowCost}는 {@link FlightAirlineNameCache#isLowCost}로 채운다 — Travelpayouts
 * {@code /data/airlines.json}의 {@code is_lowcost}를 시딩 단계에서 그대로 가져온 값이다.
 *
 * <p>{@code holiday}는 mock과 동일하게 항상 0/빈 값 스텁이다 — 필터 단계({@link
 * FlightDealPostFilter})가 공휴일 여부를 실제로 조회하긴 하지만 그건 딜을 뺄지 말지 판단하는
 * 용도일 뿐, 그 결과를 여기로 다시 채워 넣는 별도 보강 단계는 아직 없다.
 */
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
        return IntStream.range(0, items.size())
                .mapToObj(index -> mapOne(items.get(index), FlightDealResponses.idOf(sessionToken, index)))
                .toList();
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
                FlightDealResponses.NO_HOLIDAY,
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

package com.restroute.flight.service;

import com.restroute.flight.cache.FlightAirlineNameCache;
import com.restroute.flight.cache.FlightAirportNameCache;
import com.restroute.flight.client.response.TravelpayoutsPriceItem;
import com.restroute.flight.controller.response.FlightDealResponse;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
 * <p>{@code isLowCost}는 지금 참조 데이터에 저비용항공사 여부 컬럼이 없어 항상 {@code false}로
 * 채운다 — 항공사 동기화 파이프라인에 그 값을 추가하는 건 이 클래스의 책임 밖이다.
 *
 * <p>{@code holiday}는 mock과 동일하게 항상 0/빈 값 스텁이다 — 실제 공휴일 배지 계산은 별도
 * 후처리 단계(포함 필터와 같은 공휴일 도메인 작업)에서 채운다.
 */
@Component
@RequiredArgsConstructor
class FlightRangeSearchResponseMapper {

    private static final DateTimeFormatter LEG_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /** 실제 공휴일 배지 계산 전까지는 mock과 동일하게 항상 0/빈 값이다. */
    private static final FlightDealResponse.Holiday NO_HOLIDAY = new FlightDealResponse.Holiday(0, List.of(), 0);

    private final FlightAirportNameCache airportNameCache;
    private final FlightAirlineNameCache airlineNameCache;

    /**
     * id는 세션 토큰 + 순번으로 매긴다(mock과 동일한 형식). 전체 최저가 표시({@link
     * FlightDealResponses#markLowestInRange})는 여기서 하지 않는다 — 이후 필터(주말/공휴일
     * 제외 등)를 거치면서 최저가였던 항목이 빠질 수 있어, 필터까지 다 적용한 다음에 표시해야
     * 한다.
     */
    List<FlightDealResponse> mapAll(List<TravelpayoutsPriceItem> items, String sessionToken) {
        return IntStream.range(0, items.size())
                .mapToObj(index -> mapOne(items.get(index), idOf(sessionToken, index)))
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
                NO_HOLIDAY,
                new FlightDealResponse.Airline(item.airline(), airlineNameCache.findName(item.airline()), false),
                new FlightDealResponse.Price(item.price(), "KRW"),
                false,
                item.gate(),
                item.link(),
                null);
    }

    private static FlightDealResponse.Leg legOf(OffsetDateTime departAt, int durationMinutes, int transferCount) {
        OffsetDateTime arriveAt = departAt.plusMinutes(durationMinutes);
        return new FlightDealResponse.Leg(
                LEG_TIME_FORMAT.format(departAt), LEG_TIME_FORMAT.format(arriveAt), durationMinutes, transferCount);
    }

    private static String idOf(String sessionToken, int index) {
        return "%s_%04d".formatted(sessionToken, index + 1);
    }
}

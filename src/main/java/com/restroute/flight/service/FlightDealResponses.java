package com.restroute.flight.service;

import com.restroute.flight.controller.response.FlightDealResponse;
import java.util.List;
import java.util.stream.IntStream;

/** mock/실 연동 양쪽에서 공통으로 쓰는 {@link FlightDealResponse} 목록 후처리. */
final class FlightDealResponses {

    private FlightDealResponses() {}

    /** 가격 기준 전체 최저가 한 건에만 isLowestInRange를 true로 표시한다(동가면 첫 항목 하나만). */
    static List<FlightDealResponse> markLowestInRange(List<FlightDealResponse> items) {
        if (items.isEmpty()) {
            return items;
        }
        int lowestIndex = 0;
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).price().amount() < items.get(lowestIndex).price().amount()) {
                lowestIndex = i;
            }
        }
        int finalLowestIndex = lowestIndex;
        return IntStream.range(0, items.size())
                .mapToObj(i -> i == finalLowestIndex ? withLowestInRange(items.get(i)) : items.get(i))
                .toList();
    }

    private static FlightDealResponse withLowestInRange(FlightDealResponse deal) {
        return new FlightDealResponse(
                deal.id(),
                deal.destination(),
                deal.departure(),
                deal.arrival(),
                deal.nights(),
                deal.holiday(),
                deal.airline(),
                deal.price(),
                true,
                deal.gateName(),
                deal.bookingLink(),
                deal.seatsLeft());
    }
}

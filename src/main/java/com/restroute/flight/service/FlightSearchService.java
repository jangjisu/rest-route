package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 항공권 실 연동 검색 진입점. 무한스크롤을 위해 첫 조회 결과를 세션에 cursor로 저장해두고,
 * 이후 요청은 그 세션을 이어서 페이지만 잘라 준다({@link FlightDealSessionStore}). 실제 딜을
 * 어떻게 구해오는지는 {@link FlightDealFetcher}에 위임한다.
 */
@Primary
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class FlightSearchService {

    private final FlightDealSessionStore sessionStore;
    private final FlightDealFetcher dealFetcher;

    /** {@link FlightDealFetcher}만 바꿔 같은 검색 흐름을 재사용할 때 쓴다. */
    static FlightSearchService create(FlightDealSessionStore sessionStore, FlightDealFetcher dealFetcher) {
        return new FlightSearchService(sessionStore, dealFetcher);
    }

    public FlightDealSearchResponse search(FlightSearchRequestDto request) {
        if (request.isFirstRequest()) {
            return sessionStore.create(request, request.boundedLimit(), () -> dealFetcher.fetch(request));
        }
        return sessionStore.find(request, request.cursor(), request.boundedLimit());
    }
}

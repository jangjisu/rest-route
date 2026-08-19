package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 프론트엔드 개발용 모킹 검색. {@link FlightSearchService}를 상속해서 세션 생성/조회, 정렬은
 * 그대로 물려 쓰고, 딜을 실제로 가져오는 {@link #fetchDeals}만 오버라이드해 고정 데이터를
 * 돌려준다 — 실 연동 의존성(rangeExecutor 등)은 아예 필요 없어서 생성자에서 전부 null로 넘긴다.
 *
 * <p>{@code totalSize}는 이번 검색에서 총 몇 건 생성할지를 뜻하는 모킹 전용 값이다 — 실 연동엔
 * 없는 개념이라 부모 클래스의 공개 API({@link FlightSearchService#search(FlightSearchRequestDto)})엔
 * 없고, 여기서만 노출한다.
 */
@Service
public class FlightSearchMockService extends FlightSearchService {

    /**
     * 스프링이 실제로 쓰는 생성자. 아래 편의 생성자(패키지 밖 테스트용)와 함께 있어 명시하지
     * 않으면 스프링이 편의 생성자를 대신 고를 수 있다.
     */
    @Autowired
    public FlightSearchMockService(FlightDealSessionStore sessionStore) {
        super(sessionStore, null, null, null, null, null);
    }

    /** SessionStore를 외부에 노출하지 않고도 조립할 때 쓰는 편의 생성자(패키지 밖 테스트 등). */
    public FlightSearchMockService() {
        this(FlightDealSessionStore.create());
    }

    public FlightDealSearchResponse search(FlightSearchRequestDto request, int totalSize) {
        return search(request, (Integer) totalSize);
    }

    @Override
    protected List<FlightDealResponse> fetchDeals(FlightSearchRequestDto request, Integer totalSize, String token) {
        return FlightSearchMockFixture.generateAll(request, token, totalSize);
    }
}

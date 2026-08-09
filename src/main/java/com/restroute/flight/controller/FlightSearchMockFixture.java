package com.restroute.flight.controller;

import com.restroute.flight.controller.response.FlightLegResponse;
import com.restroute.flight.controller.response.FlightSearchResultResponse;
import com.restroute.flight.controller.response.FlightTicketResponse;
import java.util.List;

/**
 * 프론트엔드 개발용 고정 모킹 데이터.
 * 실제 검색/정렬 비즈니스 로직은 아직 확정되지 않아, 화면 시안(오사카/후쿠오카/오키나와 예시)과
 * 동일한 형태의 응답만 우선 제공한다.
 */
final class FlightSearchMockFixture {

    private static final String ICN = "ICN";
    private static final String FUK = "FUK";

    private FlightSearchMockFixture() {}

    static FlightSearchResultResponse sampleResult() {
        return new FlightSearchResultResponse(128, "2026-08-09T14:02:00+09:00", List.of(osaka(), fukuoka(), okinawa()));
    }

    private static FlightTicketResponse osaka() {
        return new FlightTicketResponse(
                true,
                "오사카 간사이",
                "KIX",
                "2026-07-23",
                "2026-07-26",
                3,
                0,
                new FlightLegResponse("진에어", "LJ223", "08:30", ICN, "10:15", "KIX", 105, null, null),
                new FlightLegResponse("진에어", "LJ224", "11:20", "KIX", "13:10", ICN, 110, null, null),
                List.of("위탁수하물 15kg", "기내 10kg", "좌석 지정 유료"),
                198000,
                "KRW",
                "왕복·성인 1인",
                "Trip.com",
                "https://www.aviasales.com/search/mock-osaka-kix");
    }

    private static FlightTicketResponse fukuoka() {
        return new FlightTicketResponse(
                false,
                "후쿠오카",
                FUK,
                "2026-08-01",
                "2026-08-04",
                3,
                0,
                new FlightLegResponse("제주항공", "7C1401", "07:55", ICN, "09:15", FUK, 80, null, null),
                new FlightLegResponse("제주항공", "7C1406", "20:40", FUK, "22:05", ICN, 85, null, null),
                List.of("위탁수하물 없음·추가구매", "기내 10kg"),
                214000,
                "KRW",
                "왕복·성인 1인",
                "마이리얼트립",
                "https://www.aviasales.com/search/mock-fukuoka-fuk");
    }

    private static FlightTicketResponse okinawa() {
        return new FlightTicketResponse(
                false,
                "오키나와 나하",
                "OKA",
                "2026-07-30",
                "2026-08-02",
                3,
                1,
                new FlightLegResponse("티웨이", "TW301", "09:10", ICN, "14:40", "OKA", 330, FUK, 100),
                new FlightLegResponse("티웨이", "TW302", "15:30", "OKA", "17:55", ICN, 145, null, null),
                List.of("위탁수하물 15kg", "기내 10kg"),
                246000,
                "KRW",
                "왕복·성인 1인",
                "Trip.com",
                "https://www.aviasales.com/search/mock-okinawa-oka");
    }
}

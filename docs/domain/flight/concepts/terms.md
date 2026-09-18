---
domain: flight
aliases: ["딜", "Deal", "searchMode", "sector", "참조 데이터 엔티티"]
paths:
  - "src/main/java/com/restroute/flight/controller/response/FlightDealResponse.java"
  - "src/main/java/com/restroute/flight/controller/dto/FlightSearchMode.java"
  - "src/main/java/com/restroute/flight/controller/dto/FlightRegion.java"
  - "src/main/java/com/restroute/flight/domain/FlightCountryEntity.java"
  - "src/main/java/com/restroute/flight/domain/FlightCityEntity.java"
  - "src/main/java/com/restroute/flight/domain/FlightAirportEntity.java"
  - "src/main/java/com/restroute/flight/domain/FlightAirlineEntity.java"
  - "src/main/java/com/restroute/flight/service/FlightDealSessionStore.java"
related_domains: []
sources: []
---

# flight — 용어와 핵심 엔티티

- **딜(Deal)** — 검색 결과 한 건(`FlightDealResponse`). 출발/도착 각 편(`Leg`), 항공사,
  가격, 공휴일/주말 목록, `isLowestInRange`(그 검색 전체에서 최저가 1건 표시)를 담는다.
  왕복 전체에 항공사가 하나뿐이라는 Travelpayouts 응답 특성상 항공사 정보는 가는 편
  기준으로만 채워지고, 경유 공항·대기시간·수하물 규정은 이 API로 알 수 없어 응답에 없다.
- **searchMode(FIXED/RANGE)** — 지정날짜(FIXED) vs 기간(RANGE). 결과 의미가 완전히
  다르므로 항상 명시적으로 받고 절대 추측하지 않는다(`FlightSearchMode` 주석).
- **sector** — Travelpayouts 도시 데이터와 무관하게 서비스가 자체 정의한 지역권 필터
  (`JAPAN`/`SOUTHEAST_ASIA`/`GREATER_CHINA`/`GUAM_SAIPAN`, `FlightRegion`). `destination`을
  직접 지정하는 것과는 상호 배타적이다.
- **참조 데이터 엔티티** — `FlightCountryEntity`/`FlightCityEntity`/`FlightAirportEntity`/
  `FlightAirlineEntity`. 전부 `code`가 유니크 키이고 `korName`/`engName`을 둘 다 갖는다(공항은
  `korName`이 nullable). 항공사는 Travelpayouts `is_lowcost`를 그대로 가져온 `isLowCost`
  필드도 갖는다.
- **세션/커서** — 무한스크롤을 위해 첫 조회 결과 전체를 세션 토큰(4자리 랜덤 문자열)으로
  잠깐 캐시해두고, 이후 요청은 `cursor`(= `토큰_0001` 형식의 딜 id)로 그 세션을 이어 페이지만
  잘라 서빙한다(`FlightDealSessionStore`).

---
domain: flight
aliases: ["패키지 구조", "코드 경계"]
paths:
  - "src/main/java/com/restroute/flight/**"
related_domains: ["holiday", "route"]
sources: []
---

# flight — 코드 경계와 진입점

**구성 패턴**: `FlightSearchService`(얇은 오케스트레이터, `@Primary @Service`)는 세션
분기(첫 요청 vs 후속 페이지)만 판단하고, 실제 딜을 어떻게 구해오는지는 `FlightDealFetcher`
전략 인터페이스에 위임해서 계획 수립(`FlightRangeCallPlanner`/`FlightFixedCallPlanner`)과
조립(`FlightDealAssembler`)의 세부사항은 전혀 모른다(`e07cb2c` 리팩토링 목표 그대로). 실
연동 구현은 `FlightRealDealFetcher`(`@Component`)이고, `FlightSearchMockService`는
`FlightSearchService`를 상속하지 않는다 — 패키지 전용 정적 팩토리
`FlightSearchService.create(sessionStore, dealFetcher)`에 고정 픽스처를 반환하는
`FlightDealFetcher`를 주입해, 상속 없이도 같은 세션/페이지네이션 흐름과 요청/응답 계약을
그대로 재사용한다(`5d54c2e` — 상속 + null 생성자 인자로 흉내내던 이전 구조를 전략
인터페이스 + 조합으로 교체).

- `flight.client` — Travelpayouts Feign 클라이언트/설정/rate limiter, 예외.
- `flight.domain` / `flight.repository` — 참조 데이터 JPA 엔티티/리포지토리(국가/도시/
  공항/항공사).
- `flight.scheduler` — 참조 데이터 시작 시 재시딩 러너(`FlightReferenceDataStartupInitializer`
  `ApplicationRunner` 1개가 4개 도메인의 `ReferenceDataSyncSpec` 목록을 순회, 반복 스케줄
  아님).
- `flight.service` — 검색 오케스트레이션(`FlightSearchService`/`FlightSearchMockService`),
  계획(`FlightRangeCallPlanner`/`FlightFixedCallPlanner`), 조립(`FlightDealAssembler`,
  `FlightDealResponseMapper`, `FlightDealPostFilter`, `FlightDealHolidayEnricher`), 세션
  (`FlightDealSessionStore`), 참조 데이터 조회(`Flight{Country|City|Airport|Airline}
  QueryService`), 공휴일 조회(`FlightHolidayQueryService`), 시딩(`FlightReferenceDataSeeder`).
  대부분 package-private(`class`, `@Component`)로 선언되어 있어 flight 패키지 밖에서 직접
  주입할 수 없다 — `FlightSearchService`/`FlightSearchMockService`/`Flight*QueryService`만
  `public`.
- `flight.service.util` — 순수 알고리즘/헬퍼(`FlightSearchDestinations`,
  `FlightParallelPriceCalls`, `FlightDealResponses`, `FlightSectorCountries`,
  `FlightSearchMockFixture`) — 의존성 없는 static 메서드 위주.
- `flight.controller` — HTTP 진입점: `FlightSearchController`(`/api/flights/search`),
  `FlightSearchMockController`(`/api/flights/search/mock`), `FlightHolidayController`
  (`/api/flights/holidays`), `Flight{Country|City|Airport}Controller`(참조 데이터 조회),
  `FlightExceptionHandler`(flight 전용 예외 처리기).

**related_domains 근거**: `holiday`는 `FlightHolidayController`/`FlightHolidayQueryService`
/`FlightDealHolidayEnricher`/`FlightDealPostFilter`가 `holiday.repository.HolidayRepository`를
직접 참조하는 것으로 코드상 명확히 확인된다. `route`는 이번 조사에서 `flight` ↔ `route`
패키지 간 상호 참조를 코드에서 찾지 못했다 — **추정 — 확인 필요**: 두 도메인이 실제로
연결되어 있다면 그 지점(공유 UI, 공통 검색/추천 흐름 등)을 아직 특정하지 못했으므로,
인용자(호출한 에이전트)가 제시한 관련성 추정이 이 리포지토리의 현재 코드로는 뒷받침되지
않는다는 점을 명시해 둔다.

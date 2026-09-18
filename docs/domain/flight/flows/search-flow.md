---
domain: flight
aliases: ["검색 흐름", "무한스크롤", "모킹 API"]
paths:
  - "src/main/java/com/restroute/flight/controller/FlightSearchController.java"
  - "src/main/java/com/restroute/flight/controller/FlightSearchMockController.java"
  - "src/main/java/com/restroute/flight/controller/dto/FlightSearchRequestDto.java"
  - "src/main/java/com/restroute/flight/service/FlightSearchService.java"
  - "src/main/java/com/restroute/flight/service/FlightSearchMockService.java"
  - "src/main/java/com/restroute/flight/service/FlightRealDealFetcher.java"
  - "src/main/java/com/restroute/flight/service/FlightRangeCallPlanner.java"
  - "src/main/java/com/restroute/flight/service/FlightFixedCallPlanner.java"
  - "src/main/java/com/restroute/flight/service/FlightDealAssembler.java"
  - "src/main/java/com/restroute/flight/service/util/FlightParallelPriceCalls.java"
  - "src/main/java/com/restroute/flight/service/util/FlightSearchMockFixture.java"
related_domains: []
sources: []
---

# flight — 사용자·시스템 흐름

1. 클라이언트가 `GET /api/flights/search`를 `origin`, `searchMode`, `dateFrom`/`dateTo`,
   (선택) `destination` 또는 `sector`, (선택) `nights`, `includeWeekend`/`includeHoliday`/
   `includeTransfer`, `sort`, `cursor`/`limit` 등으로 호출한다.
2. 요청 DTO(`FlightSearchRequestDto`) 생성자가 즉시 전체 필드를 검증한다 — 이 객체가
   존재한다는 것 자체가 이미 검증 통과를 의미한다.
3. `cursor`가 없는 첫 요청이면 `FlightSearchService.search`가 `FlightDealSessionStore.create`를
   통해 새로 조회한다:
   - RANGE면 `FlightRangeCallPlanner`, FIXED면 `FlightFixedCallPlanner`가 각각 Travelpayouts
     호출 계획(`Callable` 목록)을 세운다.
   - `FlightParallelPriceCalls.runAll`이 가상 스레드로 병렬 호출하고 결과를 중복 제거해
     합친다.
   - `FlightDealAssembler`가 매핑 → 필터(`FlightDealPostFilter`) → 공휴일 채우기
     (`FlightDealHolidayEnricher`) → 전체 최저가 표시 → 정렬 순서로 최종 목록을 조립한다.
   - 세션 스토어가 각 항목에 id를 부여하고 저장한 뒤 첫 페이지를 반환한다.
4. `cursor`가 있는 후속 요청이면 세션을 찾아 다음 페이지만 잘라 반환한다. 세션이 없으면
   (형식 오류/만료/검색 조건 불일치 포함) `FlightDealNotFoundException`(404)을 던진다.
5. 프론트 개발 단계에서는 동일한 요청/응답 계약의 `GET /api/flights/search/mock`을 대신
   호출할 수 있다 — 실제 Travelpayouts 대신 고정 픽스처(`FlightSearchMockFixture`, 총
   77건)를 세션/페이지네이션까지 동일하게 흉내 낸다.
6. 참조 데이터(국가/도시/공항/항공사)는 별도 조회 API(`/api/flights/{countries|cities|
   airports|airlines}`, 추정 — 정확한 경로는 각 컨트롤러 확인 필요)로 자동완성 등에 쓰인다.

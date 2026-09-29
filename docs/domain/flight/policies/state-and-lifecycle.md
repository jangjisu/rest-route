---
domain: flight
aliases: ["검색 세션 수명", "참조 데이터 재시딩", "이름 조회", "rate limit 예산"]
paths:
  - "src/main/java/com/restroute/flight/service/FlightDealSessionStore.java"
  - "src/main/java/com/restroute/flight/scheduler/FlightReferenceDataStartupInitializer.java"
  - "src/main/java/com/restroute/flight/scheduler/ReferenceDataSyncSpec.java"
  - "src/main/java/com/restroute/flight/service/FlightDealResponseMapper.java"
  - "src/main/java/com/restroute/flight/client/TravelpayoutsRateLimiter.java"
related_domains: ["holiday"]
sources: []
---

# flight — 상태와 데이터 수명주기

- **검색 세션**: 서버 메모리(`ConcurrentHashMap`)에만 존재, TTL 300초, 만료되면 다음 조회
  시점에 lazy 제거된다. 재시작하면 전부 사라진다 — 영속 저장소가 아니다.
- **참조 데이터(국가/도시/공항/항공사)**: DB에 영속 저장되고,
  `FlightReferenceDataStartupInitializer`(`ApplicationRunner` 1개, 4개 도메인 spec 순회)가
  애플리케이션 시작 시 1회 SQL 파일로 재시딩한다. 이후 반복 동기화는 없다(공휴일과 달리
  `@Scheduled` 없음 — 정적 참조 데이터로 취급).
  `flight.{country|city|airline|airport}.sync.startup-enabled` 프로퍼티로 도메인별 개별
  비활성화 가능(기본 `true`).
- **이름 조회는 캐시 없이 매 요청 DB 조회**: 딜 응답 조립 시 공항/항공사 이름·저비용여부는
  `FlightAirportRepository`/`FlightAirlineRepository`의 `findByCode`로 그때그때 조회한다
  (`FlightDealResponseMapper`) — 항공권 검색 자체가 자주 호출되지 않는 기능이라, 참조 테이블
  전체(공항만 수천 건)를 상시 메모리에 올려두는 인메모리 캐시(`FlightAirlineNameCache` 등)를
  두는 대신 매번 조회하는 편이 낫다고 판단해 제거했다.
- **Travelpayouts rate limit 예산**: `TravelpayoutsRateLimiter`가 프로세스 전역으로 공유하는
  `AtomicReference<Budget>` 상태 하나로 관리된다. 서버가 고정 분당 창이 아니라 rolling
  window로 한도를 관리하는 게 실측 확인돼서, 클라이언트가 스스로 창을 계산하지 않고 매 응답의
  `x-rate-limit-remaining`/`x-rate-limit-reset` 헤더로 계속 재보정한다.
- **공휴일 데이터**: 이 도메인은 저장하지 않는다 — `holiday.repository.HolidayRepository`를
  직접 참조해 읽기만 한다(자세한 내용은 `holiday` 도메인 문서 8절 참고).

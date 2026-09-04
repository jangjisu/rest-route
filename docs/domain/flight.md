---
domain: flight
aliases: ["항공권 검색", "항공권"]
paths:
  - "src/main/java/com/restroute/flight/**"
related_domains: ["holiday", "route"]
sources:
  - "deadae5 feat: 항공권 최저가 검색 백엔드 뼈대 추가 (Travelpayouts 연동, 도시 참조 API, 모킹 컨트롤러)"
  - "c5d47db feat: 항공권 참조데이터(국가/도시/공항/항공사) 한글명 완성 + SQL 시딩 구조로 전환"
  - "e291337 fix: 참조데이터 시딩을 '비어있을 때만'에서 매번 재시딩으로 전환"
  - "d598322 refactor: 라이브 API 방식이던 인천/Travelpayouts JSON 클라이언트 삭제"
  - "9f7b23d build: Java 25(LTS) + Gradle 9.7.0 + Spring Boot 3.5.16로 업그레이드"
  - "c12d1dd feat: 공공데이터포털 특일 정보 API로 공휴일 자동 동기화"
  - "e44dcdc refactor: 공휴일 데이터/동기화 코드를 flight 패키지에서 holiday 패키지로 분리"
  - "1ab1d0c feat: FlightSearchService에 실제 Travelpayouts 연동 배선"
  - "c59e611 refactor: 실 연동/모킹 검색 서비스를 상속으로 분리"
  - "9324bae feat: RANGE 검색 호출 계획을 세우는 FlightRangeSearchPlanner 추가"
  - "d64eb2a feat: Travelpayouts API 호출에 rate limiting 게이트 도입"
  - "e6c083c feat: 딜 응답의 holiday를 공휴일/주말 날짜 목록으로 실제 채우기"
  - "e07cb2c refactor: FlightSearchService가 계획 수립·조립 세부사항을 몰라도 되게 정리"
  - "65e47ef refactor: 검색 실행 계획·조립 구조를 플래너/공유 실행기 중심으로 재편"
  - "145bd16 feat: 공휴일 목록 조회 API 추가"
  - "5d54c2e refactor: jisu-dev 네이밍/구조 감사 18건 수정 (상속 기반 모킹 구조를 FlightDealFetcher 전략 인터페이스로 교체)"
---

# flight

## 1. 목적과 범위

항공권 최저가 검색 기능. 출발지·목적지·날짜(정확한 날짜 또는 기간)를 조건으로 Travelpayouts
Data API(Aviasales)를 조회해 딜 목록을 최저가순/빠른 날짜순으로 보여준다. 범위에 포함되는
것: 검색 실행(RANGE/FIXED 두 모드), 무한스크롤 페이지네이션, 국가/도시/공항/항공사 참조
데이터 관리, 공휴일·주말 배지, 프론트 개발용 모킹 API. 범위에 포함되지 않는 것: 실제 결제나
예약(응답의 `bookingLink`로 외부 예약처로 안내만 함), 좌석 재고의 실시간성 보장(`seatsLeft`는
현재 응답 계약에는 있지만 매핑 코드에서는 항상 `null`), 공휴일 데이터 자체의 저장/동기화(→
`holiday` 도메인이 소유, 8절 참고).

이 도메인은 저장소 안에서 "얇은 오케스트레이터 서비스 + 단일 책임 협력자" 구성 패턴의
기준(reference) 구현으로 취급된다 — 다른 도메인이 리팩토링될 때 참고하는 대상이다.

## 2. 용어와 핵심 엔티티

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

## 3. 사용자·시스템 흐름

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

## 4. 정책과 불변 조건

- **fan-out 상한**: 검색 하나가 Travelpayouts를 몇 번 호출하든 항상 `MAX_FANOUT_CALLS=20`을
  넘지 않는다(`FlightSearchDestinations`). RANGE는 `destinations × months × nightsWindows`
  축을 아래 순서로 단계적으로 낮춰 예산을 맞춘다(`FlightRangeCallPlanner`). 개월 수는
  `dateFrom~dateTo`가 걸치는 달력상 월이라 줄일 수 없다 — grouped_prices가 달 하나 단위로만
  응답을 주기 때문이다.
  - **1단계 — nights 축.** 개별 모드는 값 하나하나를 정확한 창(`min=max=그값`)으로 따로
    조회해 "3박은 얼마, 4박은 얼마"를 각각 보여주지만 nights 개수만큼 호출이 늘어난다.
    범위 모드는 nights 전체를 `min~max` 창 하나로 뭉쳐 한 번만 조회한다. 예산을 넘으면
    개별 → 범위로 낮춘다. nights를 생략해 자동 확장된 경우는 최대 90개까지 갈 수 있어
    개별 모드를 시도조차 하지 않고 바로 범위 모드로 간다.
  - **2단계 — destination 축.** sector로 여러 국가가 잡혔으면 국가별 조회(N)에 "전체"
    조회(1)를 얹은 N+1로 예산을 다시 확인해, 넘지 않으면 국가별+전체를 함께 하고 넘으면
    국가별을 포기하고 전체 하나만 한다. 직접 지정 destination은 이미 1개라 해당 없다.
  - 이 순서 덕분에 최종 호출 수가 상한을 넘지 않는다 — 전체만 하는 경우는 destination 축이
    1이라 `1 × months × nightsWindows`인데, `nightsWindows`는 이미 1단계에서 원래 destination
    개수 기준으로 예산 안에 들도록 정해졌기 때문이다. 예시(1개월 기준):

  ```
  sector=JAPAN(1개국), nights=[3,4,5]           → 국가별 1×1×3=3, +전체 2×1×3=6 ≤20 → 국가별+전체
  sector 4개 전부(9개국), nights=[3,4]          → 국가별 9×1×2=18 ≤20 → nights는 개별 유지
                                                  +전체 10×1×2=20 ≤20 → 국가별+전체
  sector 4개 전부(9개국), nights=[3,4,5]        → 국가별 9×1×3=27 >20 → nights 범위 모드로 낮춤(9×1×1=9)
                                                  +전체 10×1×1=10 ≤20 → 국가별+전체
  sector 4개 전부(9개국), 3개월 걸침, nights=[3] → nights 유지(9×3×1=27>20→범위 9×3×1=27,
                                                  여전히 초과) → 국가별 포기, 전체만(1×3×1=3)
  ```
- **날짜 범위 상한**: `dateTo`는 오늘부터 3개월(`MAX_DATE_RANGE_MONTHS`)을 넘을 수 없다.
  RANGE의 fan-out 방지 목적과 별개로, FIXED 모드에도 동일 상한이 적용되는 건 서비스 자체의
  스코프 제한 때문이다(`FlightSearchRequestValidator` 주석).
- **nights 파싱**: `nights`를 생략하면 `dateFrom~dateTo` 기간 전체를 1박부터 최대박까지
  자동 확장한다(최대 90박, `MAX_NIGHTS`). FIXED 모드에서는 `nights`를 아예 받지 않는다.
  자동 확장된 nights는 개별 조회를 시도조차 하지 않고 바로 범위 모드로 간다.
- **중복 제거**: 국가별 조회와 "전체" 조회를 함께 하면 같은 항공권이 양쪽에 잡힐 수 있다 —
  `destinationAirport·departureAt·returnAt·flightNumber`가 모두 같으면 더 싼 쪽만 남긴다
  (`FlightParallelPriceCalls`).
- **후처리 필터 순서**: 요청 범위 밖 항목 제거(무조건) → `includeTransfer=false`면 경유 제외
  → `includeWeekend=false`면 주말 출발 제외 → `includeHoliday=false`면 공휴일 출발 제외.
  공휴일/주말 판정은 항상 **출발일** 기준(`FlightDealPostFilter`).
  전체 최저가 표시(`isLowestInRange`)는 이 필터를 모두 거친 뒤에만 계산한다 — 필터로 진짜
  최저가 항목이 빠질 수 있어서다.
- **정렬 기본값**: `sort` 생략 시 `PRICE`(최저가순). `includeTransfer` 생략 시 기본은
  포함(경유도 보여줌) — `false`일 때만 직항으로 좁힌다.
- **모든 외부 호출 실패는 즉시 전체 실패**: fan-out 호출 중 하나라도 실패하면 일부만 성공한
  결과를 조용히 보여주지 않고 전체를 실패시킨다.
- **참조 데이터 재시딩**: 국가/도시/공항/항공사는 매 애플리케이션 시작마다 SQL 시드 파일을
  전량 삭제 후 재삽입한다("비어있을 때만" 조건을 의도적으로 쓰지 않음) — 스키마 마이그레이션
  중 재시딩이 스킵돼 방치된 컬럼 값이 프로덕션에 노출된 실제 사고(2026-08-13, flight_city
  컬럼 분리 배포) 이후의 정책(`FlightReferenceDataSeeder` 주석).

## 5. 상태와 데이터 수명주기

- **검색 세션**: 서버 메모리(`ConcurrentHashMap`)에만 존재, TTL 300초, 만료되면 다음 조회
  시점에 lazy 제거된다. 재시작하면 전부 사라진다 — 영속 저장소가 아니다.
- **참조 데이터(국가/도시/공항/항공사)**: DB에 영속 저장되고,
  `FlightReferenceDataStartupInitializer`(`ApplicationRunner` 1개, 4개 도메인 spec 순회)가
  애플리케이션 시작 시 1회 SQL 파일로 재시딩한다. 이후 반복 동기화는 없다(공휴일과 달리
  `@Scheduled` 없음 — 정적 참조 데이터로 취급).
  `flight.{country|city|airline|airport}.sync.startup-enabled` 프로퍼티로 도메인별 개별
  비활성화 가능(기본 `true`).
- **인메모리 이름 캐시**: `FlightAirlineNameCache`(및 동종 airport/city/country 캐시)가
  code→이름/저비용여부를 `volatile Map`으로 들고 있다가 시작 시 1회, 시딩 직후 1회
  `refresh()`로 채워진다 — 딜 응답 조립 때 DB 왕복 없이 읽기 전용으로 쓰인다.
- **Travelpayouts rate limit 예산**: `TravelpayoutsRateLimiter`가 프로세스 전역으로 공유하는
  `AtomicReference<Budget>` 상태 하나로 관리된다. 서버가 고정 분당 창이 아니라 rolling
  window로 한도를 관리하는 게 실측 확인돼서, 클라이언트가 스스로 창을 계산하지 않고 매 응답의
  `x-rate-limit-remaining`/`x-rate-limit-reset` 헤더로 계속 재보정한다.
- **공휴일 데이터**: 이 도메인은 저장하지 않는다 — `holiday.repository.HolidayRepository`를
  직접 참조해 읽기만 한다(자세한 내용은 `holiday` 도메인 문서 8절 참고).

## 6. UI·오류·권한 상태

- **응답 봉투가 두 가지**: `/api/flights/search`(+ `/search/mock`)는 flight 전용
  `FlightApiResponse{data, meta, error}`를 쓰고, `/api/flights/holidays`는 나머지 도메인과
  같은 공통 `ApiResponse{success, data}`를 쓴다 — 같은 패키지 안에서도 API마다 봉투가
  다르다(추정 — 의도적 설계인지 이력 상 우연인지는 커밋 메시지로 확인 못함, 확인 필요).
- **`FlightExceptionHandler`**가 `com.restroute.flight.controller` 패키지 전용으로
  `@Order(HIGHEST_PRECEDENCE)`로 동작한다 — 공용 `GlobalExceptionHandler`보다 먼저 평가되게
  강제해야 flight 전용 응답 형태가 나간다(안 하면 공용 500 응답으로 새 나가는 걸 실측
  확인했다고 주석에 명시).
  - `FlightDealNotFoundException` → 404, `deal_not_found`
  - `InvalidFlightSearchException` → 400, `validation_failed` + 필드별 상세(`details`)
  - `ExternalApiException`(Travelpayouts/특일정보 등 외부 API 전체) → **200 OK**로
    `external_api_unavailable` 에러 페이로드를 내려준다 — 외부 장애를 5xx로 전파하지 않고
    클라이언트가 정상 응답 파싱 경로에서 그대로 처리하게 하는 설계.
  - `MissingServletRequestParameterException` → 400, `validation_failed`.
- **권한**: `/api/flights/**`는 `SecurityConfig`에서 `permitAll` — 인증 없이 누구나 호출
  가능하다. 모킹 API(`/search/mock`)도 프로파일 제한 없이 항상 활성화된다("프론트 개발이
  운영 환경에서도 이 계약으로 붙어야 해서").

## 7. 외부 시스템과 계약

**Travelpayouts Data API (Aviasales)** — `https://api.travelpayouts.com`
(`travelpayouts.api.url`), 토큰은 `TRAVELPAYOUTS_API_TOKEN` 환경변수.
- 엔드포인트: `GET /aviasales/v3/grouped_prices` — `origin`, `destination`(선택),
  `departure_at`(RANGE는 `yyyy-MM`, FIXED는 정확한 날짜), `return_at`(FIXED만),
  `min_trip_duration`/`max_trip_duration`(RANGE만), `currency=krw`, `token`.
- 응답: `{success, currency, data: {키: PriceItem}}`. `success=false`거나 응답 자체가
  없으면 `TravelpayoutsApiException`(→ 공통 `ExternalApiException`)을 던진다.
- `PriceItem`은 origin/destination/공항코드/가격/항공사/편명/출발·귀국 일시/경유
  횟수(가는 편·오는 편 별도)/총 소요시간/`gate`(예약처)/`link`(예약 링크)를 담는다.
- **Rate limiting**: 모든 호출이 `TravelpayoutsRateLimitingFeignClient`(Feign `Client`
  데코레이터)를 거쳐 `TravelpayoutsRateLimiter.acquire()`로 게이트된다. 기본 한도는
  분당 600건으로 초기화되고, 응답 헤더로 서버 기준 실측치로 계속 재보정된다. 예산이
  바닥이면 재보정될 때까지 블로킹(가상 스레드라 비용이 낮다는 전제).
  `TravelpayoutsFeignConfig`는 의도적으로 `@Configuration`을 안 붙여 이 rate limiter가
  다른 Feign 클라이언트에 전역 적용되지 않게 막는다.

**공공데이터포털 특일 정보 API** — flight이 직접 호출하지 않고 `holiday` 도메인을 거쳐
간접 소비한다. 계약 상세는 `holiday` 도메인 문서 7절 참고.

**인천공항/구 Travelpayouts JSON 클라이언트**는 한때 존재했으나 `d598322`에서 제거되고
SQL 파일 기반 정적 시딩(`FlightReferenceDataSeeder`)으로 대체되었다 — 참조 데이터는 더 이상
런타임에 외부 API를 호출하지 않는다.

## 8. 코드 경계와 진입점

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
- `flight.cache` — 참조 데이터 인메모리 이름 캐시 4종.
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

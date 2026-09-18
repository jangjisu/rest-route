---
domain: flight
aliases: ["항공권 검색", "항공권"]
paths: []
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

## 0. 문서 구성

이 문서는 목적·범위와 UI·오류·권한 상태만 소유하고, 나머지는 아래 group이 나눠 갖는다.

| group | 문서 | 다루는 것 |
|---|---|---|
| concepts | `concepts/terms.md` | 딜/searchMode/sector 용어, 참조 데이터 엔티티, 세션·커서 |
| flows | `flows/search-flow.md` | 검색 요청부터 페이지네이션, 모킹 API까지의 흐름 |
| policies | `policies/fanout-and-filters.md` | fan-out 상한, 날짜·nights 상한, 중복 제거, 후처리 필터 순서 |
| policies | `policies/state-and-lifecycle.md` | 검색 세션·참조 데이터 수명주기, 이름 조회, rate limit 예산 |
| contracts | `contracts/external-apis.md` | Travelpayouts(Aviasales) API 계약 |
| contracts | `contracts/code-map.md` | 패키지 경계, 진입점, related_domains 근거 |

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

→ `concepts/terms.md`

## 3. 사용자·시스템 흐름

→ `flows/search-flow.md`

## 4. 정책과 불변 조건

→ `policies/fanout-and-filters.md`

## 5. 상태와 데이터 수명주기

→ `policies/state-and-lifecycle.md`

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

→ `contracts/external-apis.md`

## 8. 코드 경계와 진입점

→ `contracts/code-map.md`

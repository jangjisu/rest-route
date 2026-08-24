---
domain: holiday
aliases: ["공휴일", "특일 정보"]
paths:
  - "src/main/java/com/restroute/holiday/**"
  - "src/main/java/com/restroute/flight/controller/FlightHolidayController.java"
  - "src/main/java/com/restroute/flight/service/FlightHolidayQueryService.java"
  - "src/main/java/com/restroute/flight/controller/response/FlightHolidayResponse.java"
  - "src/main/java/com/restroute/flight/service/FlightDealHolidayEnricher.java"
  - "src/main/java/com/restroute/flight/service/FlightDealPostFilter.java"
  - "src/main/java/com/restroute/controller/admin/AdminFlightHolidayController.java"
  - "src/main/java/com/restroute/service/admin/AdminFlightHolidayService.java"
related_domains: ["flight"]
sources:
  - "e44dcdc refactor: 공휴일 데이터/동기화 코드를 flight 패키지에서 holiday 패키지로 분리"
  - "c12d1dd feat: 공공데이터포털 특일 정보 API로 공휴일 자동 동기화"
  - "ea80599 feat: 공휴일 동기화를 delete+insert 재조정으로 바꾸고 주말은 저장 대상에서 제외"
  - "b1ac763 fix: 항공권 공휴일 동기화 코드 리뷰 지적사항 반영"
  - "8c70b65 feat: 공휴일 등록·동기화가 주말도 저장하도록 변경"
  - "72af422 feat: 항공권 공휴일 관리자 페이지 추가"
  - "e6c083c feat: 딜 응답의 holiday를 공휴일/주말 날짜 목록으로 실제 채우기"
  - "145bd16 feat: 공휴일 목록 조회 API 추가"
---

# holiday

## 1. 목적과 범위

한국 공휴일(대체공휴일 포함) 날짜와 이름을 저장·동기화하는 범용 데이터 도메인이다. 현재
유일한 소비자는 `flight` 도메인의 연차/연휴 배지 계산과 주말·공휴일 출발 필터링이지만,
`e44dcdc` 커밋 메시지가 명시하듯 "공휴일 정보 자체는 항공권 검색과 무관한 범용 데이터"라는
판단으로 `flight` 패키지에서 분리되었다. 범위에 포함되는 것: 공휴일 날짜/이름 저장,
공공데이터포털 API로부터의 일일 자동 동기화, 관리자 수동 등록/삭제. 범위에 포함되지 않는
것: 공휴일을 사용한 연차 계산 로직 자체(그건 프론트가 담당 — `FlightDealResponse.HolidayDay`
목록만 내려준다), 공휴일 여부에 따른 UI 표시.

## 2. 용어와 핵심 엔티티

- **HolidayEntity** (`holiday.domain`) — 공휴일 한 건(`holidayDate` 유니크, `name`,
  `adminOverridden`). 테이블명은 `flight_holiday`다 — `holiday` 패키지로 옮긴 뒤에도
  "스키마 안정성을 위해" 테이블명은 그대로 유지했다(`e44dcdc`).
- **adminOverridden** — 이 행을 관리자가 직접 등록했는지 나타내는 플래그. 배치 동기화는
  `adminOverridden=false`(=동기화가 예전에 채운) 행만 삭제 후보로 보고, 관리자가 등록한
  행은 API 응답에서 사라져도 절대 지우지 않는다.
- **주말(weekend)** — 별도 저장 개념이 아니라 `HolidayEntity.isWeekend(date)` 정적 메서드로
  그때그때 계산한다(토/일 여부). 다만 주말에 걸리는 공휴일(대체공휴일 지정 전의 원래
  공휴일 등)은 이름 정보가 필요해서 DB에 그대로 저장된다.
- **특일 정보(Special Day)** — 공공데이터포털 "한국천문연구원_특일 정보"
  (`getRestDeInfo`) API가 내려주는 원본 항목. `isHoliday="Y"`인 것만 "실제 공휴일"로
  취급한다(`SpecialDayResponse.Item.isActualHoliday()`).

## 3. 사용자·시스템 흐름

**동기화 흐름(시스템 주도)**: `HolidayScheduler`가 매일 00:30(Asia/Seoul)에 올해·내년치를
각각 `HolidaySyncService.syncYear(year)`로 동기화한다. 대체공휴일은 관보 고시가 늦어질 수
있어 매일 재확인하고, 내년 것도 미리 받아 연도가 바뀌기 전에 반영한다(`HolidayScheduler`
주석). 실패해도 다음 스케줄에서 재시도할 뿐 애플리케이션에 영향을 주지 않는다(연도별로
try/catch, 예외는 로그만 남김).

**관리자 흐름**: 관리자가 `/admin/flights/holidays` 화면(추정 — 이 리포지토리에는 컨트롤러/
서비스만 확인했고 프론트 화면 자체는 조사 범위 밖)에서 날짜를 골라 공휴일을 추가하거나
기존 등록분을 삭제한다. 추가/삭제 시 `AdminActivityLogService`에 로그가 남는다.

**조회 흐름(flight 소비자)**: 사용자가 항공권 검색 시 `includeHoliday=false`를 주면
`FlightDealPostFilter`가 출발일 기준으로 공휴일에 해당하는 딜을 제외하고, 검색 결과에는
`FlightDealHolidayEnricher`가 출발일~귀국일 범위의 공휴일·주말 목록을 채운다. 또한
`/api/flights/holidays`로 공휴일 목록 자체를 월별 필터와 함께 직접 조회할 수도 있다(달력
UI 등에 쓰일 것으로 추정 — 확인 필요).

## 4. 정책과 불변 조건

- `holidayDate`는 항상 유니크하다(DB 유니크 인덱스 + 관리자 등록 시
  `existsByHolidayDate`로 사전 차단, 위반 시 `DuplicateFlightHolidayException`).
- 관리자가 등록한 행(`adminOverridden=true`)은 배치 동기화가 절대 삭제하지 않는다 — 실제
  값의 최종 권한은 관리자 화면에 있다는 명시적 설계 원칙(`HolidaySyncService` 주석).
- 배치 동기화는 API 응답에 있는데 DB에 없는 날짜만 추가하고(기존 이름을 덮어쓰지 않음),
  동기화가 예전에 채웠는데 오늘 응답엔 없는 날짜만 삭제한다(delete+insert 재조정,
  `ea80599`).
- API가 그 해의 실제 공휴일을 0건 반환하면 동기화를 건너뛴다(전량 삭제 사고를 막기 위한
  안전장치, `HolidaySyncService.syncYear` 경고 로그 후 `HolidaySyncResult.of(0, 0)` 반환).
- 주말에 걸리는 공휴일도 저장 대상에서 제외하지 않는다 — 연차 배지 계산이 날짜별 이름까지
  보여줘야 하기 때문(`8c70b65`).
- flight 쪽 공휴일/주말 판정은 항상 **출발일** 기준이다 — 왕복 실제 인벤토리가 그 조합만
  주기 때문에 귀국일은 별도로 보지 않는다(`FlightDealPostFilter` 주석).

## 5. 상태와 데이터 수명주기

- **생성**: 배치 동기화(`HolidayEntity.syncedFromApi`, `adminOverridden=false`) 또는
  관리자 수동 등록(`HolidayEntity.createdByAdmin`, `adminOverridden=true`).
- **조회**: `FlightHolidayQueryService`(공개 API), `FlightDealHolidayEnricher`/
  `FlightDealPostFilter`(검색 내부 소비), `AdminFlightHolidayService`(관리자 목록).
- **갱신**: 이름 등 필드 수정 API는 없다 — 있는 건 생성/삭제뿐이다(추정 — 확인 필요: 관리자
  화면에 수정 기능이 없는지 프론트까지는 확인하지 못함).
- **삭제**: 관리자가 임의의 행을 id로 삭제하거나(`AdminFlightHolidayService.delete`), 배치
  동기화가 `adminOverridden=false`이고 API 응답에서 사라진 행을 삭제.
- **만료/캐시**: 별도 TTL이나 캐시 계층은 없다 — DB에 영구 저장되고, 매일 배치가 최신 상태로
  맞춘다.

## 6. UI·오류·권한 상태

- 특일 정보 API 호출 실패 시 `SpecialDayApiException`(→ 공통 `ExternalApiException`)이
  발생하고, 스케줄러가 잡아서 로그만 남긴다 — 사용자에게 노출되는 오류는 없다(배치이므로).
- 관리자 등록 시 날짜 공란/형식 오류는 `InvalidFlightHolidayRequestException`, 이름 공란은
  같은 예외의 다른 팩토리 메서드, 중복 날짜는 `DuplicateFlightHolidayException`, 존재하지
  않는 id 삭제는 `FlightHolidayNotFoundException`으로 각각 구분된다.
- `/api/admin/flights/holidays`는 `SecurityConfig`에서 `hasRole("ADMIN")`으로 보호된다.
  `/api/flights/holidays`(공개 조회)는 별도 인가 없이 `permitAll`이다.

## 7. 외부 시스템과 계약

**공공데이터포털 "한국천문연구원_특일 정보" (`getRestDeInfo`)**
- URL: `https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService` +
  `/getRestDeInfo` (`special-day.api.url` 프로퍼티, 서비스 키는
  `SPECIAL_DAY_API_SERVICE_KEY` 환경변수).
- 요청 파라미터: `solYear`(연도), `numOfRows`(=100 고정), `pageNo`(=1 고정), `_type=json`,
  `ServiceKey`.
- 응답 봉투: `{response:{header:{resultCode,resultMsg}, body:{items:{item:[...]}}}}`.
  `resultCode="00"`이 성공. `item`은 `{locdate(YYYYMMDD), dateName, isHoliday(Y/N)}`.
  `isHoliday="Y"`인 것만 실제 공휴일로 취급한다 — 즉 이 API가 "Y"가 아닌 특일 항목도 함께
  내려줄 수 있다는 뜻이다(추정 — 이 리포지토리 코드로 "N" 항목이 실제로 오는지까지는 확인
  못함, API 자체 스펙 특성으로 보임).
- `numOfRows=100`으로 한 해 전체를 페이지네이션 없이 한 번에 받는다고 가정한다(1년치
  공휴일이 100건을 넘지 않는다는 전제 — 명시적으로 다중 페이지를 처리하는 코드는 없음).

## 8. 코드 경계와 진입점

`holiday` 패키지 자체는 **HTTP 진입점이 없는 순수 데이터/동기화 계층**이다:
`holiday.domain`(엔티티), `holiday.repository`(JPA repository), `holiday.client`
(특일 정보 Feign 클라이언트 + 예외), `holiday.service`(`HolidaySyncService`, 동기화
오케스트레이션), `holiday.scheduler`(`HolidayScheduler`, cron 트리거).

이 데이터를 향한 HTTP 진입점은 두 곳에서 각자의 목적으로 소유한다:
- **`flight.controller.FlightHolidayController`** + **`flight.service.FlightHolidayQueryService`**
  — `holiday.repository.HolidayRepository`를 중간 추상화 없이 직접 주입받아 쓰는 **읽기
  전용** 조회 API(`GET /api/flights/holidays`, 공통 `ApiResponse` 봉투). `flight.service`의
  `FlightDealHolidayEnricher`/`FlightDealPostFilter`도 같은 방식으로 `HolidayRepository`를
  직접 참조해 검색 결과 보강·필터링에 쓴다 — 애초에 이 도메인이 flight 패키지 안에서
  태어났던 흔적이다.
- **`com.restroute.controller.admin.AdminFlightHolidayController`** +
  **`com.restroute.service.admin.AdminFlightHolidayService`** — `flight`도 `holiday`도
  아닌 별도의 공용 admin 패키지에 위치한 **CRUD** 관리 API(`/api/admin/flights/holidays`).
  `e44dcdc` 리팩토링이 데이터/동기화 코드를 `flight`→`holiday`로 옮길 때 "admin
  컨트롤러/서비스/DTO는 원래도 flight 패키지 밖(공용 admin 패키지)에 있었고 별도 확인된
  화면이라 이번엔 손대지 않았다"고 명시적으로 밝히고 있다.

요약하면: **`holiday` = 소유권 있는 데이터·동기화 계층(쓰기는 배치뿐)**, **`flight` =
읽기 전용 소비자(공개 조회 API + 검색 내부 보강/필터)**, **공용 `admin` 패키지 = 관리자
쓰기 전용 소비자(CRUD)**. 세 곳 모두 `HolidayEntity`/`HolidayRepository`에 직접
의존하며, `holiday` 패키지 안에는 이를 감싸는 서비스 인터페이스나 파사드가 없다.

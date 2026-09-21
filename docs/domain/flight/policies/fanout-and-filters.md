---
domain: flight
aliases: ["fan-out 상한", "후처리 필터", "중복 제거", "재시딩 정책"]
paths:
  - "src/main/java/com/restroute/flight/service/util/FlightSearchDestinations.java"
  - "src/main/java/com/restroute/flight/service/FlightRangeCallPlanner.java"
  - "src/main/java/com/restroute/flight/service/FlightFixedCallPlanner.java"
  - "src/main/java/com/restroute/flight/service/util/FlightParallelPriceCalls.java"
  - "src/main/java/com/restroute/flight/service/FlightDealPostFilter.java"
  - "src/main/java/com/restroute/flight/controller/dto/FlightSearchRequestValidator.java"
  - "src/main/java/com/restroute/flight/service/FlightReferenceDataSeeder.java"
related_domains: []
sources: []
---

# flight — 정책과 불변 조건

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

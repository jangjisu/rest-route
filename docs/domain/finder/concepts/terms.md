---
domain: finder
aliases: ["finder 용어", "배지", "관심 항목", "RestStopInterest"]
paths:
  - "src/main/resources/static/js/finder-condition.js"
  - "src/main/java/com/restroute/reststop/service/dto/RestStopInterest.java"
  - "src/main/java/com/restroute/reststop/controller/response/RestStopNearbyItemResponse.java"
  - "src/main/java/com/restroute/route/controller/response/RouteRestStopListItemResponse.java"
  - "src/main/java/com/restroute/route/service/RouteRestStopFuelTierCalculator.java"
related_domains: ["rest-stop", "route", "oil-price", "ev-charger", "rest-stop-content"]
sources: []
---

# finder — 용어와 핵심 엔티티

- **이름·거리로 찾기**: 위치·이름을 조건으로 `GET /api/rest-stops/nearby` 하나를 호출해 목록을 받는다.
- **목적지로 추천받기**: 출발지(위치 필수)+목적지를 이 도메인 전용 엔드포인트
  `GET /api/route-rest-stops/list`(route 도메인 소유)로 조회한다. 지도 화면이 쓰는 기존
  `GET /api/route-rest-stops`(대안 경로·이미지 포함, [[route]] 문서 소관)와는 별개 엔드포인트다.
- **sticky 헤더**(`.finder-sticky-top`): 이름·거리로 찾기는 헤더+검색창까지, 목적지로 추천받기는
  헤더+검색창+목적지 칩+조건 필터까지 스크롤 시 상단 고정된다.
- **RestStopInterest** (`reststop.service.dto.RestStopInterest`): `EV`/`GASOLINE`/`DIESEL`/`LPG`. 위치
  팝업 다음에 뜨는 연료 선택 팝업에서 고르며, 건너뛰면 `null`로 취급되어 그 관심 배지 자체가 안 붙는다
  (질문 문구: "지금 주행 중이신 차의 연료는 무엇인가요?"). 이 팝업은 두 화면이 완전히 공유하는 같은
  DOM/컴포넌트다.
- **route.service.dto.FuelType**: `GASOLINE`/`DIESEL`/`LPG` 3종(EV 없음). 목적지로 추천받기가 백엔드에
  유가 판정을 요청할 때 쓰는 파라미터 타입 — `RestStopInterest`와 값 이름은 겹치지만 타입은 다르다.
- **배지(태그)**: 두 화면은 판정 함수(`finder-condition.js`의 `nearbyBadgesFor` / `destinationBadgesFor`)와
  색상 매핑이 완전히 독립이다. 이름·거리로 찾기는 4개(규모/이용량/볼거리/이벤트) 항상 + 관심 항목 1개,
  목적지로 추천받기는 2개(규모/이용량) 항상 + 관심 항목 1개(EV충전 또는 유가). "이용량 상위 10%"는 두
  화면 다 상세 패널 기준 색(`--rr-color-warning-*`)으로 통일돼 있다.
- **hasTheme vs hasEvent**: 서로 다른 테이블·시간 의미를 가진 독립 신호([[rest-stop-content]] 소관),
  이름·거리로 찾기 전용(목적지로 추천받기 응답에는 없음). `hasTheme`은 `rest_theme`에 매핑된 행이
  하나라도 있으면 true(상시 — 예: "입장 거봉포도 체험장"). `hasEvent`는 `rest_event`에서 오늘 날짜
  (주입된 `Clock` 기준)가 `stime`~`etime` 사이인 행이 있으면 true(기간 한정). 한 휴게소가 둘 다/하나만/
  둘 다 아님 어느 쪽도 가능하다.
- **fuelBelowAverage vs fuelPriceTier**: 이름은 비슷하지만 기준이 다르다. 이름·거리로 찾기의
  `fuelBelowAverage`는 선택한 연료 1종만 [[oil-price]]의 오늘자 오피넷 전국 평균과 비교해 쌀 때만
  `true`(그 외는 항상 `null`). 목적지로 추천받기의 `fuelPriceTier`도 선택한 유종 1개만 보지만, 두 단계
  (`CHEAPEST`: 이번 조회 목록 안에서 그 유종 최저가, `BELOW_AVERAGE`: 전국 평균보다 저렴 — CHEAPEST가
  아닐 때만)를 구분한다(`RouteRestStopFuelTierCalculator` 소관). 색상은 둘 다 같은 "저렴" 태그로 묶어
  하나로 통일했다(`finder-badge-savings`) — 문구만 갈릴 뿐 색으로는 구분하지 않는다.

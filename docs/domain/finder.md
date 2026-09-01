---
domain: finder
aliases: ["휴게소 찾기", "finder 페이지", "이름·거리로 찾기", "목적지로 추천받기"]
paths:
  - "src/main/resources/templates/finder.html"
  - "src/main/resources/static/js/finder-app.js"
  - "src/main/resources/static/js/finder-condition.js"
  - "src/main/resources/static/js/finder-rest-stop-nearby-request.js"
  - "src/main/resources/static/js/finder-route-rest-stop-list-request.js"
  - "src/main/resources/static/js/finder-destination-chips.js"
  - "src/main/resources/static/css/finder.css"
  - "src/main/java/com/restroute/reststop/service/RestStopNearbyQueryService.java"
  - "src/main/java/com/restroute/reststop/service/dto/RestStopInterest.java"
  - "src/main/java/com/restroute/reststop/controller/response/RestStopNearbyItemResponse.java"
  - "src/main/java/com/restroute/route/service/RouteRestStopListQueryService.java"
  - "src/main/java/com/restroute/route/service/RouteRestStopFuelTierCalculator.java"
  - "src/main/java/com/restroute/route/controller/response/RouteRestStopListItemResponse.java"
related_domains: ["rest-stop", "route", "oil-price", "ev-charger", "rest-stop-content", "place-search-and-map-config"]
sources: []
---

# finder (휴게소 찾기 모바일 페이지)

## 1. 목적과 범위

`/finder` 모바일 전용 페이지. 지도 중심의 기존 화면(`index.html`/`rest-stops-map.js`, route 도메인)과 별개로,
"이름·거리로 찾기"(mode1)와 "목적지로 추천받기"(mode2) 두 진입 경로로 휴게소를 찾고, 각 결과 카드에 판단에
도움이 되는 배지 태그를 붙여 보여준다.

포함: 위치 권한 팝업(공용) + mode1 전용 관심 항목(연료/EV) 선택 팝업, mode1의 이름·거리 검색과 서버 계산 거리
정렬, mode2의 목적지 기반 추천과 프런트 조건 필터, 두 모드 각각의 배지 판정·색상 매핑.
제외: 배지가 참조하는 원본 데이터의 동기화·계산 자체(각 도메인 소유 — 아래 2·7절), 지도 렌더링, 휴게소 상세
화면 진입(카드 클릭 시 상세로 이어질 예정이나 이번 범위 밖).

## 2. 용어와 핵심 엔티티

- **mode1 (이름·거리로 찾기)**: 위치·이름을 조건으로 `GET /api/rest-stops/nearby` 하나를 호출해 목록을 받는다.
  화면 헤더+검색창은 스크롤 시에도 sticky로 고정되어 목록을 내려도 이름 검색을 계속할 수 있다.
- **mode2 (목적지로 추천받기)**: 출발지(위치 필수)+목적지를 finder 전용 신규 엔드포인트
  `GET /api/route-rest-stops/list`(route 도메인 소유, 아래 7절)로 조회한다. 지도 화면이 쓰는 기존
  `GET /api/route-rest-stops`(대안 경로·이미지 포함, [[route]] 문서 소관)와는 완전히 별개 엔드포인트 —
  v1에서는 기존 API를 그대로 재사용했으나, 유가 판정을 유종 1개로 좁히고 거리를 서버가 계산하도록
  바뀌면서(v2) 계약이 달라져 새 엔드포인트로 분리했다.
- **RestStopInterest** (`reststop.service.dto.RestStopInterest`): `EV`/`GASOLINE`/`DIESEL`/`LPG`. 위치 팝업
  다음에 뜨는 연료 선택 팝업에서 고르며, 건너뛰면 `null`로 취급되어 그 관심 배지 자체가 안 붙는다(질문 문구:
  "지금 주행 중이신 차의 연료는 무엇인가요?"). 이 팝업(`finderInterestPopup`)은 mode1/mode2가 완전히 공유하는
  같은 DOM/컴포넌트다 — `finder-app.js`의 `interestPopupTargetMode` 변수로 팝업을 연 게 어느 모드인지만
  구분해서, 완료 후 `enterMode1()`/`enterMode2()` 중 하나로 분기한다.
- **route.service.dto.FuelType**: `GASOLINE`/`DIESEL`/`LPG` 3종(EV 없음). mode2가 백엔드에 유가 판정을 요청할
  때 쓰는 요청 파라미터 타입 — `RestStopInterest`와 값 이름은 겹치지만 타입은 다르다(프런트가 `interest`에서
  EV를 제외하고 문자열로만 실어 보낸다).
- **배지(태그)**: mode1과 mode2는 판정 함수(`finder-condition.js`의 `nearbyBadgesFor` / `mode2BadgesFor`)가
  완전히 독립이다. mode1은 4개(규모/이용량/볼거리/이벤트) 항상 + 관심 항목 1개, mode2는 2개(규모/이용량)
  항상 + 관심 항목 1개(v2부터 규모·이용량·EV충전·유가만 남고 먹거리는 뺐다). 색상은 v2부터
  "이용량 상위 10%"를 포함해 둘 다 상세 패널 기준(`--rr-color-warning-*`)으로 통일했다(v1에서는 mode2만 파란색을
  따로 썼는데 지금은 안 씀).
- **hasTheme vs hasEvent**: 서로 다른 테이블·시간 의미를 가진 독립 신호([[rest-stop-content]] 소관), mode1
  전용(mode2 응답에는 없음). `hasTheme`은 `rest_theme`에 매핑된 행이 하나라도 있으면 true(상시, 기간 없음 —
  예: "입장 거봉포도 체험장"). `hasEvent`는 `rest_event`에서 오늘 날짜(주입된 `Clock` 기준)가 `stime`~`etime`
  사이인 행이 있으면 true(기간 한정). 한 휴게소가 둘 다/하나만/둘 다 아님 어느 쪽도 가능하다.
- **fuelBelowAverage(mode1) vs fuelPriceTier(mode2)**: 이름은 비슷하지만 기준이 다르다. mode1의
  `fuelBelowAverage`는 선택한 연료 1종만 [[oil-price]]의 오늘자 오피넷 전국 평균과 비교해 쌀 때만 `true`(그 외는
  항상 `null`, 명시적 `false` 없음). mode2의 `fuelPriceTier`도 v2부터 선택한 유종 1개만 보지만, 두 단계
  (`CHEAPEST`: 이번 조회 목록 안에서 그 유종 최저가, `BELOW_AVERAGE`: 전국 평균보다 저렴— CHEAPEST가 아닐
  때만)를 유지한다는 점이 mode1과 다르다(`RouteRestStopFuelTierCalculator` 소관).

## 3. 사용자·시스템 흐름

**공통 진입**: 랜딩 화면에서 "이름·거리로 찾기" 또는 "목적지로 추천받기" 버튼 선택 → 각 모드 전용 위치 권한
팝업(`finderPermissionMode1`/`finderPermissionMode2`).

**mode1 흐름**: 위치 팝업에서 Allow(좌표 확보) 또는 Skip(좌표 없이 진행) 둘 다 다음 단계인 연료/EV 관심 선택
팝업(`finderInterestPopup`)으로 이어진다 → 칩 선택 시 해당 관심을 저장하고 목록 화면 진입, "건너뛰기"는 관심을
`null`로 두고 진입, 팝업의 X(닫기)는 랜딩으로 되돌아간다 → 목록 화면 진입 시 항상 `runMode1Query('')`로 1회
조회(위치도 이름도 없으면 서버 호출 없이 빈 목록 유지) → 이름 검색창에 입력할 때마다 `nearby` 재호출(요청
경합은 `finder-rest-stop-nearby-request.js`가 요청 ID로 최신 응답만 반영해 방지) → 서버가 좌표가 있을 때만
거리 오름차순으로 정렬해 내려주므로 프런트는 정렬하지 않는다.

**mode2 흐름**: 위치 팝업은 Allow만 있고(위치 확보 실패 시 에러 메시지, 재시도) 위치 없이는 mode2로 진입하지
않는다 → Allow 성공 시 mode1과 같은 연료 선택 팝업으로 이어진다(`interestPopupTargetMode='mode2'`) → 목적지를
정하는 방법이 두 갈래다: **인기 칩**(부산역/대전역/강릉역/광주송정역) 클릭은 후보 선택 없이 바로
`/api/route-rest-stops/list`를 호출하고, **직접 입력 후 검색**은 먼저 place-search
([[place-search-and-map-config]] `GET /api/place-search`)로 후보 팝업(`finderDestinationCandidatePopup`)을
띄워 사용자가 하나를 고른 뒤 그 좌표로 같은 엔드포인트를 호출한다 → 조건 필터 칩(관심 항목에 따라 규모 큰
곳 + EV 충전 또는 유가 저렴한 곳)을 선택하면 프런트에서 AND 필터링만 수행(서버 재호출 없음).

**실패/빈 결과**: mode1은 검색 결과가 없거나 위치·이름 둘 다 없으면 상태 텍스트만 보여주고 목록을 비운다(빈
상태 문구, 별도 에러 코드 없음 — nearby 엔드포인트는 파라미터가 전부 optional이라 항상 200과 빈 배열/부분
필드로 응답). mode2는 목적지를 못 찾거나 경로 API 실패 시 상태 텍스트로 안내하고, 목적지 후보 팝업은 검색
결과 0건이면 "검색 결과가 없어요"(에러 아님, place-search 도메인 정책 그대로), 카카오 API 자체 장애면
"장소 검색을 잠시 이용할 수 없어요"를 보여준다.

## 4. 정책과 불변 조건

- **`/api/rest-stops/nearby`는 모든 입력 파라미터가 optional이고, 값이 없을 때 출력 필드를 생략이 아니라
  `null`로 내린다** — 프런트는 "있으면 표시, null/absent면 렌더링 안 함"만 하면 되고 위치·이름·관심 유무별로
  분기하는 별도 코드 경로가 없다. mode1은 이 계약 하나로 위치 없음/이름 없음/관심 없음 조합을 전부 처리한다.
- **mode1 관심 항목은 최대 1개, 마지막 배지 슬롯에만 영향**: 규모/이용량/볼거리/이벤트 4개 배지는 관심 선택과
  무관하게 항상 계산되고, EV 충전 개수 또는 유가 배지 중 하나만 선택한 관심에 따라 붙는다(둘 다 붙는 경우 없음
  — `interest`가 `EV`면 EV 카운트만, 연료 종류면 유가 배지만).
- **mode1의 유가 배지는 "전국 평균보다 저렴"만 판정한다** — mode2의 "이번 조회 목록 중 최저가"(CHEAPEST) 같은
  집합-내 최저가 판정은 mode1에는 없다(의도적 단순화). 국가 평균 데이터([[oil-price]] `NationalOilPriceService`)가
  없으면(당일 오피넷 응답 실패 등) 값이 없는 것으로 보고 `null`을 반환한다.
- **mode1의 EV 충전 배지는 개수가 1 이상일 때만 노출**: `evChargerCount`가 `null`이거나 0이면 배지 자체를
  만들지 않는다(0대라는 정보를 굳이 보여주지 않음).
- **mode2와 mode1은 배지 판정·색상 로직을 공유하지 않는다**(위 2절) — 한쪽을 고칠 때 다른 쪽에 영향이 없는지
  항상 별도로 확인해야 한다.
- **먹거리/화장실은 두 모드 배지에서 전부 빠져 있다**: 먹거리는 없는 휴게소가 사실상 없어 DB 누락만으로
  "없음"처럼 보일 위험이 있고, 화장실은 실시간 잔여 좌석수가 아니라 표시 근거가 약하다고 판단해서다.
  mode2는 v1에서는 먹거리 배지·필터가 있었으나 v2에서 mode1과 맞춰 없앴다.
- **mode2 배지·필터는 항상 관심 항목당 1개만 켜진다**: 규모/이용량 2개는 관심과 무관하게 항상 계산되고,
  마지막 자리는 `interest === 'EV'`면 EV 충전(대수), 유종이면 유가 등급 하나만 붙는다 — 두 신호가 동시에
  붙는 경우는 없다. 조건 필터 칩 구성(`mode2ConditionFilters`)도 배지와 정확히 대응해서 규모는 항상, 나머지
  한 자리만 관심 항목에 맞춰 보인다(건너뛰었으면 규모 칩 하나뿐).
- **"유가 저렴한 곳" 필터 하나가 CHEAPEST/BELOW_AVERAGE를 모두 매칭한다** — "제일 저렴"만 따로 거르는 필터는
  없다(`mode2MatchesFilter`의 `CHEAP_FUEL`).
- **인기 목적지 칩은 place-search 후보 팝업을 타지 않는다**: 이미 검증된 단일 역 이름이라 후보 선택 없이
  바로 route-rest-stops/list를 호출한다(빠른 경로 유지). 직접 입력만 후보 팝업을 거친다.

## 5. 상태와 데이터 수명주기

- **mode1Origin / mode1Interest / mode2Origin**: 페이지 메모리(JS 변수)에만 있는 세션 상태, 새로고침하면
  초기화된다(서버·로컬스토리지 저장 없음). 관심 항목은 팝업에서 한 번 고르면 그 화면 세션 동안 유지되고,
  재선택 UI는 없다(다시 고르려면 랜딩부터 재진입).
  화면 안팎으로 값이 넘어가는 지속 저장소는 없다.
- **요청 경합**: mode1의 `nearby` 요청과 mode2의 route 요청 모두 요청 ID/AbortController로 최신 요청만 반영하는
  공통 패턴(다른 finder request 모듈과 동일)을 쓴다 — 빠르게 이름을 바꿔 치며 검색해도 늦게 도착한 오래된
  응답이 화면을 덮어쓰지 않는다.
- **캐시**: 없음. 검색어·위치가 바뀔 때마다 매번 재호출.

## 6. UI·오류·권한 상태

- **인증**: 전부 공개 페이지·공개 API. 관리자 권한 불필요.
- **위치 거부/미지원**: mode1은 Skip이 있어 위치 없이도 이름 검색만으로 계속 쓸 수 있다. mode2는 Allow 실패
  시 에러 문구를 보여주고 같은 팝업에서 재시도만 가능(Skip 경로 없음 — 목적지 경로 계산 자체에 출발 좌표가
  필수이기 때문). 이 비대칭은 확인된 후 유지하기로 확정한 설계다.
  관심 팝업의 "건너뛰기"는 에러가 아니라 정상 경로로 취급되어 EV/유가 배지 없이 목록으로 바로 진입한다.
- **sticky 헤더**: mode1 화면에서 헤더+부제+검색창(`.finder-mode1-sticky`)만 스크롤 시 상단 고정되고, 빈
  상태/상태 문구/목록은 고정 영역 밖이라 그대로 스크롤된다.

## 7. 외부 시스템과 계약

- **`GET /api/rest-stops/nearby`**(`RestStopController`, [[rest-stop]] 도메인 소유): 쿼리 파라미터
  `originLat`, `originLng`, `name`, `interest`(`RestStopInterest` enum 문자열) 전부 optional.
  `RestStopNearbyItemResponse` 배열을 응답하며 필드는 기본 휴게소 정보 + `distanceMeters`(좌표 없거나 파싱
  실패 시 `null`, 정렬 시 항상 마지막) + `sizeTier`/`topTrafficTier`/`hasTheme`/`hasEvent`(항상 계산) +
  `evChargerCount`/`fuelBelowAverage`(선택한 `interest`에 해당할 때만 값, 그 외 `null`). 내부적으로
  `RestStopAggregateQueryService`([[rest-stop]]), `EvChargerQueryService.findActiveChargerCounts`([[ev-charger]]),
  `NationalOilPriceService.getTodaySummary`([[oil-price]])를 배치로 호출해 N+1을 피한다.
- **기존 `GET /api/rest-stops`, `/api/rest-stops/search`**: 지도 화면(`rest-stops-map.js`) 전용으로 그대로 남아
  있고, finder mode1은 이 두 엔드포인트를 쓰지 않는다(완전히 별개 진입점).
- **`GET /api/route-rest-stops/list`**(`RouteRestStopController`, [[route]] 도메인 소유, finder mode2 전용):
  `originLat`/`originLng` 필수, `destinationQuery` 또는 `destinationLat`+`destinationLng`(+`destinationName`)
  중 하나, `fuelType`(`GASOLINE`/`DIESEL`/`LPG`) 선택. 목적지 해석·경로 매칭은 기존 route 내부 부품
  (`RouteResolverService`/`RouteCoordinateReducer`/`RouteRestStopMatcher`)을 그대로 재사용하되 응답 조립은
  `RouteOptionAssemblyService`를 거치지 않고 `RouteRestStopListQueryService`가 직접 한다 — 대안 경로 중
  첫 번째만 쓰고, `RouteRestStopListItemResponse` 평평한 배열(거리 오름차순 정렬)로 응답한다. `distanceMeters`는
  mode1처럼 `CoordinateDistanceCalculator`로 서버가 계산(mode2는 위치가 항상 있어 `null` 케이스가 없음),
  `evChargerCount`는 `EvChargerQueryService.findActiveChargerCounts` 배치 조회, `fuelPriceTier`는
  `RouteRestStopFuelTierCalculator`가 `fuelType` 하나만 스코프해서 계산(`fuelType` 없으면 항상 `null`). 목적지를
  못 찾으면 기존과 동일하게 `RouteRestStopNotFoundException` → 404.
- **기존 `GET /api/route-rest-stops`**([[route]] 소관): 지도 화면 전용으로 그대로 남아 있고, 대안 경로·이미지·
  먹거리 비교 등 `/list`에는 없는 필드를 그대로 유지한다 — 두 엔드포인트는 같은 route 내부 부품을 재사용할 뿐
  서로 대체하지 않는다.
- **`GET /api/place-search`**([[place-search-and-map-config]] 소유): mode2가 목적지를 직접 입력했을 때만
  호출(인기 칩은 안 씀). 지도 화면의 목적지 후보 검색과 완전히 동일한 API·응답 형태를 그대로 재사용 — 계약을
  바꿀 필요가 없어서 새 엔드포인트를 만들지 않았다.

## 8. 코드 경계와 진입점

- **템플릿/정적 자원**: `templates/finder.html`, `static/js/finder-app.js`(화면 상태·이벤트 오케스트레이션),
  `static/js/finder-condition.js`(배지 판정 순수 함수, mode1/mode2 분리), `static/css/finder.css`.
- **요청 모듈**: `static/js/finder-rest-stop-nearby-request.js`(mode1, `/nearby` 전용), `static/js/
  finder-route-rest-stop-list-request.js`(mode2, `/route-rest-stops/list` 전용) — 둘 다 요청 ID/
  AbortController로 최신 응답만 반영하는 같은 패턴. `static/js/finder-destination-chips.js`(인기 목적지 칩
  4개, 라벨=검색어). mode2의 목적지 후보 검색은 지도 화면과 공유하는 `static/js/place-search-request.js`를
  그대로 import한다.
- **백엔드 — rest-stop 소유**(이 문서는 소비 관점만 기록): `reststop.service.RestStopNearbyQueryService`,
  `reststop.service.dto.RestStopInterest`, `reststop.controller.response.RestStopNearbyItemResponse`,
  `RestStopController.getNearbyRestStops`.
- **백엔드 — route 소유**(이 문서는 소비 관점만 기록): `route.service.RouteRestStopListQueryService`,
  `route.service.RouteRestStopFuelTierCalculator`, `route.controller.response.RouteRestStopListItemResponse`,
  `RouteRestStopController.getRouteRestStopList`.
- **진입점**: 브라우저에서 `GET /finder` 하나(서버 사이드 뷰 컨트롤러가 렌더링하는 단일 템플릿, 모드 전환은
  전부 클라이언트 사이드 화면 전환).

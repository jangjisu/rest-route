---
domain: finder
aliases: ["휴게소 찾기", "finder 페이지", "이름·거리로 찾기", "목적지로 추천받기"]
paths:
  - "src/main/resources/templates/finder.html"
  - "src/main/resources/static/js/finder-*.js"
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
"이름·거리로 찾기"와 "목적지로 추천받기" 두 진입 경로로 휴게소를 찾고, 각 결과 카드에 판단에 도움이 되는
배지 태그를 붙여 보여준다.

포함: 위치 권한 팝업, 연료/EV 관심 선택 팝업(두 화면 공유), 각 화면의 목록 조회·정렬·조건 필터, 배지
판정·색상 매핑, 결과 카드 클릭 시 지도 화면과 같은 상세 팝업 열기(마크업·모듈 재사용).
제외: 배지가 참조하는 원본 데이터의 동기화·계산 자체(각 도메인 소유 — 2·7절), 지도 렌더링, 상세 정보
자체의 조회·계산([[rest-stop-content]]/[[rest-stop]] 소유 — 이 도메인은 재사용만 한다).

## 2. 용어와 핵심 엔티티

- **이름·거리로 찾기**: 위치·이름을 조건으로 `GET /api/rest-stops/nearby` 하나를 호출해 목록을 받는다.
- **목적지로 추천받기**: 출발지(위치 필수)+목적지를 이 도메인 전용 엔드포인트
  `GET /api/route-rest-stops/list`(route 도메인 소유, 7절)로 조회한다. 지도 화면이 쓰는 기존
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

## 3. 사용자·시스템 흐름

**공통 진입**: 랜딩 화면에서 버튼 선택 → 화면별 위치 권한 팝업(`finderPermissionMode1`/
`finderPermissionMode2`) → 연료/EV 관심 팝업(`finderInterestPopup`, 공유) → 각 화면 진입. 위치·연료
둘 다 이미 이번 탭에서 답했으면 팝업 없이 바로 진행한다(5절).

**이름·거리로 찾기**: 위치 팝업에서 Allow(좌표 확보) 또는 Skip(좌표 없이 진행) 모두 연료 팝업으로
이어진다 → 진입 시 항상 한 번 조회(위치도 이름도 없으면 서버 호출 없이 빈 목록 유지) → 이름 검색창에
입력할 때마다 재호출(요청 경합은 `finder-rest-stop-nearby-request.js`가 요청 ID로 최신 응답만 반영해
방지) → 서버가 좌표가 있을 때만 거리 오름차순으로 정렬해 내려주므로 프런트는 정렬하지 않는다.

**목적지로 추천받기**: 위치 팝업은 Allow만 있다(실패 시 에러 메시지, 재시도) → 목적지를 정하는 방법이
두 갈래다: **인기 칩**(부산역/대전역/강릉역/광주송정역) 클릭은 후보 선택 없이 바로
`/api/route-rest-stops/list`를 호출하고, **직접 입력 후 검색**은 먼저 place-search
([[place-search-and-map-config]] `GET /api/place-search`)로 후보 팝업을 띄워 하나를 고른 뒤 그 좌표로
같은 엔드포인트를 호출한다 → 조건 필터 칩(규모·이용량은 항상, 나머지 한 자리만 관심 항목에 따라 EV
충전 또는 유가 저렴한 곳)을 선택하면 프런트에서 AND 필터링만 수행한다(서버 재호출 없음).

**휴게소 상세**: 두 화면 모두 결과 카드를 누르면(클릭/Enter/Space) 지도 화면(`index.html`)과 같은 상세
팝업이 finder 화면 안에서 그대로 뜬다(페이지 이동 없음, 8절). 닫기 전까지 검색/필터 상태는 유지된다.

**실패/빈 결과**: 이름·거리로 찾기는 검색 결과가 없거나 위치·이름 둘 다 없으면 상태 텍스트만 보여주고
목록을 비운다(별도 에러 코드 없음 — nearby 엔드포인트는 파라미터가 전부 optional이라 항상 200과 빈
배열/부분 필드로 응답). 목적지로 추천받기는 목적지를 못 찾거나 경로 API 실패 시 상태 텍스트로
안내하고, 목적지 후보 팝업은 검색 결과 0건이면 "검색 결과가 없어요", 카카오 API 장애면 "장소 검색을
잠시 이용할 수 없어요"를 보여준다.

## 4. 정책과 불변 조건

- **`/api/rest-stops/nearby`는 모든 입력 파라미터가 optional이고, 값이 없을 때 출력 필드를 생략이 아니라
  `null`로 내린다** — 프런트는 "있으면 표시, null/absent면 렌더링 안 함"만 하면 되고 위치·이름·관심
  유무별로 분기하는 별도 코드 경로가 없다.
- **이름·거리로 찾기의 관심 항목은 최대 1개, 마지막 배지 슬롯에만 영향**: 규모/이용량/볼거리/이벤트
  4개 배지는 관심 선택과 무관하게 항상 계산되고, EV 충전 개수 또는 유가 배지 중 하나만 선택한 관심에
  따라 붙는다(`interest`가 `EV`면 EV 카운트만, 연료 종류면 유가 배지만).
- **이름·거리로 찾기의 유가 배지는 "전국 평균보다 저렴"만 판정한다** — 목적지로 추천받기의 "이번 조회
  목록 중 최저가"(CHEAPEST) 같은 집합-내 최저가 판정은 없다(의도적 단순화). 국가 평균 데이터
  ([[oil-price]] `NationalOilPriceService`)가 없으면 값이 없는 것으로 보고 `null`을 반환한다.
- **이름·거리로 찾기의 EV 충전 배지는 개수가 1 이상일 때만 노출**: `evChargerCount`가 `null`이거나
  0이면 배지 자체를 만들지 않는다.
- **먹거리/화장실은 두 화면 배지에서 전부 빠져 있다**: 먹거리는 없는 휴게소가 사실상 없어 DB 누락만으로
  "없음"처럼 보일 위험이 있고, 화장실은 실시간 잔여 좌석수가 아니라 표시 근거가 약하다고 판단해서다.
- **목적지로 추천받기의 배지·필터는 항상 관심 항목당 1개만 켜진다**: 규모/이용량 2개는 관심과 무관하게
  항상 계산되고, 마지막 자리는 `interest === 'EV'`면 EV 충전(대수), 유종이면 유가 등급 하나만 붙는다.
  조건 필터 칩 구성(`destinationConditionFilters`)도 배지와 정확히 대응해서 규모·이용량은 항상, 나머지
  한 자리만 관심 항목에 맞춰 보인다(건너뛰었으면 규모·이용량 칩 2개뿐).
- **"유가 저렴한 곳" 필터 하나가 CHEAPEST/BELOW_AVERAGE를 모두 매칭한다** — "제일 저렴"만 따로 거르는
  필터는 없다(`destinationMatchesFilter`의 `CHEAP_FUEL`).
- **인기 목적지 칩은 place-search 후보 팝업을 타지 않는다**: 이미 검증된 단일 역 이름이라 후보 선택
  없이 바로 route-rest-stops/list를 호출한다. 직접 입력만 후보 팝업을 거친다.

## 5. 상태와 데이터 수명주기

- **각 화면의 origin/interest**: 화면별 모듈(8절) 안의 JS 변수에만 있는 상태이고, 좌표 자체는 절대
  캐시하지 않는다 — 진입할 때마다 `navigator.geolocation.getCurrentPosition()`을 새로 호출해서 최신
  위치를 받아온다(위치가 바뀌었을 수 있어서다).
- **`finder-session-memory.js`(`sessionStorage`)**: "이 위치 팝업/연료 팝업에 이미 답했다"는 사실만 탭
  세션 동안 기억해서, 같은 탭 안에서 다시 들어갈 때 팝업을 또 띄우지 않는다(새로고침에도 살아남고, 탭을
  닫으면 사라진다 — 다른 탭·다음 방문에는 이어지지 않는다). `finder.locationAnswered.nearby-search`/
  `.destination-recommendation`는 `'granted'`/`'skipped'`, `finder.interest`는 고른 유종/EV 값(건너뛰면
  내부적으로 `'NONE'` 센티널로 구분 저장 — "아직 안 답함"과 "건너뛰기를 답함"을 구분해야 해서). 재선택
  UI는 없고, "첫 화면으로" 뒤로가기로도 초기화되지 않는다 — 랜딩으로 돌아갈 수 있는 유일한 경로가
  뒤로가기라, 거기서 초기화하면 화면을 오갈 때마다 매번 다시 물어보게 돼 애초에 기억을 두는 의미가
  없어진다. 잘못 골랐을 때 고치는 방법은 탭을 닫았다 새로 여는 것뿐이다(sessionStorage 자체 수명).
- **요청 경합**: 두 화면의 목록 요청 모두 요청 ID/AbortController로 최신 요청만 반영하는 공통 패턴을
  쓴다 — 빠르게 조건을 바꿔가며 검색해도 늦게 도착한 오래된 응답이 화면을 덮어쓰지 않는다.
- **캐시**: 없음. 검색어·위치가 바뀔 때마다 매번 재호출.

## 6. UI·오류·권한 상태

- **인증**: 전부 공개 페이지·공개 API. 관리자 권한 불필요.
- **위치 거부/미지원**: 이름·거리로 찾기는 Skip이 있어 위치 없이도 이름 검색만으로 계속 쓸 수 있다.
  목적지로 추천받기는 Allow 실패 시 에러 문구를 보여주고 같은 팝업에서 재시도만 가능하다(Skip 경로
  없음 — 목적지 경로 계산 자체에 출발 좌표가 필수). 연료 팝업의 "건너뛰기"는 에러가 아니라 정상
  경로로 취급되어 EV/유가 배지 없이 목록으로 바로 진입한다.

## 7. 외부 시스템과 계약

- **`GET /api/rest-stops/nearby`**(`RestStopController`, [[rest-stop]] 도메인 소유): 쿼리 파라미터
  `originLat`, `originLng`, `name`, `interest`(`RestStopInterest` enum 문자열) 전부 optional.
  `RestStopNearbyItemResponse` 배열을 응답하며 필드는 기본 휴게소 정보 + `distanceMeters`(좌표 없거나
  파싱 실패 시 `null`, 정렬 시 항상 마지막) + `sizeTier`/`topTrafficTier`/`hasTheme`/`hasEvent`(항상
  계산) + `evChargerCount`/`fuelBelowAverage`(선택한 `interest`에 해당할 때만 값, 그 외 `null`).
  내부적으로 `RestStopAggregateQueryService`([[rest-stop]]),
  `EvChargerQueryService.findActiveChargerCounts`([[ev-charger]]),
  `NationalOilPriceService.getTodaySummary`([[oil-price]])를 배치로 호출해 N+1을 피한다.
- **기존 `GET /api/rest-stops`, `/api/rest-stops/search`**: 지도 화면(`rest-stops-map.js`) 전용으로 그대로
  남아 있고, 이 도메인은 쓰지 않는다.
- **`GET /api/route-rest-stops/list`**(`RouteRestStopController`, [[route]] 도메인 소유, 목적지로
  추천받기 전용): `originLat`/`originLng` 필수, `destinationQuery` 또는
  `destinationLat`+`destinationLng`(+`destinationName`) 중 하나, `fuelType`(`GASOLINE`/`DIESEL`/`LPG`)
  선택. 목적지 해석·경로 매칭은 기존 route 내부 부품(`RouteResolverService`/`RouteCoordinateReducer`/
  `RouteRestStopMatcher`)을 재사용하되 응답 조립은 `RouteRestStopListQueryService`가 직접 하고, 대안
  경로 중 첫 번째만 써서 `RouteRestStopListItemResponse` 평평한 배열(거리 오름차순 정렬)로 응답한다.
  `distanceMeters`는 `CoordinateDistanceCalculator`로 서버가 계산(위치가 항상 있어 `null` 케이스 없음),
  `evChargerCount`는 `EvChargerQueryService.findActiveChargerCounts` 배치 조회, `fuelPriceTier`는
  `RouteRestStopFuelTierCalculator`가 `fuelType` 하나만 스코프해서 계산(없으면 항상 `null`). 목적지를
  못 찾으면 `RouteRestStopNotFoundException` → 404.
- **기존 `GET /api/route-rest-stops`**([[route]] 소관): 지도 화면 전용으로 그대로 남아 있고, 대안 경로·
  이미지·먹거리 비교 등 `/list`에는 없는 필드를 유지한다 — 두 엔드포인트는 같은 route 내부 부품을
  재사용할 뿐 서로 대체하지 않는다.
- **`GET /api/place-search`**([[place-search-and-map-config]] 소유): 목적지를 직접 입력했을 때만
  호출한다(인기 칩은 안 씀). 지도 화면의 목적지 후보 검색과 동일한 API·응답 형태를 그대로 재사용한다.
- **휴게소 상세 API 6종**([[rest-stop-content]]/[[rest-stop]] 소유, `GET /api/rest-stops/{code}/*`): 카드
  클릭 시 지도 화면과 동일하게 `rest-stop-detail-request.js`가 그대로 호출한다(계약은 해당 문서 소관).

## 8. 코드 경계와 진입점

- `finder-app.js` — 조립부. `initializeNearbySearch`/`initializeDestinationRecommendation`이 반환한
  `enterNearbySearch`/`enterDestinationRecommendation`을 `initializeFinderEntryFlow`의 콜백으로
  연결한다.
- `finder-entry-flow.js` — 랜딩 타일 클릭부터 위치 동의 팝업(화면별)·연료 관심 팝업(공유)까지 소유.
  좌표+관심 항목이 정해지면 `onNearbySearchReady`/`onDestinationRecommendationReady` 콜백으로만
  넘기고, 그 화면들이 뭘 하는지는 모른다.
- `finder-nearby-search.js` / `finder-destination-recommendation.js` — 각 화면의 검색·필터·목록
  렌더링·요청 호출·뒤로가기. 서로의 존재를 모른다(둘 다 `enterNearbySearch(origin, interest)`/
  `enterDestinationRecommendation(origin, interest)` 함수 하나만 노출).
- `finder-render.js` — `showScreen`/`setLoading`/`setStatus`/`renderResultCard` 등 두 화면이 공유하는
  순수 렌더링 헬퍼. `document`를 인자로 받아서(`admin-rest-stop-image.js`와 같은 패턴) 테스트에서 가짜
  DOM으로 검증할 수 있다.
- `finder-condition.js` — 배지·필터 판정 순수 함수(화면별로 분리). `finder-session-memory.js` — 위치/
  연료 팝업 재노출 여부 판단(`sessionStorage` 읽기/쓰기, 5절)을 `finder-entry-flow.js`만 직접 참조한다.
- **요청 모듈**: `finder-rest-stop-nearby-request.js`(이름·거리로 찾기, `/nearby` 전용),
  `finder-route-rest-stop-list-request.js`(목적지로 추천받기, `/route-rest-stops/list` 전용) — 둘 다
  요청 ID/AbortController로 최신 응답만 반영하는 같은 패턴. `finder-destination-chips.js`(인기 목적지
  칩 4개, 라벨=검색어). 목적지 후보 검색은 지도 화면과 공유하는 `place-search-request.js`를 그대로
  import한다.
- `finder-rest-stop-detail.js` — 상세 팝업 열기/닫기, 주유 요금 갱신, 먹거리 모달 이벤트를 묶는다.
  지도 화면(`rest-stops-map.js`)과 똑같이 `rest-stop-detail-view.js`/`rest-stop-detail-request.js`를
  수정 없이 import하되, 부트스트랩 토스트(`showApiUnavailableAlert`) 자리엔 `onExternalUnavailable`을
  no-op으로 넘긴다(부트스트랩 없는 finder에선 상세 팝업 안 상태 문구가 같은 내용을 이미 보여준다).
  `finder-app.js`가 이 모듈의 `openDetail`을 두 화면 모듈에 그대로 넘겨준다.
- **백엔드 — rest-stop 소유**(이 문서는 소비 관점만 기록): `reststop.service.RestStopNearbyQueryService`,
  `reststop.service.dto.RestStopInterest`, `reststop.controller.response.RestStopNearbyItemResponse`,
  `RestStopController.getNearbyRestStops`.
- **백엔드 — route 소유**(이 문서는 소비 관점만 기록): `route.service.RouteRestStopListQueryService`,
  `route.service.RouteRestStopFuelTierCalculator`, `route.controller.response.RouteRestStopListItemResponse`,
  `RouteRestStopController.getRouteRestStopList`.
- **진입점**: 브라우저에서 `GET /finder` 하나(서버 사이드 뷰 컨트롤러가 렌더링하는 단일 템플릿, 화면
  전환은 전부 클라이언트 사이드).

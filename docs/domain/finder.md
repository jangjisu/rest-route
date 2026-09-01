---
domain: finder
aliases: ["휴게소 찾기", "finder 페이지", "이름·거리로 찾기", "목적지로 추천받기"]
paths:
  - "src/main/resources/templates/finder.html"
  - "src/main/resources/static/js/finder-app.js"
  - "src/main/resources/static/js/finder-condition.js"
  - "src/main/resources/static/js/finder-rest-stop-nearby-request.js"
  - "src/main/resources/static/css/finder.css"
  - "src/main/java/com/restroute/reststop/service/RestStopNearbyQueryService.java"
  - "src/main/java/com/restroute/reststop/service/dto/RestStopInterest.java"
  - "src/main/java/com/restroute/reststop/controller/response/RestStopNearbyItemResponse.java"
related_domains: ["rest-stop", "route", "oil-price", "ev-charger", "rest-stop-content"]
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
- **mode2 (목적지로 추천받기)**: 출발지(위치 필수)+목적지를 기존 route 도메인의
  `route-rest-stop-request.js`(`/api/route/rest-stops` 계열, [[route]] 문서 소관)로 그대로 재사용해 경로상 휴게소를
  받는다. finder는 이 응답 위에 조건 필터 칩(규모/먹거리/EV/유가 저렴)만 얹는다 — 백엔드 계약은 route 도메인
  그대로다.
- **RestStopInterest** (`reststop.service.dto.RestStopInterest`): `EV`/`GASOLINE`/`DIESEL`/`LPG`. mode1의 위치
  팝업 다음에 뜨는 연료 선택 팝업에서 고르며, 건너뛰면 `null`로 취급되어 그 관심 배지 자체가 안 붙는다(질문
  문구: "지금 주행 중이신 차의 연료는 무엇인가요?").
- **배지(태그)**: mode1과 mode2는 판정 함수(`finder-condition.js`의 `nearbyBadgesFor` / `badgesFor`)와 색상
  매핑(`finder-app.js`의 `MODE1_BADGE_COLOR_CLASS_BY_KEY` / `MODE2_BADGE_COLOR_CLASS_BY_KEY`)이 완전히 독립이다.
  같은 이름의 배지라도 모드마다 색이 다를 수 있다 — 예: "이용량 상위 10%"는 mode1에서는 상세 패널 기준 올리브색
  (`--rr-color-warning-*`), mode2에서는 기존 파란색(`--rr-color-info-*` 계열)을 그대로 쓴다. 하나로 통합하지
  않은 이유는 상세 패널 색 기준을 mode1에만 새로 맞췄기 때문.
- **hasTheme vs hasEvent**: 서로 다른 테이블·시간 의미를 가진 독립 신호([[rest-stop-content]] 소관). `hasTheme`은
  `rest_theme`에 매핑된 행이 하나라도 있으면 true(상시, 기간 없음 — 예: "입장 거봉포도 체험장"). `hasEvent`는
  `rest_event`에서 오늘 날짜(주입된 `Clock` 기준)가 `stime`~`etime` 사이인 행이 있으면 true(기간 한정). 한 휴게소가
  둘 다/하나만/둘 다 아님 어느 쪽도 가능하다.
- **fuelBelowAverage(mode1) vs fuelPriceTier(mode2)**: 이름은 비슷하지만 기준이 다르다. mode1의
  `fuelBelowAverage`는 선택한 연료 1종만 [[oil-price]]의 오늘자 오피넷 전국 평균과 비교해 쌀 때만 `true`(그 외는
  항상 `null`, 명시적 `false` 없음 — 배지를 안 붙이면 그만이라 굳이 구분하지 않음). mode2의 `fuelPriceTier`는
  route 도메인이 계산하는 두 단계 값으로, `CHEAPEST`(이번에 조회된 휴게소 집합 안에서 최저가, 우선 판정)와
  `BELOW_AVERAGE`(전국 평균보다 저렴, CHEAPEST가 아닐 때만)를 구분한다.

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
않는다 → 목적지 검색어 입력 → 기존 route 도메인 API를 그대로 호출해 방향(상행/하행)+경로 요약을 표시 →
조건 필터 칩(규모 큰 곳/먹거리 있는 곳/EV 충전/유가 저렴한 곳)을 선택하면 프런트에서 AND 필터링만 수행(서버
재호출 없음).

**실패/빈 결과**: mode1은 검색 결과가 없거나 위치·이름 둘 다 없으면 상태 텍스트만 보여주고 목록을 비운다(빈
상태 문구, 별도 에러 코드 없음 — nearby 엔드포인트는 파라미터가 전부 optional이라 항상 200과 빈 배열/부분
필드로 응답). mode2는 목적지를 못 찾거나 경로 API 실패 시 상태 텍스트로 안내한다.

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
- **먹거리/화장실은 mode1 배지에서 의도적으로 제외됨**: 먹거리는 없는 휴게소가 사실상 없어 DB 누락만으로
  "없음"처럼 보일 위험이 있고, 화장실은 실시간 잔여 좌석수가 아니라 표시 근거가 약하다고 판단해 mode1에는
  넣지 않았다(mode2의 `HAS_FOOD` 필터는 기존 유지 — route 도메인 응답 기준이라 별개 판단).

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
  필수이기 때문).
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
- **route 도메인 API**([[route]] 소관, 계약 상세는 그 문서 참고): finder mode2는 이 계약을 그대로 소비만 하고
  자체 백엔드 엔드포인트를 새로 만들지 않았다.

## 8. 코드 경계와 진입점

- **템플릿/정적 자원**: `templates/finder.html`, `static/js/finder-app.js`(화면 상태·이벤트 오케스트레이션),
  `static/js/finder-condition.js`(배지 판정 순수 함수, mode1/mode2 분리), `static/css/finder.css`.
- **요청 모듈**: `static/js/finder-rest-stop-nearby-request.js`(mode1, `/nearby` 전용, 요청 경합 방지 패턴 공유),
  mode2는 기존 route 도메인의 `static/js/route-rest-stop-request.js`를 그대로 import해서 재사용(별도 finder
  전용 요청 모듈 없음).
- **백엔드**(파일은 [[rest-stop]] 도메인 소유, 이 문서는 소비 관점만 기록): `reststop.service.RestStopNearbyQueryService`,
  `reststop.service.dto.RestStopInterest`, `reststop.controller.response.RestStopNearbyItemResponse`,
  `RestStopController.getNearbyRestStops`.
- **진입점**: 브라우저에서 `GET /finder` 하나(서버 사이드 뷰 컨트롤러가 렌더링하는 단일 템플릿, 모드 전환은
  전부 클라이언트 사이드 화면 전환).

---
domain: finder
aliases: ["finder 모듈 구조", "finder 코드 경계", "finder 진입점"]
paths:
  - "src/main/resources/static/js/finder-app.js"
  - "src/main/resources/static/js/finder-render.js"
  - "src/main/resources/static/js/finder-rest-stop-detail.js"
  - "src/main/resources/static/js/finder-rest-stop-nearby-request.js"
  - "src/main/resources/static/js/finder-route-rest-stop-list-request.js"
related_domains: ["rest-stop", "route", "place-search-and-map-config"]
sources: []
---

# finder — 코드 경계와 진입점

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
  연료 팝업 재노출 여부 판단(`sessionStorage` 읽기/쓰기)을 `finder-entry-flow.js`만 직접 참조한다.
- **요청 모듈**: `finder-rest-stop-nearby-request.js`(이름·거리로 찾기, `/nearby` 전용),
  `finder-route-rest-stop-list-request.js`(목적지로 추천받기, `/route-rest-stops/list` 전용) — 둘 다
  요청 ID/AbortController로 최신 응답만 반영하는 같은 패턴. `finder-destination-chips.js`(인기 목적지
  칩 4개, 라벨=`destinationName`이고 서버 `PopularDestination`의 표시명과 정확히 같아야 한다 — 어긋나면
  목적지를 찾지 못한다). 직접 입력한 목적지는 지도 화면과 공유하는 `place-search-request.js`로 후보를
  받아 고른 뒤, 그 좌표를 `destinationLat`/`destinationLng`로 넘긴다.
- `finder-rest-stop-detail.js` — `rest-stop-detail-popup.js`(지도 화면과 완전히 공유, 마크업까지 그
  모듈이 직접 만들어 붙인다 — finder.html엔 상세 팝업 마크업이 없다)를 호출하는 얇은 어댑터. finder가
  얹는 건 부트스트랩 토스트(`showApiUnavailableAlert`, 기본값) 자리의 no-op과 Escape 키 처리뿐이다.
  `finder-app.js`가 이 모듈의 `openDetail`을 두 화면 모듈에 그대로 넘겨준다.
- **백엔드 — rest-stop 소유**(이 문서는 소비 관점만 기록): `reststop.service.RestStopNearbyQueryService`,
  `reststop.service.dto.RestStopInterest`, `reststop.controller.response.RestStopNearbyItemResponse`,
  `RestStopController.getNearbyRestStops`.
- **백엔드 — route 소유**(이 문서는 소비 관점만 기록): `route.service.RouteRestStopListQueryService`,
  `route.service.RouteRestStopFuelTierCalculator`, `route.controller.response.RouteRestStopListItemResponse`,
  `RouteRestStopController.getRouteRestStopList`, `route.service.DestinationResolver`,
  `route.domain.PopularDestination`.
- **진입점**: `GET /finder`(서버 사이드 뷰 컨트롤러가 렌더링하는 단일 템플릿, 화면 전환은 전부
  클라이언트 사이드) — `HomeController`가 `GET /`도 User-Agent로 모바일 기기를 감지하면 같은
  `finder` 뷰로 보낸다(그 외는 지도 화면 `index`).

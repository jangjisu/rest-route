---
domain: finder
aliases: ["finder API", "nearby API", "route-rest-stops/list", "finder 외부 계약"]
paths:
  - "src/main/java/com/restroute/route/service/RouteRestStopListQueryService.java"
  - "src/main/java/com/restroute/route/service/RouteRestStopFuelTierCalculator.java"
related_domains: ["rest-stop", "route", "oil-price", "ev-charger", "rest-stop-content", "place-search-and-map-config"]
sources: []
---

# finder — 외부 시스템과 계약

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
  추천받기 전용): `originLat`/`originLng` 필수, `destinationLat`+`destinationLng` 또는
  `destinationName` 중 하나, `fuelType`(`GASOLINE`/`DIESEL`/`LPG`) 선택. **자유 검색어를 받지
  않는다** — 좌표로 받거나, 서버가 좌표를 아는 이름(`PopularDestination`)으로만 받으므로 이
  엔드포인트는 지오코딩을 거치지 않고 외부 호출이 길찾기 1회로 끝난다. 목적지 해석은
  `DestinationResolver`, 경로 매칭은 기존 route 내부 부품(`RouteCoordinateReducer`/
  `RouteRestStopMatcher`)을 재사용하되 응답 조립은 `RouteRestStopListQueryService`가 직접 하고, 대안
  경로 중 첫 번째만 써서 `RouteRestStopListItemResponse` 평평한 배열(거리 오름차순 정렬)로 응답한다.
  `distanceMeters`는 `CoordinateDistanceCalculator`로 서버가 계산(위치가 항상 있어 `null` 케이스 없음),
  `evChargerCount`는 `EvChargerQueryService.findActiveChargerCounts` 배치 조회, `fuelPriceTier`는
  `RouteRestStopFuelTierCalculator`가 `fuelType` 하나만 스코프해서 계산(없으면 항상 `null`). 목적지를
  못 찾으면 `RouteRestStopNotFoundException` → 404.
- **기존 `GET /api/route-rest-stops`**([[route]] 소관): 지도 화면 전용으로 그대로 남아 있고, 대안 경로·
  이미지·먹거리 비교 등 `/list`에는 없는 필드를 유지한다 — 두 엔드포인트는 같은 route 내부 부품을
  재사용할 뿐 서로 대체하지 않는다. 자유 검색어 지오코딩이 남아 있는 쪽도 이 엔드포인트다.
- **`GET /api/place-search`**([[place-search-and-map-config]] 소유): 목적지를 직접 입력했을 때만
  호출한다(인기 칩은 안 씀). 지도 화면의 목적지 후보 검색과 동일한 API·응답 형태를 그대로 재사용한다.
- **휴게소 상세 API 6종**([[rest-stop-content]]/[[rest-stop]] 소유, `GET /api/rest-stops/{code}/*`): 카드
  클릭 시 지도 화면과 동일하게 `rest-stop-detail-popup.js`(내부의 `rest-stop-detail-request.js`)가 그대로
  호출한다(계약은 해당 문서 소관).

---
domain: route
aliases: ["경로 휴게소 검색", "route-rest-stop"]
paths:
  - "src/main/java/com/restroute/route/controller/**"
  - "src/main/java/com/restroute/route/service/**"
  - "src/main/resources/static/js/rest-stops-map.js"
  - "src/main/resources/static/js/bottom-sheet.js"
related_domains: ["rest-stop", "oil-price", "ev-charger", "place-search-and-map-config", "finder"]
sources: []
---

# route (경로 휴게소 검색)

## 1. 목적과 범위

출발지·도착지 좌표(또는 도착지 검색어)를 받아, 그 경로 주변에 있는 휴게소를 경로 순서대로 찾아
목록으로 돌려주는 기능. 카카오 대안 경로(alternatives)까지 지원하며, 각 휴게소에 이미지·EV
충전·테마/이벤트·유가 비교·추천 태그 등 부가 정보를 붙여서 응답한다.

포함: 목적지 지오코딩, 카카오 길찾기 호출, 경로 좌표 축소, 반경 기반 휴게소 매칭, 상·하행
방향 판별, 휴게소 detail 부착.
제외: 실제 내비게이션(턴바이턴 안내), 휴게소 마스터 데이터 자체의 편집(→ `rest-stop`/`admin`
도메인 소관), 지도 렌더링 자체(→ `place-search-and-map-config`).

## 2. 용어와 핵심 엔티티

- **Destination**: 목적지(name, latitude, longitude). 좌표가 직접 오면 그대로 쓰고, 없으면
  `destinationQuery`를 카카오 키워드 검색으로 지오코딩해서 만든다.
- **RawRouteResult**: 목적지 해석 + 카카오 길찾기 원본 응답(`List<KakaoDirectionsResponse.Route>`).
  아직 좌표가 축소되지 않은 상태.
- **RouteGeometry**: `(RoutePath, Summary)`. 축소된 좌표열과 거리/시간/통행료 요약.
- **RoutePath**: 이미 축소된 좌표 목록(`PathPoint`)과 구간별 `trafficState`를 들고, 근접거리
  계산(`nearestTo`)과 진행방향 좌/우 판별(`sideOfTravel`)을 제공하는 순수 값객체.
- **RouteCandidate**: `(routeIndex, RouteGeometry, 매칭된 RouteRestStopItem 목록)`. 대안 경로
  하나에 대한 매칭 결과.
- **RouteOption / RouteSummary / RouteRestStopItem**: 최종 응답 DTO. `RouteRestStopItem`은
  서비스구역코드, 이름, 노선명, 좌표, 경로까지 거리, 인근 소통상황(`NearbyTraffic`), EV/테마/
  이벤트 flag, 이미지 URL, 유가 비교요약, 추천 태그, `hasDirectionAlternative`를 담는다.
- **hasDirectionAlternative**: 같은 이름의 상·하행 휴게소 페어가 함께 잡혔는데 진행방향으로
  어느 쪽이 실제 진입 가능한지 애매할 때, 제거하지 않고 이 flag만 켜서 두 후보를 그대로 전달함을
  뜻한다.

## 3. 사용자·시스템 흐름

1. **좌표/검색어 받기** — `RouteResolverService.resolveDestinationAndRoute()`가 목적지를
   확정하고(좌표 직접 지정 또는 `KakaoMapClient.searchKeyword()` 지오코딩), 카카오
   `getDirections()`를 호출해 원본 경로(들)를 받는다. 길찾기 자체가 실패하면(`result_code`
   비정상) 여기서 바로 `RouteRestStopNotFoundException`으로 끝난다.
2. **좌표 개수 줄이기** — `RouteCoordinateReducer.reduce()`가 대안 경로마다 원본 폴리라인을
   200m 간격/최소 300점 기준으로 균등 샘플링(uniform sampling)해 축소한다. 축소 후 폴리라인이
   빈 경로는 그 경로만 제외하고, 전부 비면 예외로 끝낸다.
3. **방향 매칭** — `RouteRestStopMatcher.match()`가 대안 경로마다 독립적으로, 전체 휴게소 중
   경로 반경(`radiusMeters`) 안에 있는 것만 골라 경로 순서대로 정렬하고, 같은 이름의 상·하행
   페어는 진행방향 기준으로 실제 진입 가능한 쪽만 남긴다(애매하면 `hasDirectionAlternative`만
   켠다).
4. **detail 조립** — `RouteOptionAssemblyService.attachDetails()`가 대안 경로 전체의 매칭
   결과를 모아 이미지 유무/EV충전/테마/이벤트 집계를 한 번만 조회하고(`RestStopAggregateQueryService`),
   오늘자 전국 평균 유가(`NationalOilPriceService`)와 비교해 가격차이·추천 태그를 계산해 붙인다.
5. `RouteRestStopService.findRouteRestStops()`가 위 4단계를 순서대로 호출하는 오케스트레이터이며,
   최종적으로 `RouteRestStopResponse(destination, routes)`를 반환한다.

프론트엔드: 사용자가 지도에서 목적지를 검색하거나 좌표를 찍으면 `/api/route-rest-stops`를
호출하고, 결과는 모바일에서 드래그로 리사이즈되는 바텀시트(`bottom-sheet.js`)에 표시된다 —
**추정 — 확인 필요**: 실패 시 프론트엔드가 정확히 어떤 UI를 보여주는지는 이번 조사 범위 밖.

## 4. 정책과 불변 조건

- 대한민국은 우측통행이므로 `RoutePath.Side.RIGHT`만 진행방향에서 실제로 진입 가능한 쪽으로
  간주한다(`RouteRestStopMatcher`).
- 상·하행 페어 판별이 애매하면(진행방향 기준 RIGHT가 0개거나 2개 이상) **아무것도 제거하지
  않고** `hasDirectionAlternative`만 켠다 — 잘못 걸러내는 것보다 사용자에게 둘 다 보여주고
  판단을 맡기는 쪽을 택함.
- 좌표 축소는 200m 간격 기준 목표 개수를 정하되 최소 300개는 유지한다(`RouteCoordinateReducer`).
  근접거리 계산이 매 휴게소마다 전체 정점을 순회하므로 성능을 위한 절충.
- 길찾기 실패 시 `result_code`별로 안내 메시지를 구분한다: 101/105(출발지 주변 도로 없음),
  102/106(도착지 주변 도로 없음), 104(출발지·도착지가 너무 가까움), 그 외 일반 실패 메시지.
- 목적지 좌표가 함께 오면 지오코딩을 건너뛴다(좌표 우선). 좌표가 일부만 오면 `destinationQuery`
  지오코딩으로 폴백한다. 표시명이 비어있으면 `"목적지"` 기본값을 쓴다.
- 이미지/EV/테마/이벤트 집계는 대안 경로 전체에 대해 **한 번만** 조회한다(경로마다 반복 조회하지
  않음) — `RouteOptionAssemblyService.aggregatesForCandidates()`.

## 5. 상태와 데이터 수명주기

매 요청마다 카카오 API를 실시간 호출해 계산하는 stateless 조회다. 이 도메인 자체는 아무 것도
저장하지 않는다 — 휴게소 마스터 데이터는 `rest-stop` 도메인이, 오늘자 평균 유가는 `oil-price`
도메인이 각각 소유·동기화하고, 여기서는 읽기 전용으로 조합만 한다.

**추정 — 확인 필요**: 이 API 응답 자체에 캐싱(예: 같은 출발/도착 조합 반복 요청 시)이 있는지는
`RouteRestStopService`/`RouteResolverService` 코드상 보이지 않음 — 없는 것으로 보이나 캐시
레이어가 다른 곳(예: Kakao 클라이언트 내부)에 있을 가능성은 확인하지 않음.

## 6. UI·오류·권한 상태

- `RouteRestStopNotFoundException`은 `BusinessException`(→ `ResponseCode.NOT_FOUND`)을
  상속해 `GlobalExceptionHandler`의 `@ExceptionHandler(BusinessException.class)`가 공통
  포맷으로 응답한다. 목적지 검색 결과 없음, 좌표 해석 실패, 길찾기 실패(result_code별), 경로
  좌표 없음이 모두 이 예외로 통일되어 있고, 메시지만 케이스별로 다르다.
- 별도 인증/권한 검사는 없음 — 공개 API(`/api/route-rest-stops`)로 보임. **추정 — 확인 필요**:
  Spring Security 설정에서 이 경로가 별도로 제한되는지는 config를 따로 확인하지 않음.
- 반경(`radiusMeters`)은 요청 파라미터로 조절 가능하며 기본값 1000m.

## 7. 외부 시스템과 계약

- **KakaoMapClient**: `searchKeyword(query)`(키워드 장소 검색, 지오코딩용) /
  `getDirections(origin, destination)`(길찾기, 대안 경로 포함) 두 메서드를 호출한다. 좌표
  포맷은 `RouteCoordinateFormat.toParam(lng, lat)`(경도,위도 순서 문자열)를 통해 카카오 API
  규격에 맞춘다.
- 응답의 `result_code`가 0이 아니면 실패로 간주(`KakaoDirectionsResponse.failedToRoute()`).

## 8. 코드 경계와 진입점

- **진입점**: `RouteRestStopController.getRouteRestStops()` (`GET /api/route-rest-stops`,
  파라미터: originLat/Lng, destinationQuery, destinationLat/Lng, destinationName, radiusMeters).
- **오케스트레이터**: `RouteRestStopService` — 4단계(좌표 얻기 → 좌표 축소 → 방향 매칭 →
  detail 조립)를 이름 붙은 메서드 호출로 그대로 노출한다(각 단계 구현은 아래 협력자에 위임).
- **협력자(`@Component`, 의존성 없는 순수 알고리즘)**: `RouteCoordinateReducer`(좌표 축소),
  `RouteRestStopMatcher`(반경/방향 매칭).
- **협력자(`@Service`, 외부 자원/다른 서비스 의존)**: `RouteResolverService`(카카오 API 호출),
  `RouteOptionAssemblyService`(집계 조회 + 비교/추천 조립), `RouteRestStopComparisonSummaryService`,
  `RouteRestStopRecommendationTagService`.
- **값객체**: `service/route/dto/*` (RoutePath, PathPoint, Direction, RouteCandidate,
  ResolvedRoute, RouteRestStopComparison, RouteRestStopRecommendationStandards 등).
- **예외**: `service/route/exception/RouteRestStopNotFoundException`.
- 이 도메인은 `FlightSearchService`(`flight` 도메인)의 "얇은 오케스트레이터 + 이름 붙은
  단계별 협력자" 구성을 참고해 리팩토링된 것으로, 그 패턴을 따르는 두 번째 사례다.
- **finder mode2 전용 진입점**([[finder]] 소비, 상세는 그 문서 참고): `RouteRestStopController.
  getRouteRestStopList()`(`GET /api/route-rest-stops/list`)는 위 1~5단계 중 좌표/경로 관련 부품
  (`RouteResolverService`/`RouteCoordinateReducer`/`RouteRestStopMatcher`)만 재사용하고,
  `RouteOptionAssemblyService`는 거치지 않는다 — 대신 `RouteRestStopListQueryService`가 대안 경로 중
  첫 번째만 골라 거리(서버 계산)·유가(`RouteRestStopFuelTierCalculator`, 요청 유종 1개만 스코프)를 직접
  조립한다. 기존 `getRouteRestStops()`(지도 화면용, 대안 경로 전체·이미지·먹거리 포함)는 계약을 그대로
  유지하려고 손대지 않았다 — 계약이 달라져야 할 때는 기존 진입점을 고치지 않고 새 진입점을 추가한다는
  판단.

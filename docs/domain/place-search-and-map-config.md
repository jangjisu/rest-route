---
domain: place-search-and-map-config
aliases: ["장소 검색", "지도 설정", "place-search", "map-config"]
paths:
  - "src/main/java/com/restroute/service/PlaceSearchService.java"
  - "src/main/java/com/restroute/controller/PlaceSearchController.java"
  - "src/main/java/com/restroute/controller/MapConfigController.java"
  - "src/main/java/com/restroute/controller/response/PlaceCandidateResponse.java"
  - "src/main/java/com/restroute/controller/response/MapConfigResponse.java"
  - "src/main/java/com/restroute/controller/HomeController.java"
  - "src/main/java/com/restroute/client/KakaoMapClient.java"
  - "src/main/java/com/restroute/client/response/KakaoLocalSearchResponse.java"
  - "src/main/java/com/restroute/client/exception/KakaoApiException.java"
  - "src/main/resources/static/js/place-search-request.js"
  - "src/main/resources/static/js/rest-stops-map.js"
related_domains: ["route"]
sources:
  - "f990383 feat: 목적지 검색 후보 선택 (place-search + 경로 좌표 수용 + 후보 모달)"
  - "d00eb57 feat: 지도 설정 API와 상세 패널 추가"
  - "fdc0e0e feat: 네이버 지도 기반 휴게소 위치 표시"
  - "2b1a011 feat: 경로상 휴게소 조회 백엔드 추가 (카카오 길찾기 연동)"
  - "c16b2ad feat: 경로 검색에 카카오 대안 경로(여러 경로 선택) 지원 추가"
  - "83f8de0 refactor: client 예외 패키지와 Feign 로깅 정리"
---

# place-search-and-map-config

## 1. 목적과 범위

이 도메인은 서로 다른 이유로 존재하는 두 개의 작은 기능을 묶은 것이다. (1) **장소 검색(place-search)**: 사용자가 목적지를 텍스트로 검색하면 카카오 로컬 검색 API로 후보(이름/주소/좌표) 목록을 반환해, 프론트가 후보 모달을 띄우고 사용자가 좌표를 고를 수 있게 한다. (2) **지도 설정(map-config)**: 프론트엔드가 네이버 지도(Naver Maps) SDK를 로드할 때 필요한 NCP 클라이언트 키를 서버에서 내려주는 설정 API다. 두 기능은 코드 경로도, 사용하는 지도 제공사(카카오 vs 네이버)도 다르며, "지도/장소" 관련 백엔드 진입점을 한 곳에 모아 문서화하기 위해 하나의 위키 도메인으로 묶었을 뿐 원래 하나의 기능 단위는 아니다.

## 2. 용어와 핵심 엔티티

- **PlaceCandidateResponse**: `name`, `address`, `latitude`, `longitude`로 이루어진 검색 후보 DTO. 별도 엔티티/테이블은 없음 — 카카오 응답을 그대로 변환해 반환하는 stateless 조회다.
- **MapConfigResponse**: `naverMapsNcpKeyId` 하나만 담는 DTO. 서버 설정값(`naver.maps.ncp-key-id`)을 그대로 노출한다.
- **KakaoMapClient**: 카카오 API(로컬 검색 + 길찾기) 호출을 캡슐화하는 공용 클라이언트(`@Component`). `searchKeyword(query)`와 `getDirections(origin, destination)` 두 메서드를 가지며, 내부적으로 `KakaoLocalFeignClient`/`KakaoNaviFeignClient`(Feign)를 감싼다.
- **KakaoLocalSearchResponse**: 카카오 로컬 검색 원본 응답 매핑. `Document`는 `x`(경도, 문자열), `y`(위도, 문자열), `place_name`, `address_name`을 가지며, `label()`은 `place_name`이 있으면 그것을, 없으면 `address_name`을 사용한다.

## 3. 사용자·시스템 흐름

**장소 검색:**
1. 사용자가 목적지 검색창에 키워드 입력 → 프론트가 `GET /api/place-search?query=`(부산역 등) 호출.
2. `PlaceSearchService`가 `KakaoMapClient.searchKeyword`로 카카오 로컬 검색 호출.
3. 응답 documents를 `PlaceCandidateResponse` 목록으로 변환. 좌표(x/y)를 숫자로 파싱할 수 없는 항목은 결과에서 제외(`filter(Objects::nonNull)`), 검색 결과 자체가 없으면 빈 리스트 반환(예외 아님).
4. 프론트가 후보 목록을 모달로 보여주고 사용자가 하나를 선택 → 선택된 좌표로 경로 검색 API(route 도메인) 호출. 이 흐름 도입 전에는 검색 결과 맨 위 항목을 자동으로 썼는데, 사용자가 직접 고르도록 UX를 바꾼 것이 f990383 커밋의 목적이다.

**지도 설정:**
1. 휴게소 위치를 지도에 표시하는 화면 로드 시 프론트(`rest-stops-map.js`)가 `GET /api/map-config` 호출.
2. `naverMapsNcpKeyId`가 비어 있으면(설정 안 됨) 지도 스크립트를 로드하지 않고 빠짐(코드 확인: `if (!mapConfig.naverMapsNcpKeyId) { ... }`로 가드).
3. 키가 있으면 그 키로 네이버 지도 SDK 스크립트를 동적 로드(`loadNaverMapsScript`)한 뒤 지도를 렌더링.

## 4. 정책과 불변 조건

- 장소 검색은 좌표가 없는 후보를 절대 반환하지 않는다(좌표 파싱 실패 시 그 후보만 제외, 전체 실패로 처리하지 않음).
- 검색 결과가 0건이어도 에러가 아니라 빈 리스트 — 프론트가 "결과 없음" UI를 스스로 처리해야 한다는 뜻.
- 카카오 API 실패는 `KakaoApiException`(→ 공통 `ExternalApiException` 상속)으로 감싸져 `GlobalExceptionHandler`가 처리한다(`src/main/java/com/restroute/common/GlobalExceptionHandler.java:36`). 즉 place-search는 카카오 응답 형식을 그대로 노출하지 않고 공통 에러 응답 포맷(`ApiResponse.error`)으로 변환해 내려준다.
- `KakaoMapClient`는 요청/성공/실패를 각각 `log.info`/`log.warn`으로 남긴다(83f8de0에서 정리) — 외부 API 로깅 규약이 이미 정해져 있다는 뜻이므로 새 외부 연동 추가 시 참고할 만한 기존 패턴.
- 지도 설정 키가 비어 있는 것은 오류가 아니라 정상적으로 다룰 수 있는 상태로 설계됨(빈 문자열 기본값 `@Value("${naver.maps.ncp-key-id:}")`) — 로컬/테스트 환경에서 지도 없이도 동작 가능하게 하려는 의도로 추정(추정 — 확인 필요, 커밋 메시지에 명시적 설명 없음).

## 5. 상태와 데이터 수명주기

두 기능 모두 자체 저장소나 캐시가 없는 stateless 프록시/설정 조회다.
- place-search: 매 요청마다 카카오 API를 직접 호출한다. 결과를 캐싱하는 로직은 없음(요청마다 외부 API 호출 발생 — 트래픽이 늘면 비용/레이트리밋 이슈가 될 수 있으나 현재 캐시 계층은 없다).
- map-config: 서버 기동 시 주입된 설정값을 매 요청 그대로 반환. DB 접근 없음.

## 6. UI·오류·권한 상태

- 두 API 모두 `SecurityConfig`상 `/api/**`에 포함되어 인증 없이 접근 가능(permitAll).
- place-search에서 카카오 API가 실패하면 프론트는 공통 에러 응답을 받는다 — 화면별로 재시도 UI가 있는지는 `place-search-request.js`/모달 관련 프론트 코드 확인이 필요(추정 — 확인 필요).
- map-config에서 키가 비어 있을 때 지도 자체를 렌더링하지 않는 것이 유일하게 확인된 프론트 방어 로직이며, 사용자에게 별도 오류 메시지를 보여주는지는 `rest-stops-map.js` 전체를 더 읽어야 확정 가능(추정 — 확인 필요).

## 7. 외부 시스템과 계약

- **카카오 로컬 검색 API** (`kakao.local.url`, `kakao.rest-api-key` 설정, 인증 헤더 `Authorization: KakaoAK {key}`): place-search가 사용. 응답의 `x`/`y`는 문자열이며 파싱 실패 시 후보에서 제외.
- **카카오 내비/길찾기 API** (`kakao.navi.url`): place-search가 아니라 route 도메인(`RouteResolverService`)이 `KakaoMapClient.getDirections`로 호출 — **동일한 `KakaoMapClient` 클래스를 두 도메인이 공유**하며, 이는 place-search 신규 개발 시 "기존 KakaoMapClient 재사용(신규 외부 연동 없음)"이라고 명시적으로 밝힌 설계 결정이다(f990383). 참고로 route 도메인의 `RouteResolverService`는 레거시 `destinationQuery` 파라미터 경로에서 `KakaoMapClient.searchKeyword`를 **직접** 호출하는 지점도 남아있어(place-search와 별개 호출 경로), 목적지 지오코딩 로직이 두 곳에 흩어져 있는 상태다.
- **네이버 지도(Naver Maps) SDK**: map-config가 클라이언트 키만 내려주고, 실제 지도 렌더링은 프론트에서 네이버가 제공하는 JS SDK를 동적 로드해 수행한다. 백엔드는 네이버 API를 직접 호출하지 않는다.
- 로컬 개발용 API 키(`naver.maps.ncp-key-id`, `kakao.rest-api-key`)가 `application-local.properties`에 평문으로 커밋되어 있음 — 로컬 전용 값으로 보이나 확인 필요(추정 — 확인 필요, 보안 관점 별도 검토 권장 사항이며 이 문서 범위 밖).

## 8. 코드 경계와 진입점

- **컨트롤러**: `controller/PlaceSearchController`(`GET /api/place-search?query=`), `controller/MapConfigController`(`GET /api/map-config`), `controller/HomeController`(`GET /` → `index` 뷰만 반환; map-config를 모델에 직접 주입하지는 않고 프론트가 별도로 `/api/map-config`를 fetch하는 구조).
- **서비스**: `service/PlaceSearchService`(카카오 응답 → `PlaceCandidateResponse` 변환). map-config는 별도 서비스 계층 없이 컨트롤러가 설정값을 바로 응답으로 감싼다.
- **클라이언트**: `client/KakaoMapClient`(+ 내부 Feign 클라이언트, `client/response/KakaoLocalSearchResponse`, `client/exception/KakaoApiException`) — route 도메인과 공유.
- **DTO**: `controller/response/PlaceCandidateResponse`, `controller/response/MapConfigResponse`.
- **프론트엔드**: `static/js/place-search-request.js`(장소 검색 API 호출 래퍼, 단위 테스트 존재), `static/js/rest-stops-map.js`(map-config 조회 + 네이버 지도 SDK 동적 로드).
- **테스트**: `src/test/java/com/restroute/controller/PlaceSearchControllerTest.java`, `src/test/java/com/restroute/service/PlaceSearchServiceTest.java`.

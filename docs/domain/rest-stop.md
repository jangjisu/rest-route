---
domain: rest-stop
aliases: ["휴게소", "rest-stop-core", "RestStop"]
paths:
  - "src/main/java/com/restroute/reststop/domain/**"
  - "src/main/java/com/restroute/reststop/repository/**"
  - "src/main/java/com/restroute/reststop/service/**"
  - "src/main/java/com/restroute/reststop/controller/**"
  - "src/main/java/com/restroute/reststop/scheduler/**"
related_domains: ["rest-stop-content", "oil-price", "ev-charger", "route", "finder"]
sources:
  - "DATA.md"
  - "DATA_LOOKUP_KEY_DESIGN.md"
  - "API.md"
  - "git log (RestStopEntity, RestStopImageEntity, RestStopServiceAreaCodeBackfillService 등)"
---

# rest-stop

## 1. 목적과 범위

고속도로 휴게소(REST_STOP) 자체의 정체성과 핵심 정보를 관리하는 도메인이다. 외부 공공 API(`data.ex.co.kr`)에서
휴게소 위치·상세·영업시설 정보를 동기화해 저장하고, 앱 내부 식별자(`service_area_code`)를 기준으로 다른 도메인이
휴게소를 참조할 수 있게 하는 "허브" 역할을 한다.

포함 범위: 휴게소 기본 식별 정보(위치·노선), 상세정보(연락처·브랜드·편의시설 Y/N), 고속도로 영업소 정보(주차대수 등),
대표 이미지(관리자 업로드), 매출 판매순위(상품/매장), 관리자에 의한 휴게소 생성·수정(`admin_overridden` 잠금 메커니즘).

제외 범위(각각 별도 도메인 문서로 다룸): 휴게소 콘텐츠(주유소 `rest_oil`/`rest_oil_price`, 먹거리 `rest_food`, 테마
`rest_theme`, 이벤트 `rest_event` — 이 문서에서는 rest-stop-content로 지칭), 오일 가격(oil-price), EV 충전기
(ev-charger), 경로 탐색·경로상 휴게소 추천(route). 이 도메인은 `RestStopRelatedInfoQueryService`/
`RestStopAggregateQueryService`를 통해 위 도메인들의 조회 결과를 "조합"만 하며, 각 도메인 자체의 동기화·정책은
소유하지 않는다.

## 2. 용어와 핵심 엔티티

- **REST_STOP** (`rest_stop` 테이블, `RestStopEntity`): 휴게소 1건. `service_area_code`가 앱 내부에서 이 휴게소를
  가리키는 기준 키다(추정이 아니라 `DATA_LOOKUP_KEY_DESIGN.md`에 명시된 설계 결정). 그 외 `unitCode`/`unitName`/
  `routeNo`/`routeName`/`xValue`/`yValue`/`stdRestCd`는 외부 API 원본 필드를 그대로 보관한다.
- **REST_STOP_DETAIL** (`rest_stop_detail`, `RestStopDetailEntity`): 휴게소 1건당 0~1건. 연락처·브랜드·주소·편의시설
  Y/N 플래그(`convenience`/`maintenanceYn`/`truckSaYn`)를 담는다. 원본 키는 `service_area_code`/`service_area_code2`
  /`routeCode`이고, `rest_stop_service_area_code`는 REST_STOP과 연결하기 위해 별도로 채우는 내부 조회 키다.
- **HIGHWAY_SERVICE_AREA_INFO** (`highway_service_area_info`): 휴게소 1건당 0건 이상(방향별로 여러 행 가능). 주차대수
  (일반/대형/장애인), 시설유형, 관할 지사 등 "영업소" 관점 정보. `business_facility_code`가 REST_STOP.service_area_code와
  자연키로 대응한다.
- **REST_STOP_IMAGE** (`rest_stop_image`, `RestStopImageEntity`): 관리자가 업로드하는 대표 이미지. PK가
  `service_area_code`라서 휴게소 1건당 정확히 0건 또는 1건만 존재한다.
- **REST_STOP_PRODUCT_SALES_RANK / REST_STOP_STORE_SALES_RANK**: 월별(baseYearMonth) 상품/매장 매출 순위. 관리자가
  CSV로 업로드하는 원본 데이터이며, 휴게소 이름으로 사후 매칭(`isMapped()`/`isUnmapped()`)해 `restStopServiceAreaCode`를
  채운다.
- **rest_stop_service_area_code**: REST_STOP_DETAIL·HIGHWAY_SERVICE_AREA_INFO·판매순위 두 테이블 모두에 존재하는
  "이 행이 앱의 어느 REST_STOP에 귀속되는지"를 나타내는 내부 조회 키. 원본 API의 `service_area_code`(원본 필드)와
  이름은 비슷하지만 의미가 다르므로 혼동 주의.
- **admin_overridden**: REST_STOP/REST_STOP_DETAIL에 있는 boolean(기본 false). 관리자가 직접 편집하면 true가 되어
  이후 자동 동기화(upsert)에서 이 행을 건너뛴다.
- **ADMIN- 접두 코드**: 외부 API에 없는 휴게소를 관리자가 직접 등록할 때 `service_area_code`/`unit_code`/`std_rest_cd`를
  전부 `"ADMIN-" + UUID`로 발급해 실제 API 자연키와 충돌하지 않게 한다.

## 3. 사용자·시스템 흐름

**동기화 흐름(시스템)**: `RestStopStartupInitializer`가 서버 기동 시 `rest_stop`/`rest_stop_detail` 테이블이 비어
있을 때만 최초 적재를 수행하고(`initializeXxxIfEmpty`), 이후 `RestStopScheduler`가 매일 자정(Asia/Seoul)
`RestStopSyncService`/`RestStopDetailSyncService`/`HighwayServiceAreaInfoSyncService`를 순서대로 호출해 전체 페이지를
다시 조회한 뒤 자연키(`service_area_code`) 기준 upsert로 갱신한다. 각 동기화는 실패해도 다른 동기화를 막지 않도록
스케줄러 레벨에서 개별 try/catch로 격리돼 있다. 동기화 마지막 단계로 `RestStopServiceAreaCodeBackfillService.backfill()`
을 호출해 REST_STOP_DETAIL/HIGHWAY_SERVICE_AREA_INFO/판매순위 등 여러 연관 테이블의 `rest_stop_service_area_code`를
한 번에 재계산한다.

**사용자 조회 흐름**: 목록(`GET /api/rest-stops`) → 검색(`GET /api/rest-stops/search?name=`) → 상세 진입 시
기본정보(`basic-info`)·영업시설(`facilities`)·이미지(`images/detail|list`)·판매순위(`sales-rankings`)를 화면 섹션별로
각각 별도 API로 호출한다(하나의 통합 응답으로 묶지 않는 설계). 이와 별도로 모바일 `/finder` 페이지의 "이름·거리로
찾기" 화면은 `GET /api/rest-stops/nearby` 하나로 목록+거리+배지 태그를 한 번에 받는다 — 이 엔드포인트의 상세 계약과
소비 화면은 [[finder]] 문서 소관.

**관리자 편집 흐름**: 관리자가 `GET .../editable`로 현재 값을 조회 → `PUT .../editable`로 저장(성공 시 두 엔티티 모두
`admin_overridden=true`로 전환) → 필요 시 `DELETE .../editable/override`로 다시 자동 동기화 대상으로 되돌린다. 외부
API에 없는 휴게소는 `POST /api/admin/rest-stops`로 신규 생성하며 생성 즉시 `admin_overridden=true`로 시작한다.
이미지는 별도로 `PUT/DELETE /api/admin/rest-stops/{code}/image`(multipart)로 관리한다.

**실패 종료 지점**: 존재하지 않는 `serviceAreaCode` 조회는 공개 API에서 `Optional.empty()` → 컨트롤러가 404로 변환,
관리자 API에서는 `RestStopNotFoundException`(404) 또는 `InvalidRestStopEditException`(400, 좌표 파싱 실패)으로 종료된다.

## 4. 정책과 불변 조건

- **service_area_code가 내부 조회 기준 키**: 다른 모든 연관 테이블은 이 코드로 REST_STOP을 참조한다
  (`DATA_LOOKUP_KEY_DESIGN.md`에 명시된 설계 결정, 추정 아님).
- **동기화는 삭제하지 않는다**: 외부 응답에서 사라진 행이 있어도 upsert만 하고 삭제는 하지 않는다(DATA.md "갱신 원칙"에
  명시). 삭제 대응은 별도 후속 과제로 남아 있다.
- **admin_overridden=true인 행은 자동 동기화에서 건너뛴다**: `RestStopSyncService.upsertOne`/
  `RestStopDetailSyncService.upsertOne`이 `existing.isSyncable()`(= `!adminOverridden`)을 확인하고, false일 때만
  `updateFrom`을 호출한다. 신규 행(existing == null)은 조건 없이 생성된다.
- **REST_STOP_IMAGE는 휴게소당 최대 1건**: PK가 `service_area_code` 자체이므로 재업로드는 upsert가 아니라 기존 행을
  대체(save)한다.
- **이미지 업로드 제약**: JPEG/PNG만 허용, 픽셀 수 3천만 이하, 상세용 최대 1600px·목록용 최대 480px 롱사이드로 축소해
  WebP(품질 0.80/0.75)로 재인코딩 후 원본은 보관하지 않는다. 이 규칙은 관리자 휴게소 이미지와 먹거리 메뉴 이미지가
  `RestStopImageProcessor`를 공유한다(추정 — RestStopFoodImage 쪽 소비 코드는 이 조사 범위 밖).
- **판매순위 매칭은 유일 매칭일 때만**: `RestStopUniqueNameMatcher.findUniqueServiceAreaCode`는 정규화한 이름이
  정확히 1개의 REST_STOP과만 일치할 때만 코드를 반환한다(동명이인 휴게소가 있으면 매칭하지 않고 unmapped로 남긴다).
- **판매순위 조회는 유효 순위만, 최신 월만, 상위 5건만**: `RestStopSalesRankingQueryService`가 rank가 양의 정수로
  파싱 가능하고 이름·월이 채워진 행만 걸러(`hasValidRank`), 매핑된 데이터 중 가장 최신 `baseYearMonth`를 골라 그 달의
  상위 5건만 반환한다.
- **좌표 값은 비어 있거나 실수(double)여야 한다**: 관리자 편집 시 `xValue`/`yValue`가 공백이 아니면 `Double.parseDouble`
  로 검증하고, 실패하면 400.
- **관리자 편집은 REST_STOP과 REST_STOP_DETAIL을 함께 잠근다**: 하나의 관리자 편집 요청이 두 엔티티의
  `admin_overridden`을 동시에 true로 바꾼다(REST_STOP_DETAIL 행이 없으면 빈 행을 만들어서까지 잠근다).

## 5. 상태와 데이터 수명주기

- **생성**: (a) 외부 API 동기화로 신규 자연키 발견 시 자동 생성, (b) 관리자가 `POST /api/admin/rest-stops`로 직접
  생성(`ADMIN-` 코드 발급, 즉시 잠금 상태로 시작).
- **갱신**: 매일 1회 전체 재조회 후 자연키 upsert(위치정보), 매일 1회 전체 재조회 후 자연키 upsert(상세/영업시설).
  `admin_overridden=true`인 행은 갱신에서 제외된다.
- **REST_STOP_DETAIL의 지연 생성 갭**: REST_STOP과 REST_STOP_DETAIL은 서로 다른 외부 API로 동기화되므로 시점이
  어긋나면 REST_STOP만 있고 DETAIL이 없는 상태가 일시적으로 존재할 수 있다. 관리자 편집 API는 이 경우
  `RestStopDetailEntity.createEmpty(serviceAreaCode)`로 빈 행을 만들어 저장한다.
- **backfill**: `RestStopServiceAreaCodeBackfillService.backfill()`이 매 동기화 사이클(자정 전체 동기화, 3시간 주기
  주유가격 동기화) 끝에 실행되어 REST_STOP_DETAIL/HIGHWAY_SERVICE_AREA_INFO/판매순위 등 여러 연관 테이블의
  `rest_stop_service_area_code`를 REST_STOP 목록 기준으로 재계산한다. 이 서비스 자체는 rest-stop 도메인 소유이지만
  내부적으로 rest-stop-content/ev-charger 여러 도메인의 backfiller를 오케스트레이션한다(그 개별 도메인들의 매칭 규칙
  자체는 이 문서 범위 밖).
- **잠금 해제**: `DELETE .../editable/override`로 `admin_overridden`을 false로 되돌리면 다음 동기화부터 다시 자동
  갱신 대상이 된다.
- **삭제**: 코드 조사 범위에서 REST_STOP/REST_STOP_DETAIL/HIGHWAY_SERVICE_AREA_INFO를 삭제하는 API나 배치는 발견하지
  못했다(이미지 삭제 API는 있음: `DELETE /api/admin/rest-stops/{code}/image`). 물리적 삭제 정책 자체가 "미결정 사항"으로
  DATA.md에 남아 있다 — 추정 아님, 문서화된 미결정 상태.
- **캐시**: 이미지 응답은 `ETag`(MD5) + `Cache-Control: public, no-cache`로 조건부 재검증 캐시를 사용한다(304 지원).
  그 외 엔드포인트에서 명시적 캐시 계층은 발견하지 못했다(추정 — 확인 필요, 상위 레이어의 HTTP 캐시/CDN 설정은 이
  조사 범위 밖).

## 6. UI·오류·권한 상태

- **공개 조회 API**(`/api/rest-stops/**`): 인증 불필요(추정 — SecurityConfig에서 `/api/admin/**`만 `ROLE_ADMIN`으로
  제한하는 것을 확인했고, 나머지 경로에 대한 명시적 permitAll 규칙까지는 이 조사에서 전부 추적하지 않음, 확인 필요).
  존재하지 않는 `serviceAreaCode`는 전부 `404 Not Found`(`ApiResponse.error(NOT_FOUND)` 바디 포함, 이미지 API만 예외로
  바디 없는 `204 No Content`).
- **관리자 API**(`/api/admin/rest-stops/**`): `SecurityConfig`에서 `hasRole("ADMIN")`으로 보호. 편집/생성 성공 시
  `AdminActivityLogService`로 활동 로그를 남긴다(생성/수정/이미지저장/이미지삭제/잠금해제 각각 별도 로그 메서드).
- **오류 상태**: `RestStopNotFoundException` → 404, `InvalidRestStopEditException`(좌표 파싱 실패) → 400,
  `InvalidRestStopImageException`(빈 파일/미지원 포맷/픽셀 초과) → 400. 모두 공통 `BusinessException` +
  `GlobalExceptionHandler`의 단일 핸들러로 `ResponseCode` 기반 매핑을 받는다.
- **빈 상태**: 판매순위는 매핑된 유효 데이터가 하나도 없으면 404가 아니라 `RestStopSalesRankingResponse.empty()`(빈
  본문의 200)를 반환한다 — 휴게소 자체는 존재하지만 순위 데이터가 없는 경우와, 휴게소가 없는 경우(404)를 구분하는
  설계.
- **이미지 없음**: 이미지가 없는 휴게소는 `basic-info.detailImageUrl`이 `null`이고, 이미지 엔드포인트 자체를 직접
  호출하면 `204 No Content`를 반환한다.

## 7. 외부 시스템과 계약

- **`data.ex.co.kr` 공공 API**(`ExApiClient` 경유): `getLocationInfoRest(pageNo)` → 위치정보(REST_STOP 원본),
  `getConvenienceServiceArea(pageNo)` → 상세/편의시설(REST_STOP_DETAIL 원본), `getHighwayServiceAreaInfoList()` →
  영업소 정보(페이지네이션 없이 1회 호출, HIGHWAY_SERVICE_AREA_INFO 원본). 개별 페이지 조회 실패는 `fetchPageSafely`가
  잡아서 로그만 남기고 `null` 처리 — 부분 실패 시 그 페이지만 누락되고 나머지는 정상 반영된다(HighwayServiceAreaInfo는
  단일 호출이라 이 안전장치가 없음).
- **관리자 CSV 업로드**(`SalesRankingUploadService`): `multipart/form-data`로 상품/매장 판매순위 CSV를 받아
  `SalesRankingCsvParser`로 파싱 후 자연키(월+휴게소코드+매장코드[+상품순번]) upsert. 원본 CSV 포맷 자체의 계약(컬럼
  순서 등)은 `SalesRankingCsvParser`(이 조사 범위 밖) 소유.
- **이미지 저장소**: 별도 오브젝트 스토리지 없이 DB `MEDIUMBLOB` 컬럼에 WebP 바이너리를 직접 저장한다(약 200장
  규모를 전제로 한 결정, DATA.md에 명시 — 원본 보관이나 더 큰 이미지 규모는 범위 밖으로 문서화됨).
- **route 도메인과의 결합**: `RouteOptionAssemblyService`(route 도메인)가 `RestStopAggregateQueryService`를 호출해
  EV차저/이미지/테마/이벤트/연관정보를 한 번에 조합받는다 — 예전에는 각 QueryService를 개별 호출했으나 리팩터링으로
  단일 진입점으로 교체됨(커밋 `94ab307`).
- **finder 도메인과의 결합**: `RestStopNearbyQueryService`(이 도메인 소유)가 `RestStopAggregateQueryService` +
  ev-charger/oil-price 조회를 조합해 `GET /api/rest-stops/nearby` 하나로 내려준다. 위치·이름·관심 항목(연료/EV)
  파라미터가 전부 optional이며, 값이 없으면 해당 출력 필드를 `null`로 내려 프런트가 있으면 표시/없으면 생략하는
  방식으로 처리한다(별도 API 분기 없음). 상세는 [[finder]] 문서.

## 8. 코드 경계와 진입점

- **entity**: `domain/RestStopEntity`, `RestStopDetailEntity`, `RestStopImageEntity`, `HighwayServiceAreaInfoEntity`,
  `RestStopProductSalesRankEntity`, `RestStopStoreSalesRankEntity`.
- **repository**: `repository/RestStopRepository`, `RestStopDetailRepository`, `RestStopImageRepository`,
  `HighwayServiceAreaInfoRepository`, `RestStopProductSalesRankRepository`, `RestStopStoreSalesRankRepository`.
- **동기화**: `service/RestStopSyncService`, `RestStopDetailSyncService`, `HighwayServiceAreaInfoSyncService`,
  `service/backfill/**`(9개 backfiller 중 이 도메인 소유는
  `RestStopDetailServiceAreaCodeBackfiller`/`HighwayServiceAreaInfoServiceAreaCodeBackfiller`/
  `RestStopProductSalesRankBackfiller`/`RestStopStoreSalesRankBackfiller`, 오케스트레이터는
  `RestStopServiceAreaCodeBackfillService`).
- **조회**: `service/RestStopQueryService`(기본 CRUD성 조회), `RestStopBasicInfoQueryService`,
  `RestStopFacilityQueryService`, `RestStopSalesRankingQueryService`, `RestStopRelatedInfoQueryService`(연관 도메인
  N+1 없이 배치 조합), `RestStopAggregateQueryService`(route 등 외부 소비자를 위한 최상위 조합).
  `service/image/RestStopImageQueryService`, `RestStopImageCommandService`.
- **근처 조회(finder 전용)**: `service/RestStopNearbyQueryService`, `service/dto/RestStopInterest`(EV/GASOLINE/DIESEL/
  LPG), `controller/response/RestStopNearbyItemResponse`. `RestStopController`가 `getRestStops`/`searchRestStops`
  (지도 화면용, 태그 없음)와 함께 `getNearbyRestStops`(`/nearby`, finder 화면용, 태그+거리 포함)를 소유 — 두 계열은
  서로 대체하지 않고 각자의 소비 화면을 가진 별개 엔드포인트다.
- **판매순위 업로드**: `service/salesranking/SalesRankingUploadService`, `util/SalesRankingRestStopNameNormalizer`.
- **관리자 편집**: `service/admin/AdminRestStopEditService`.
- **스케줄러/부트스트랩**: `scheduler/RestStopScheduler`(cron 등록), `scheduler/RestStopStartupInitializer`
  (`rest-stop.sync.startup-enabled` 프로퍼티로 on/off, 기본 true).
- **공개 진입점(Controller)**: `RestStopController`(목록/검색/상세), `RestStopBasicInfoController`,
  `RestStopFacilityController`, `RestStopImageController`, `RestStopSalesRankingController` — 전부
  `/api/rest-stops/**` 아래.
- **관리자 진입점**: `controller/admin/AdminRestStopEditController`, `AdminRestStopImageController` —
  `/api/admin/rest-stops/**` 아래, `ROLE_ADMIN` 필요.
- **DTO**: `service/dto/RestStopAggregate`, `RestStopRelatedInfo`(연관 도메인 조합 결과),
  `controller/response/RestStopItemResponse`, `RestStopDetailViewResponse`, `RestStopBasicInfoResponse`,
  `RestStopFacilityResponse`, `RestStopSalesRankingResponse`(+ Item/Store 하위 응답), `AdminRestStopEditableResponse`,
  `controller/request/AdminRestStopUpdateRequest`.

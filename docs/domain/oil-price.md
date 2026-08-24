---
domain: oil-price
aliases: ["유가", "주유소 편의시설", "주유소 가격", "전국 평균 유가"]
paths:
  - "src/main/java/com/restroute/domain/RestOilEntity.java"
  - "src/main/java/com/restroute/domain/RestOilPriceEntity.java"
  - "src/main/java/com/restroute/domain/NationalOilPriceEntity.java"
  - "src/main/java/com/restroute/service/RestOilSyncService.java"
  - "src/main/java/com/restroute/service/RestOilPriceSyncService.java"
  - "src/main/java/com/restroute/service/RestOilPriceRefreshService.java"
  - "src/main/java/com/restroute/service/RestStopOilInfoQueryService.java"
  - "src/main/java/com/restroute/service/NationalOilPriceService.java"
  - "src/main/java/com/restroute/service/admin/AdminRestOilLinkService.java"
  - "src/main/java/com/restroute/service/backfill/RestOilServiceAreaCodeBackfiller.java"
  - "src/main/java/com/restroute/service/backfill/RestOilPriceServiceAreaCodeBackfiller.java"
  - "src/main/java/com/restroute/controller/RestStopOilInfoController.java"
  - "src/main/java/com/restroute/controller/NationalOilPriceController.java"
  - "src/main/java/com/restroute/controller/admin/AdminRestOilLinkController.java"
related_domains: ["rest-stop", "rest-stop-content", "route", "admin"]
sources:
  - "git log --follow -- src/main/java/com/restroute/domain/RestOilEntity.java|RestOilPriceEntity.java|NationalOilPriceEntity.java"
  - "commit cef3d4c (관리자 휴게소-주유소 연결 점검·수정 기능, 상세 설명 포함), 55514bc (keep oil price mappings in sync), ae6c1fc (주유소 가격 갱신 TTL 추가), 29a6fab (오피넷 전국 평균 유가 백엔드 연동)"
  - "current source under src/main/java/com/restroute/{domain,service,controller}"
---

# oil-price

## 1. 목적과 범위

휴게소 내 주유소의 편의시설/가격 정보(rest_oil, rest_oil_price)와, 휴게소와 무관한 전국 평균 유가(national_oil_price)를 다룬다. EX API에서 두 개의 별도 데이터셋(편의시설 vs 실시간 가격)을 동기화하고, 오피넷(OPINET) API에서 전국 평균 유가를 별도로 가져온다. 휴게소-주유소 매칭이 자동 매칭만으로는 부정확할 수 있어 관리자가 직접 연결을 점검·수정하는 기능이 이 도메인의 핵심 정책 중 하나다.

## 2. 용어와 핵심 엔티티

- **RestOilEntity (`rest_oil`)**: EX API "주유소 편의시설" 목록. **물리적 주유소 1개당 여러 행**이 생길 수 있다 — 세차장, 쉼터 등 부대 편의시설 개수만큼(cef3d4c 커밋 설명). 자연키는 `standardRestCode + convenienceCode`. 자동 매칭용 키는 `routeCode + normalizedStationName`(정규화: "휴게소"/"주유소" 문자열 제거 + 공백 제거, `normalizeStationName`).
- **RestOilPriceEntity (`rest_oil_price`)**: EX API "실시간 주유소 가격" 목록. **휴게소당 정확히 0~1건** — 관리 화면에서 "주유소" 단위로 취급되는 것은 이쪽이다(cef3d4c 커밋 설명, 55514bc 이후로는 "관리자 링크의 source of truth"가 RestOilEntity로 재조정됨 — 아래 섹션 4 참고). 자연키는 `serviceAreaCode2`.
- **NationalOilPriceEntity (`national_oil_price`)**: 오피넷 전국 평균 유가. `(tradeDate, productCode)` 유니크 제약. 제품 코드: 휘발유 `B027`, 경유 `D047`, LPG `K015`(`NationalOilPriceService.Product` enum).
- **restStopServiceAreaCode**: RestOilEntity/RestOilPriceEntity 둘 다 갖는 휴게소 조회용 룩업 키. 자동 매칭(백필) 또는 관리자 수동 링크로 채워진다.
- **adminOverridden**: RestOilEntity에 있는 플래그(RestFoodEntity와 동일 패턴). true면 자동 매칭 배치가 이 행의 `restStopServiceAreaCode`를 건드리지 않는다.
- **serviceAreaCode2**: RestOilPriceEntity가 갖는, EX API 원본의 보조 식별자. `AdminRestOilLinkService`의 cascade 로직에서 "같은 물리적 주유소"를 찾는 키로 쓰인다(RestOilEntity의 `standardRestCode`와 매칭).

## 3. 사용자·시스템 흐름

**동기화 흐름**
- `RestOilSyncService`: EX API 편의시설 목록 전체를 자연키 upsert(750d167 이후, 이전엔 전체교체). 매일 자정 배치 + 기동 시 최초 1회.
- `RestOilPriceSyncService`: EX API 가격 정보를 최대 3페이지(`LAST_PAGE=3`) 수집. **모든 페이지 수집 성공 시에만 `deleteAllInBatch()` 후 전체 재삽입**, 일부 페이지 실패 시에는 `serviceAreaCode2` 기준 upsert로 폴백(부분 실패 시 데이터 유실 방지). 3시간마다(자정 포함) 추가로 도는 `syncRestOilPricesEveryThreeHours` 스케줄도 있음 — 가격은 변동성이 커서 별도로 자주 갱신.
- `NationalOilPriceService.getTodaySummary()`: 요청 시점에 오늘자 3개 제품 가격이 DB에 모두 있으면 바로 반환, 없으면 오피넷 API를 호출해 오늘자를 통째로 지우고 새로 저장 후 재조회(요청 트리거형 lazy sync, 스케줄러 없음 — 커밋 로그·스케줄러 파일에 관련 등록 없음을 확인).

**사용자 조회 흐름**
- `GET /api/rest-stops/{serviceAreaCode}/oil-info`: `RestStopOilInfoQueryService`가 `RestStopRelatedInfoQueryService`를 통해 편의시설 리스트 + 가격 정보를 모아 반환. `oilServiceAreaCode2`가 없으면(=주유소 매칭이 안 된 휴게소) 빈 결과.
- `POST /api/rest-stops/{serviceAreaCode}/oil-price/refresh`: 사용자가 명시적으로 최신 가격을 요청. `RestOilPriceRefreshService`가 **TTL 10분**(`REFRESH_TTL`) 이내 캐시가 있으면 그대로 반환하고, 지났으면 EX API 단건 조회(`getCurStateStationByServiceAreaCode2`)로 갱신.
- `GET /api/national-oil-prices/summary`: 전국 평균 유가 요약(경로 검색 결과 카드 등에서 사용, `RouteRestStopResponse.NationalOilPriceSummary`와 연결됨 — route 도메인과의 접점).

**관리자 연결 관리 흐름 (cef3d4c, 55514bc)**
1. 관리자가 `/admin/rest-stops/oil-links` 화면에서 전체 휴게소-주유소 매칭 현황을 표로 확인.
2. 이름/노선으로 주유소(RestOilPriceEntity 기준) 검색(`AdminRestOilLinkService.search`).
3. `link(oilPriceId, serviceAreaCode)`: 해당 RestOilPriceEntity에 코드를 연결하고, **같은 물리적 주유소를 공유하는 모든 RestOilEntity 행에도 cascade**로 `applyAdminLink` 적용(둘 다 `adminOverridden=true`).
4. `unlink`: 연결 해제도 명시적 오버라이드로 기록(`clearAdminLink`가 코드를 null로 지우면서 `adminOverridden=true`를 세팅 — "연결 안 함"도 관리자의 의도적 결정으로 취급되어 자동 매칭이 되살리지 못하게 함).
5. `clearOverride`: 오버라이드를 해제해 자동 매칭 배치가 다시 관리하도록 되돌림(`releaseToAutoMatching`).

## 4. 정책과 불변 조건

- **"주유소 1개" 단위는 rest_oil_price 기준**: 관리 UI와 사용자 노출 화면 모두 rest_oil_price를 하나의 주유소로 취급한다. rest_oil은 그 아래 딸린 편의시설 상세 목록일 뿐이다.
- **admin_overridden은 rest_oil과 rest_oil_price 양쪽에 동기화되어야 한다**: 55514bc 커밋에서 "rest_oil을 관리자 링크의 source of truth로 삼고, 매 동기화 후 파생된 가격 매핑을 재구성"하도록 바뀌었다 — 즉 이전에는 두 테이블의 오버라이드 상태가 어긋날 수 있는 버그가 있었고, 이를 고친 것(추정 — 확인 필요: 정확한 이전 버그 재현 조건은 diff만으로는 확실치 않음).
- **자동 매칭 배치는 오버라이드된 행을 건드리지 않는다**: `RestOilServiceAreaCodeBackfiller.backfill`은 `findByRestStopServiceAreaCodesAndAdminOverridden(null, false)` — 즉 `adminOverridden=false`인 행만 대상으로 한다.
- **가격 정보는 TTL 기반 신선도 정책을 갖는다**: `RestOilPriceEntity.lastRefreshedAt` + 10분 TTL. 배치 동기화(자정, 3시간 주기)와 별개로 사용자가 수동 새로고침을 트리거할 수 있다.
- **전국 평균 유가는 3개 제품이 모두 갖춰져야 "요약"으로 취급**: `hasTodayPrices`가 휘발유/경유/LPG 셋 다 있어야 true. 하나라도 빠지면 재조회를 시도하고, 그래도 안 되면 `Optional.empty()`(컨트롤러는 `503 EXTERNAL_API_UNAVAILABLE`로 응답).

## 5. 상태와 데이터 수명주기

- **생성/갱신**: RestOil/RestOilPrice는 자정 배치 + 기동 시 1회(비어있을 때만) + RestOilPrice는 추가로 3시간마다. NationalOilPrice는 요청 시 lazy 갱신(스케줄 없음 — 추정 확인 필요, HighwayServiceAreaInfoSyncService류처럼 스케줄러에 등록되어 있지 않음을 확인했으나 향후 배치화 계획 여부는 불명).
- **삭제**: RestOilPrice는 "모든 페이지 수집 성공" 시 `deleteAllInBatch()`로 전체 삭제 후 재삽입(사실상 풀 리프레시). NationalOilPrice는 같은 `tradeDate`의 기존 행을 지우고 재삽입.
- **restStopServiceAreaCode 백필**: `RestStopServiceAreaCodeBackfillService`(rest-stop 도메인 소유로 추정)가 매 동기화 후 8개 엔티티와 함께 이 도메인의 두 엔티티도 재계산한다. RestOilPrice 쪽 백필(`RestOilPriceServiceAreaCodeBackfiller`)은 RestOilEntity의 매핑 결과를 그대로 따라간다(oil이 먼저 확정되고, price는 oil의 매핑을 상속).
- **캐시**: RestOilPriceEntity 자체가 캐시 성격(TTL 10분)을 갖는 유일한 엔티티. 나머지는 매 요청 DB 조회.

## 6. UI·오류·권한 상태

- 휴게소가 없으면 `oil-info`, `oil-price/refresh` 둘 다 `404 NOT_FOUND`.
- 전국 평균 유가 API는 오피넷 응답이 파싱 실패하거나 예외가 나면 `log.warn` 후 `503 EXTERNAL_API_UNAVAILABLE` — 서버 에러로 요란하게 실패시키지 않고 조용히 폴백.
- 관리자 연결 API의 인증/인가 체크는 이 파일들 범위 밖(확인 필요).
- 관리자가 대상 주유소를 찾지 못하면 `RestOilNotFoundException`, 휴게소를 찾지 못하면 `RestStopNotFoundException`.

## 7. 외부 시스템과 계약

- **EX API**: `ExApiClient.getRestOilList()`(편의시설, 단일 응답), `ExApiClient.getCurStateStation(pageNo)`(가격, 최대 3페이지), `ExApiClient.getCurStateStationByServiceAreaCode2(code)`(단건 갱신용).
- **오피넷(OPINET) API**: `OpinetApiClient.getAverageOilPrices()` — 전국 평균 유가. 4207564 커밋 "오피넷 응답 파싱과 외부 API 로그 보강"에서 파싱 안정성이 개선됨(추정 — 구체적 파싱 버그 내용은 diff 확인 필요).
- 두 외부 API 모두 실패 시 예외를 잡아 로그만 남기고 빈 결과/기존 캐시로 폴백하는 방어적 패턴을 공유한다.

## 8. 코드 경계와 진입점

- **엔티티**: `com.restroute.domain.{RestOilEntity, RestOilPriceEntity, NationalOilPriceEntity}`
- **동기화**: `com.restroute.service.{RestOilSyncService, RestOilPriceSyncService}` — 스케줄러에서만 호출.
- **조회/갱신**: `com.restroute.service.{RestStopOilInfoQueryService, RestOilPriceRefreshService, NationalOilPriceService}`
- **관리자**: `com.restroute.service.admin.AdminRestOilLinkService`, 컨트롤러는 `com.restroute.controller.admin.AdminRestOilLinkController`(파일 존재 확인됨, 내부 라우팅/인가 로직은 미열람 — 확인 필요).
- **공개 API**: `RestStopOilInfoController`(`GET /oil-info`, `POST /oil-price/refresh`), `NationalOilPriceController`(`GET /api/national-oil-prices/summary`).
- **백필**: `com.restroute.service.backfill.{RestOilServiceAreaCodeBackfiller, RestOilPriceServiceAreaCodeBackfiller}`, 오케스트레이터는 `com.restroute.service.RestStopServiceAreaCodeBackfillService`(rest-stop 도메인 소유로 추정 — rest-stop-content, ev-charger 백필도 이 한 곳에서 함께 처리됨).
- **route 도메인과의 접점**: `RouteRestStopResponse.NationalOilPriceSummary`, `RouteOptionAssemblyService`(경로 옵션에 유가 정보를 얹는 지점 — 확인은 rest-stop-content 조사 중 grep으로만 확인, 상세 로직은 route 도메인 담당 조사자 참고).

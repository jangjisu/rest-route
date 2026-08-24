---
domain: rest-stop-content
aliases: ["휴게소 부가 콘텐츠", "테마휴게소", "휴게소 이벤트", "휴게소 먹거리"]
paths:
  - "src/main/java/com/restroute/domain/RestThemeEntity.java"
  - "src/main/java/com/restroute/domain/RestEventEntity.java"
  - "src/main/java/com/restroute/domain/RestFoodEntity.java"
  - "src/main/java/com/restroute/domain/RestFoodImageEntity.java"
  - "src/main/java/com/restroute/service/RestThemeSyncService.java"
  - "src/main/java/com/restroute/service/RestThemeQueryService.java"
  - "src/main/java/com/restroute/service/RestEventSyncService.java"
  - "src/main/java/com/restroute/service/RestStopEventQueryService.java"
  - "src/main/java/com/restroute/service/RestFoodSyncService.java"
  - "src/main/java/com/restroute/service/RestStopFoodMenuQueryService.java"
  - "src/main/java/com/restroute/service/admin/AdminRestFoodService.java"
  - "src/main/java/com/restroute/service/admin/AdminRestFoodImage*.java"
  - "src/main/java/com/restroute/service/backfill/RestThemeServiceAreaCodeBackfiller.java"
  - "src/main/java/com/restroute/service/backfill/RestEventServiceAreaCodeBackfiller.java"
  - "src/main/java/com/restroute/service/backfill/RestFoodServiceAreaCodeBackfiller.java"
  - "src/main/java/com/restroute/controller/RestStopEventController.java"
  - "src/main/java/com/restroute/controller/RestStopFoodController.java"
  - "src/main/java/com/restroute/controller/admin/AdminRestFoodController.java"
related_domains: ["rest-stop", "oil-price", "ev-charger"]
sources:
  - "git log --follow -- src/main/java/com/restroute/domain/RestThemeEntity.java|RestEventEntity.java|RestFoodEntity.java|RestFoodImageEntity.java"
  - "git log --follow -- src/main/java/com/restroute/service/RestThemeSyncService.java|RestThemeQueryService.java|RestEventSyncService.java|RestStopEventQueryService.java|RestFoodSyncService.java|RestStopFoodMenuQueryService.java"
  - "commit 4572b8f (테마휴게소·휴게소 이벤트 데이터 연동), c326370 (관리자 먹거리 직접 추가/수정), 88011fb (자연키 upsert 전환)"
  - "current source under src/main/java/com/restroute/{domain,service,controller}"
---

# rest-stop-content

## 1. 목적과 범위

휴게소 기본 정보(rest-stop 도메인)에 딸린 "부가 콘텐츠" 세 가지 — 테마휴게소(rest_theme), 휴게소 이벤트(rest_event), 휴게소 먹거리(rest_food/rest_food_image) — 를 다룬다. 세 가지 모두 EX(한국도로공사) Open API에서 주기적으로 동기화해 오는 것이 기본이며, 먹거리(rest_food)만 관리자가 직접 추가·수정할 수 있는 오버라이드 경로를 갖는다.

범위에 포함: 외부 API 동기화(전체 upsert), 휴게소별 조회(휴게소 상세 페이지에 붙는 이벤트/먹거리 패널), 지도·경로 카드에 노출되는 테마/이벤트 배지 판정, 관리자 먹거리 CRUD와 이미지 업로드.
범위에서 제외(추정 — 확인 필요): 테마·이벤트에는 관리자 오버라이드 기능이 없다 — 코드상 RestThemeEntity/RestEventEntity에 `adminOverridden` 필드나 관리자 컨트롤러가 없음을 확인했으나, 향후 계획 여부는 커밋 로그에 언급이 없다.

## 2. 용어와 핵심 엔티티

- **테마휴게소 (RestThemeEntity, 테이블 `rest_theme`)**: 특정 휴게소가 속한 테마(예: "포토존", "반려동물 동반") 목록. 자연키는 `stdRestCd + itemNm`.
- **휴게소 이벤트 (RestEventEntity, 테이블 `rest_event`)**: 휴게소별 기간제 이벤트. `stime`/`etime`(문자열, ISO_LOCAL_DATE 포맷)로 노출 기간을 가짐. 자연키는 `stdRestCd + eventSeq`.
- **휴게소 먹거리 (RestFoodEntity, 테이블 `rest_food`)**: 휴게소 대표/추천 메뉴. 자연키는 `stdRestCd + seq`. `seq`가 `"ADMIN-"` 접두사로 시작하면 관리자가 직접 만든 항목(`isAdminCreated()`), 아니면 동기화로 생성된 항목(`isSyncedFood()`).
- **먹거리 이미지 (RestFoodImageEntity, 테이블 `rest_food_image`)**: `foodId`를 PK로 공유하는 1:1 관계. 상세용/목록용 두 개의 MEDIUMBLOB(`detailImageData`, `listImageData`)을 갖는다.
- **adminOverridden**: RestFoodEntity에만 존재하는 플래그. true면 다음 동기화 배치가 이 행의 이름/가격/설명을 덮어쓰지 않는다(`isSyncable() == !adminOverridden`).
- **restStopServiceAreaCode**: 세 엔티티 모두 갖고 있는 "휴게소 조회용 룩업 키" 컬럼. EX API 원본 데이터에는 이 코드가 없어서, 별도 백필 배치(섹션 5 참고)가 `stdRestCd` 매핑을 통해 채워 넣는다.

## 3. 사용자·시스템 흐름

**동기화 흐름 (공통 패턴, 3개 엔티티 동일)**
1. 서버 기동 시 `RestStopStartupInitializer`가 테이블이 비어 있으면(`count() == 0`) 1회 전체 동기화(`initializeXxxIfEmpty`)를 수행한다.
2. 매일 자정(KST) `RestStopScheduler.syncRestStopsDaily()`가 먹거리→테마→이벤트 순으로 `refreshXxx()`를 호출한다.
3. `refreshXxx()`는 EX API에서 전체 목록을 가져와 자연키 기준으로 메모리 맵을 만들고, upsert(있으면 update, 없으면 insert) 후 `saveAll`한다.
4. 동기화 직후 `RestStopServiceAreaCodeBackfillService.backfill()`이 실행되어 `restStopServiceAreaCode`를 채운다(추정 — 확인 필요: 이 백필은 rest-stop 도메인 소유 서비스이며 8개 엔티티를 한 번에 처리하는 공유 배치임).

**사용자 조회 흐름**
- 휴게소 상세 화면에서 `GET /api/rest-stops/{serviceAreaCode}/events`, `/foods` 호출 → `RestStopEventQueryService`/`RestStopFoodMenuQueryService`가 `RestStopRelatedInfoQueryService`를 거쳐 관련 엔티티를 모아 응답.
- 이벤트는 조회 시점(`Clock` 기준 오늘 날짜)이 `stime`~`etime` 범위 안에 있는 것만 "활성"으로 걸러 반환한다(`RestStopEventQueryService.isActiveOn`).
- 지도/경로 카드의 테마·이벤트 배지: `RestThemeQueryService.findThemeMappedServiceAreaCodes` / `RestStopEventQueryService.findActiveEventMappedServiceAreaCodes`가 여러 서비스area코드를 한 번에 받아 배지를 붙일 코드 목록만 돌려준다(커밋 700f54c "지도·경로 카드에 테마·이벤트 배지 노출").

**관리자 먹거리 관리 흐름 (커밋 c326370)**
1. 관리자가 특정 휴게소에서 먹거리 항목을 생성(`AdminRestFoodService.create`) → `RestFoodEntity.createByAdmin`으로 `seq="ADMIN-"+UUID`, `adminOverridden=true`인 엔티티 생성.
2. 기존(동기화된) 항목을 수정(`update`) → `applyAdminEdit`으로 이름/가격/설명만 덮어쓰고 `adminOverridden=true`로 전환.
3. `clearOverride` → `adminOverridden=false`로 되돌려 다음 동기화가 다시 값을 갱신하도록 허용.
4. 삭제(`delete`)는 관리자가 만든 항목(`isAdminCreated()`)만 허용 — 동기화 원본 항목(`isSyncedFood()`)을 삭제하려 하면 `InvalidRestFoodEditException`.

## 4. 정책과 불변 조건

- **동기화는 admin 오버라이드를 침범하지 않는다**: `RestFoodSyncService.upsertOne`은 기존 항목이 있어도 `existing.isSyncable()`(=`!adminOverridden`)일 때만 `updateFrom`을 호출한다. 관리자가 수정한 값은 다음 배치가 지나가도 유지된다.
- **동기화 원본 항목은 삭제 불가**: `isSyncedFood()`인 RestFoodEntity는 관리자가 삭제할 수 없다(정합성상 원본이 없어지면 다음 동기화 때 재생성되어 혼란을 줄 수 있기 때문으로 추정 — 확인 필요).
- **이벤트 활성 판정은 문자열 날짜 파싱 실패 시 비활성 처리**: `isActiveOn`에서 `DateTimeParseException` 발생 시 `false`를 반환 — 잘못된 날짜 포맷의 이벤트는 조용히 숨겨진다(예외를 던지지 않음).
- **자연키 upsert 전환**: 원래는 "전체 교체"(delete-then-insert) 방식이었으나 88011fb 커밋에서 자연키(`stdRestCd`+식별자) 기준 upsert로 전환됨 — PK(surrogate id)가 바뀌지 않아야 이미지 등 연관 데이터(FK)가 끊기지 않기 때문으로 추정(RestFoodImageEntity가 `foodId`를 그대로 PK/FK로 쓰므로).
- **같은 배치 내 자연키 중복은 병합**: b32a773 "먹거리 upsert에서 같은 배치 내 자연키 중복 시 병합" — `Collectors.toMap`의 병합 함수(`(first, second) -> first`)로 처리.

## 5. 상태와 데이터 수명주기

- **생성**: EX API 동기화(자동) 또는 관리자 생성(먹거리만, 수동).
- **갱신**: 매일 정기 배치(자정) + 서버 기동 시 최초 1회(비어있을 때만) + 관리자 수동 수정(먹거리만).
- **삭제**: 관리자가 만든 먹거리 항목만 하드 삭제 가능. 테마/이벤트는 애플리케이션 레벨의 삭제 경로가 보이지 않음(EX API가 더 이상 특정 항목을 내려주지 않아도 upsert 로직은 기존 행을 지우지 않고 그대로 둔다 — 추정 — 확인 필요: stale 데이터 정리 배치가 없어 보임).
- **캐시**: 별도 캐시 계층 없음. 조회는 매번 리포지토리 접근(`@Transactional(readOnly = true)`).
- **동기화 상태 로그**: `RestStopScheduler`/`RestStopStartupInitializer`가 각 단계 성공/실패를 `log.info`/`log.error`로 남기고, 실패해도 다음 단계로 계속 진행(개별 try-catch로 격리).
- **restStopServiceAreaCode 백필**: 동기화 직후 `RestStopServiceAreaCodeBackfillService`가 8개 엔티티(테마·이벤트·먹거리 포함)를 한 번에 재계산한다. 이 서비스는 rest-stop 도메인이 소유하는 것으로 보이며, ev-charger 매핑 재계산도 여기서 함께 이뤄진다(관련 도메인 간 공유 배치).

## 6. UI·오류·권한 상태

- 조회 API(`/events`, `/foods`)는 휴게소가 존재하지 않으면 `404 NOT_FOUND`(`ApiResponse.error`)를 반환한다. 이벤트/먹거리가 0건인 경우와 휴게소 자체가 없는 경우를 구분하는지는 확인 필요 — 코드상 `Optional<RestStop>`이 비었을 때만 404이고, 관련 정보가 없으면 빈 리스트를 담은 200 응답으로 보인다(추정 — 확인 필요).
- 관리자 CRUD는 `RestFoodNotFoundException`(대상 없음), `InvalidRestFoodEditException`(동기화 항목 삭제 시도) 등 도메인 예외를 던진다. 이 예외들이 어떤 HTTP 상태로 매핑되는지는 GlobalExceptionHandler 확인이 필요(이 조사 범위 밖).
- 권한 체크(관리자 인증/인가) 로직은 이 파일들 범위 밖 — 별도 admin 인증 도메인에 있을 것으로 추정(확인 필요).

## 7. 외부 시스템과 계약

- **EX API (한국도로공사 Open API)**: `ExApiClient.getRestThemeList()`, `getRestEventList()`, `getRestBestfoodList(pageNo)` — 세 개의 별도 엔드포인트. 먹거리만 페이지네이션(`pageNo` 파라미터, `getPageSize()`로 총 페이지 수 확인)이 있고, 테마·이벤트는 단일 응답으로 전체 리스트를 받는다.
- 응답이 `null` 리스트를 줄 수 있어 각 서비스가 `List.of()`로 방어한다.
- 먹거리 동기화는 페이지 단위로 실패를 허용(`fetchPageSafely`가 예외를 잡아 `null` 반환, 해당 페이지만 스킵) — 커밋 d83ff14 "EX API 페이지 수집 부분 성공 처리".

## 8. 코드 경계와 진입점

- **엔티티**: `com.restroute.domain.{RestThemeEntity, RestEventEntity, RestFoodEntity, RestFoodImageEntity}`
- **동기화 서비스**: `com.restroute.service.{RestThemeSyncService, RestEventSyncService, RestFoodSyncService}` — 스케줄러(`RestStopScheduler`, `RestStopStartupInitializer`)에서만 호출됨.
- **조회 서비스**: `com.restroute.service.{RestThemeQueryService, RestStopEventQueryService, RestStopFoodMenuQueryService}` — 각각 배지용 벌크 조회와 휴게소 단건 조회 책임을 겸함.
- **관리자 서비스**: `com.restroute.service.admin.{AdminRestFoodService, AdminRestFoodImageCommandService, AdminRestFoodImageQueryService}`
- **컨트롤러(공개 API)**: `RestStopEventController`(`GET /api/rest-stops/{code}/events`), `RestStopFoodController`(`GET /api/rest-stops/{code}/foods`) — 둘 다 00dacad "휴게소 기능별 API 컨트롤러 분리" 커밋으로 큰 컨트롤러에서 쪼개져 나옴.
- **컨트롤러(관리자 API)**: `com.restroute.controller.admin.AdminRestFoodController`, `AdminRestFoodImageController`(경로 미확인 — 확인 필요).
- **공유 조합 지점**: `RestStopRelatedInfoQueryService`(rest-stop 도메인 소유로 추정)와 `RestStopRelatedInfo` DTO가 테마/이벤트/먹거리/오일/디테일을 한 번에 모아 각 도메인 쿼리 서비스에 제공한다 — rest-stop-content는 이 조합 지점의 소비자다.

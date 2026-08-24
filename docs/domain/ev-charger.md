---
domain: ev-charger
aliases: ["전기차 충전소", "EV 충전기", "충전소 매핑"]
paths:
  - "src/main/java/com/restroute/domain/EvChargerEntity.java"
  - "src/main/java/com/restroute/domain/EvChargerStationMappingEntity.java"
  - "src/main/java/com/restroute/service/evcharger/**"
  - "src/main/java/com/restroute/client/EvChargerApiClient.java"
  - "src/main/java/com/restroute/client/response/EvChargerItem.java"
  - "src/main/java/com/restroute/repository/EvChargerRepository.java"
  - "src/main/java/com/restroute/repository/EvChargerStationMappingRepository.java"
related_domains: ["rest-stop", "route"]
sources:
  - "git log --follow -- src/main/java/com/restroute/domain/EvChargerEntity.java|EvChargerStationMappingEntity.java"
  - "commit 12ffe55 (sync ev chargers and map rest stops, 최초 도입), 458f244 (calculate ev charger station mappings), 75848c0 (integrate ev charger mapping into backfill), 7b21918 (simplify ev charger station mapping), 952f565 (clarify EV charger matching predicates)"
  - "current source under src/main/java/com/restroute/{domain,service/evcharger,client}"
---

# ev-charger

## 1. 목적과 범위

전국 전기차 충전소 공공 API(공공데이터포털 계열로 추정 — 확인 필요, 클라이언트명은 `EvChargerApiClient`)에서 충전기 목록을 동기화하고, 그중 "고속도로 휴게소"에 위치한 충전기만 걸러내 좌표/이름/주소 기반으로 휴게소와 매칭시키는 도메인이다. 12ffe55 커밋("sync ev chargers and map rest stops")으로 처음 도입되었고 이후 리팩토링을 여러 차례 거쳤다(단순화, 매칭 조건 명확화, 백필 통합).

범위: 원본 충전기 데이터 동기화, 휴게소-충전소 매핑 계산, 매핑 결과 기반 조회(배지·개수). 범위 밖(확인됨): 관리자가 매핑을 수동으로 교정하는 기능이 없다 — oil-price/rest-stop-content 도메인과 달리 `adminOverridden` 필드나 admin 컨트롤러/서비스가 이 도메인에는 전혀 없다(grep으로 `service/admin`, `controller/admin` 하위에 EvCharger 관련 파일 없음을 확인).

## 2. 용어와 핵심 엔티티

- **EvChargerEntity (`ev_charger`)**: 공공 API 원본 충전기 데이터. 유니크 키는 `(stat_id, chger_id)` — 하나의 충전소(`statId`)에 여러 충전기(`chgerId`)가 있을 수 있는 구조. 필드가 매우 많고(40여 개) 대부분 원본 API 응답을 그대로 저장하는 성격(위치, 상태, 커넥터 타입, 사업자 정보 등).
- **EvChargerStationMappingEntity (`ev_charger_station_mapping`)**: 충전소 단위(`statId`, 유니크)로 휴게소와의 매칭 결과만 저장하는 파생 테이블. `restStopServiceAreaCode` 하나만 갖는 얇은 매핑 테이블 — RestOilEntity/RestFoodEntity처럼 원본 데이터에 `restStopServiceAreaCode` 컬럼을 직접 추가하는 대신, **충전기(EvChargerEntity)와 매핑(EvChargerStationMappingEntity)을 별도 테이블로 분리**한 것이 이 도메인의 특징(추정 — 확인 필요: 명시적 이유가 담긴 커밋 메시지는 찾지 못했으나, 충전기는 station당 여러 행이라 매핑을 충전소 단위로 별도 정규화한 것으로 보인다).
- **canBeMappedToRestStop()**: `statId`가 있고 `delYn == "N"`(삭제되지 않음)인 충전기만 매핑 대상.
- **고속도로 휴게소 필터**: `EvChargerItem.isHighwayRestStop()` — `kindDetail`이 특정 상수(`HIGHWAY_REST_STOP_KIND_DETAIL`)와 일치하는지로 판별. 동기화 단계에서부터 이 필터를 적용해 고속도로 휴게소 충전기만 저장한다(전국 충전기 전부를 저장하지 않음).
- **MAX_MATCH_DISTANCE_METERS = 300**: 충전기 좌표와 휴게소 좌표 사이 최대 매칭 허용 거리(300m).

## 3. 사용자·시스템 흐름

**동기화 흐름**
1. 서버 기동 시 `EvChargerSyncService.initializeEvChargersIfEmpty()`(테이블이 비었을 때만) + 매일 자정 배치(`refreshEvChargers`).
2. `EvChargerApiClient.getChargerInfo(pageNo)`로 페이지 단위 수집. 각 페이지 응답에서 `isHighwayRestStop()` && `hasChargerIdentity()`(statId, chgerId 둘 다 있음)를 만족하는 항목만 골라낸다.
3. 페이지 하나가 실패해도 나머지 페이지는 계속 수집(`failedPageCount`로 집계, 전체를 막지 않음).
4. 모든 페이지가 실패해 수집된 항목이 0건이면 저장을 건너뛰고 경고 로그만 남긴다(EvChargerSyncResult로 실패/성공 페이지 수, 저장 건수, 고유 충전소 수를 리턴).
5. 자연키(`statId`+`chgerId`) upsert로 저장.

**매핑 계산 흐름 (EvChargerStationMappingCalculator)**
1. 매핑 대상 충전기를 `statId` 기준으로 중복 제거(`distinctActiveChargers`).
2. 각 충전기에 대해 휴게소 좌표와의 거리를 계산(Haversine 공식, `CoordinateDistanceCalculator`)해 300m 이내 후보만 거리순 정렬.
3. 가까운 순서대로 순회하며, **이름 일치** 또는 **주소 일치**(휴게소 상세 정보의 주소와 정규화 비교) 중 하나라도 맞는 첫 번째 후보를 매칭 대상으로 채택(`findFirst`) — 즉 "가장 가까우면서 이름/주소가 맞는" 것을 고르는 게 아니라 "가까운 순으로 보다가 처음 맞는 것"을 고른다(거리 우선, 조건은 보조).
4. 이름 정규화: 공백 제거 + "휴게소" 문자열 제거 + 한글/숫자/괄호 외 문자 제거. 주소 정규화: 공백 제거 + 한글/숫자 외 문자 제거.
5. 매칭되면 `EvChargerStationMappingEntity`를 생성해 저장(전체 재계산 방식 — 아래 섹션 5).

**조회 흐름**
- `EvChargerQueryService.findChargerMappedServiceAreaCodes`: 여러 서비스area코드를 받아 매핑된 것만 필터(지도/경로 카드 배지용, rest-stop-content의 테마/이벤트 배지와 동일한 패턴).
- `EvChargerQueryService.findActiveChargerCount`: 특정 휴게소에 매핑된 충전소들의 `statId`를 모은 뒤, `delYn == "N"`인 활성 충전기 개수를 센다(휴게소 상세 화면의 "충전기 N대" 표시용으로 추정).

## 4. 정책과 불변 조건

- **고속도로 휴게소가 아닌 충전기는 애초에 저장하지 않는다**: 필터링이 조회 시점이 아니라 동기화(수집) 시점에 이뤄진다 — DB에는 고속도로 휴게소 충전기만 존재.
- **매칭은 이름 또는 주소 중 하나만 일치해도 성립**(AND가 아니라 OR) — 이름이 다르더라도 주소가 같으면 매칭되고, 그 반대도 성립.
- **매칭 거리 상한 300m**는 하드코딩 상수(`EvChargerStationMappingCalculator.MAX_MATCH_DISTANCE_METERS`) — 설정 파일로 외부화되어 있지 않음.
- **매핑에는 관리자 개입 지점이 없다** — 자동 계산 결과가 그대로 최종 상태가 된다. oil-price/rest-stop-content와 달리 오검출/누락을 수동으로 고칠 방법이 코드상 보이지 않음(추정 — 확인 필요: 향후 필요성이 논의됐는지는 커밋 로그에 없음).
- **활성 충전기 판정은 `delYn == "N"`**: 매핑 테이블 자체에는 삭제 여부가 없고, 매핑된 `statId`로 다시 `EvChargerEntity`를 조회해 `delYn`을 확인하는 2단계 구조.

## 5. 상태와 데이터 수명주기

- **EvChargerEntity**: 자정 배치 + 기동 시 1회(비어있을 때만) upsert. 명시적 삭제 로직 없음 — 원본 API가 `delYn`을 "Y"로 내려주면 그 값 그대로 갱신되어 저장되고, 조회 쪽(`findActiveChargerCount`)에서 `delYn == "N"` 필터로 걸러낸다(soft-delete를 원본 API에 위임).
- **EvChargerStationMappingEntity**: **매번 전체 재계산 방식** — `RestStopServiceAreaCodeBackfillService.backfillEvChargerMappings`가 `evChargerStationMappingRepository.deleteAllInBatch()`로 기존 매핑을 통째로 지운 뒤 `EvChargerStationMappingCalculator.calculate()` 결과를 다시 `saveAll`한다(75848c0 "integrate ev charger mapping into backfill" 이후 rest-stop 도메인의 공유 백필 오케스트레이터에 통합됨). 이는 rest-stop-content/oil-price의 "기존 행을 찾아서 update" 방식과 다른 패턴 — 매핑 결과에 안정적인 자연키/PK 의존성이 없어서 통째로 갈아엎어도 안전하기 때문으로 추정.
- **캐시**: 없음. 매 조회가 리포지토리 직접 조회.

## 6. UI·오류·권한 상태

- 이 도메인 자체에는 전용 컨트롤러가 없다(grep 결과 `controller` 패키지에 EvCharger 전용 REST 엔드포인트 없음). 대신 `RestStopBasicInfoQueryService`, `RestStopAggregateQueryService`(rest-stop 도메인 소유로 추정), `RouteOptionAssemblyService`(route 도메인)가 `EvChargerQueryService`를 내부적으로 호출해 다른 응답(휴게소 기본 정보, 경로 옵션)에 충전기 정보를 얹는 구조 — 즉 ev-charger는 "합성 정보 제공자"이지 자체 API 표면을 갖지 않는다(확인 필요: 향후 전용 API가 추가될 수도 있음).
- 동기화 실패(전체 페이지 실패)는 조용히 스킵되고 경고 로그만 남는다 — 사용자에게 노출되는 에러는 없음(이전 동기화 결과가 유지됨).

## 7. 외부 시스템과 계약

- **EvChargerApiClient / EvChargerFeignClient**: `getChargerInfo(pageNo)` 페이지네이션 API. 12ffe55 커밋에서 함께 도입된 `EvChargerFeignClient`, `EvChargerResponse`, `EvChargerItem`이 계약을 정의(어떤 공공 API인지 — 예: 환경부 전기차충전소 정보 — 는 코드/커밋에 명시되어 있지 않아 확인 필요).
- 응답에는 `totalPageCount`, `totalCount`가 포함되어 페이지네이션 종료 조건으로 쓰인다.

## 8. 코드 경계와 진입점

- **엔티티**: `com.restroute.domain.{EvChargerEntity, EvChargerStationMappingEntity}`
- **서비스 패키지**: `com.restroute.service.evcharger` — `EvChargerSyncService`(동기화), `EvChargerQueryService`(조회), `mapping.EvChargerStationMappingCalculator`(매칭 알고리즘), `util.CoordinateDistanceCalculator`(순수 Haversine 계산, 부수효과 없음), `dto.{EvChargerSyncResult, EvChargerFetchSummary, EvChargerCoordinates}`.
- **진입점**: 동기화는 `RestStopScheduler`/`RestStopStartupInitializer`에서만 트리거. 매핑 재계산은 `RestStopServiceAreaCodeBackfillService.backfillEvChargerMappings`(rest-stop 도메인 소유로 추정)를 통해서만 실행되며, `EvChargerStationMappingCalculator`를 직접 호출하는 다른 진입점은 없음.
- **소비자(다른 도메인)**: `RestStopBasicInfoQueryService`, `RestStopAggregateQueryService`(rest-stop), `RouteOptionAssemblyService`, `RouteRestStopResponse`(route) — 이 도메인은 자체 컨트롤러 없이 두 도메인에 데이터를 공급하는 역할.

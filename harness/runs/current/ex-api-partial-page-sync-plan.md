## 작업 목표

EX API 페이지 단위 동기화에서 일부 페이지 호출이 실패해도 성공한 페이지의 데이터는 저장하도록 개선한다.
스케줄러와 시작 초기화기의 항목별 catch 구조는 유지하고, 실패 허용 범위는 SyncService 내부의 페이지 fetch 루프에 한정한다.

## 대상 범위

- `RestStopSyncService`
- `RestStopDetailSyncService`
- `RestFoodSyncService`
- `RestOilPriceSyncService`
- 각 SyncService 테스트

이번 작업에서 `RestOilSyncService`, `HighwayServiceAreaInfoSyncService`는 단일 호출 API라 제외한다.
Controller, 화면, route/detail API 응답 구조, DB schema 변경도 제외한다.

## 스펙 영향도 확인

- 내부 API 스펙: unchanged
- 외부 API 연동 스펙: unchanged
- 변경 없음 판단 이유: endpoint, request parameter, response DTO, public error response는 바꾸지 않고, 기존 EX API 호출 실패를 SyncService 내부에서 페이지 단위로 다루는 저장 정책만 바꾼다.
- 데이터 동기화 정책 영향: 일부 페이지 실패 시 성공 페이지만 반영하고, 전체 실패 또는 첫 페이지 실패로 페이지 수를 알 수 없는 경우 기존 데이터를 유지한다.

## 접근 방법과 trade-off

### 선택한 접근

각 SyncService의 페이지 fetch 루프에서 페이지별 예외를 잡아 warn 로그를 남기고 계속 진행한다. 1페이지가 실패해 total page count를 알 수 없는 `RestStopSyncService`, `RestStopDetailSyncService`, `RestFoodSyncService`는 저장 없이 0을 반환해 기존 데이터를 유지한다.

`RestOilPriceSyncService`는 전체 페이지가 성공하면 기존 replace 정책을 유지한다. 일부 페이지가 실패하면 `deleteAllInBatch()`를 실행하지 않고 성공 페이지의 row만 `serviceAreaCode2` 기준으로 upsert한다.

### 대안

- 스케줄러/초기화기 catch만 유지: 현재 문제처럼 SyncService 내부에서 한 페이지 예외가 전체 저장을 무산시키므로 부적합하다.
- 모든 동기화를 upsert로 통일: 주유 가격의 기존 전체 교체 의미가 사라지고 변경 범위가 커진다.
- 공통 paginator 추상화 도입: 중복은 줄일 수 있지만 API별 첫 페이지/고정 페이지/replace 정책이 달라 이번 작업에는 과하다.

### 선택 이유

요청한 부분 성공 동작을 가장 작게 구현하면서 기존 트랜잭션 경계(fetch 후 DB 쓰기)를 유지한다. 특히 주유 가격은 부분 실패 시 기존 정상 데이터를 삭제하지 않는 것이 핵심이므로 전체 성공과 부분 성공 저장 경로를 분리한다.

## 구현 계획

1. 테스트를 먼저 추가한다.
   - 휴게소 위치/상세/음식: 중간 페이지 실패 시 성공 페이지 데이터가 저장되는지 검증한다.
   - 휴게소 위치/상세/음식: 1페이지 실패 시 예외를 밖으로 던지지 않고 저장 없이 0을 반환하는지 검증한다.
   - 주유 가격: 일부 페이지 실패 시 기존 데이터 삭제 없이 성공 페이지 데이터만 저장/upsert하는지 검증한다.
   - 주유 가격: 전체 페이지 성공 시 기존 replace 동작이 유지되는지 기존 테스트로 확인한다.
2. 각 SyncService에 페이지 단위 안전 fetch를 추가한다.
3. 실패 페이지는 `log.warn`으로 `pageNo`와 원인을 기록한다.
4. 전체 테스트와 하네스 검증을 실행한다.

## 역할 리뷰 판단

- 개발팀장 리뷰: 필요. 동기화 실패 처리와 데이터 보존 정책 변경이 있다.
- CEO 리뷰: 불필요. 제품 화면이나 사용자-facing 범위 변경이 없다.
- DevEx 리뷰: 불필요. public API, 라이브러리, 하네스, 개발자 인터페이스 변경이 없다.
- 보안 리뷰: 불필요. 인증/비밀키/권한/외부 입력 경계 변경이 없다.
- Design 리뷰: 불필요. 화면 구조나 사용자 흐름 변경이 없다.

## 검증 계획

- 관련 SyncService 테스트를 먼저 RED/GREEN으로 실행한다.
- `./gradlew test` 전체 테스트를 실행한다.
- `harness/harness.sh verify code`
- `harness/harness.sh verify verify`
- `harness/harness.sh verify commit`
- 커밋까지 진행한다.

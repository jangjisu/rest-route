# 한국도로공사 대표 음식 API 연동 계획

## 작업 목표

기존 `EX_API_KEY`와 `data.ex.co.kr`의 `representFoodServiceArea` API를 호출할 수 있도록
외부 응답 VO와 `ExApiClient` 계층만 추가한다. 이번 단계에서는 Entity, Repository, Scheduler,
backfill, 내부 경로 응답과 프론트엔드를 변경하지 않는다.

## 범위

- 카테고리: API 연동
- 외부 endpoint: `/openapi/business/representFoodServiceArea`
- 요청 포맷: 기존 `type=json`, `numOfRows=99`, `pageNo` 패턴 재사용
- 인증: 기존 `ex.api.key` / `EX_API_KEY` 재사용
- 제외: `API.md`를 포함한 문서 변경은 사용자 요청에 따라 후속 작업으로 보류한다.

## 구현 단위

### 1. 대표 음식 응답 계약

다음 파일을 추가한다.

- `src/main/java/com/restroute/client/response/RepresentativeFoodItem.java`
- `src/main/java/com/restroute/client/response/RepresentativeFoodResponse.java`

`RepresentativeFoodResponse`는 `ExApiResponse`를 구현하고 `SUCCESS` 여부와 최상위 `message`를
기존 응답 VO와 동일하게 처리한다. `list` 항목은 `serviceAreaCode`, `serviceAreaCode2`,
`serviceAreaName`, `routeCode`, `routeName`, `direction`, `batchMenu`, `salePrice`를 원본
문자열로 보존한다. 전체 조회에서 대표 음식·가격·방향이 누락된 행이 확인되었으므로 해당 필드는
nullable로 둔다.

### 2. ExApiClient 연동

다음 파일을 수정한다.

- `src/main/java/com/restroute/client/ExApiFeignClient.java`
- `src/main/java/com/restroute/client/ExApiClient.java`

endpoint 상수와 페이지 단위 메서드를 추가한다. `requestUrl` 생성, JSON 포맷, 인증키,
예외 변환과 키 마스킹 로그는 기존 메서드 패턴을 재사용한다. 동기화 계층이 전체 페이지를
순회할 수 있도록 `pageNo`와 응답의 `pageSize`를 그대로 노출한다.

## 스펙 영향도

- 내부 API: 변경 없음. Controller나 내부 응답 DTO를 변경하지 않는다.
- 외부 API: 변경 있음. 새로운 `representFoodServiceArea` endpoint와 응답 `list` 계약을 소비한다.
- Request: `key`, `type`, `numOfRows`, `pageNo`를 사용한다.
- Response: `code`, `message`, `count`, `pageNo`, `numOfRows`, `pageSize`, `list`를 매핑한다.
- Error: 기존 `ExApiClient.fetch`를 통해 빈 응답, 실패 코드, Feign 예외를 `ExApiException`으로 변환한다.
- 데이터 매칭: 전체 실측 221건 중 현재 위치 API와 직접 매칭되는 행이 172건이므로,
  이번 단계에서 backfill이나 자동 보정은 구현하지 않는다.

## 대안과 선택 이유

- 기존 `RestBestfoodResponse`에 대표 음식 필드를 추가하는 방법은 서로 다른 upstream 계약을
  한 DTO에 섞으므로 선택하지 않는다.
- `ExApiClient`가 `Map`이나 원시 JSON을 반환하는 방법은 기존 응답 계약과 테스트 패턴을
  깨므로 선택하지 않는다.
- 전체 동기화와 저장은 다음 Java/backend 단계로 분리한다. API 연동 단계의 책임을 외부 계약
  역직렬화와 공통 호출 처리로 제한해 이후 매칭 정책을 독립적으로 검토할 수 있게 한다.

## 테스트 계획

- `src/test/java/com/restroute/client/response/RepresentativeFoodResponseTest.java`
  - 실제 성공 응답 필드 역직렬화
  - `SUCCESS`와 비성공 코드 판단
  - 대표 음식·가격·방향 누락 허용
  - 페이지네이션 필드 매핑
- `src/test/java/com/restroute/client/ExApiClientTest.java`
  - 공통 인증키, JSON 포맷, 페이지 파라미터 전달
  - endpoint 실패 시 redacted request URL과 오류 메시지 포함

## 역할 리뷰 판단

- 개발팀장: 필요. 기존 ExApiClient 응집도, 응답 계약과 테스트 범위를 검토한다.
- DevEx: 필요. 새로운 외부 API 호출 계약과 향후 동기화 계층의 사용성을 검토한다.
- CEO: 불필요. 제품 범위와 사용자 가치의 방향을 변경하지 않는 데이터 연동 단계다.
- CSO: 불필요. 새 인증 체계나 키 저장 방식을 추가하지 않고 기존 키 처리 패턴을 재사용한다.
- Design: 불필요. 프론트엔드와 화면 흐름을 변경하지 않는다.

## 완료 기준

- 대표 음식 실제 응답을 DTO로 역직렬화한다.
- `ExApiClient`가 지정 endpoint를 기존 공통 규칙으로 호출한다.
- API 실패·빈 응답·Feign 예외가 기존 예외 계약으로 처리된다.
- API 연동 테스트가 통과한다.
- Entity, Scheduler, backfill, 내부 경로 응답과 문서는 변경하지 않는다.

# 상세/route 통합 응답 슬림화 계획

## 범위

- `GET /api/rest-stops/{serviceAreaCode}`의 `RestStopDetailViewResponse`에서 기능별 API로 분리된 중복 상세 정보를 제거한다.
- 상세 통합 응답은 `REST_STOP` 기준 식별/노선/좌표 값만 유지한다.
- `RestStopQueryService`는 더 이상 주유, 음식, 시설/주차 데이터를 조합하지 않는다.
- `GET /api/route-rest-stops`의 `RouteRestStopResponse`에서 `nationalOilPriceSummary` 최상위 응답 필드를 제거한다.
- `comparisonSummary`, `recommendationTags`는 경로 결과 목록과 추천에 필요한 정보이므로 유지한다.
- 전국 평균 유가는 `comparisonSummary`의 평균 대비 차이 계산에만 내부적으로 사용한다.
- 프론트는 기능별 상세 API와 전국유가 별도 API 호출 방식을 유지하고, 제거된 `restStopName` 필드 의존만 `unitName` 기준으로 정리한다.

## 스펙 영향

- 내부 API 응답 스펙 변경 있음: 기존 상세 통합 응답과 route 응답에서 중복/전역 필드가 제거된다.
- API path 변경 없음.
- 외부 API 스펙 변경 없음.
- DB/데이터 변경 없음.
- 화면 구조 변경 없음.

## 접근 방법 비교

- 대안 1, 기존 통합 응답 필드를 유지한다: 호환성은 높지만 기능별 API 분리 후에도 중복 전송과 끼워넣기 흐름이 계속 남는다.
- 대안 2, 기능별 API로 분리된 값과 route와 직접 관계 없는 전국유가 필드를 제거한다: 응답 계약은 더 엄격해지지만 프론트가 이미 새 API를 쓰고 있어 중복을 줄이고 다음 구조 변경 기준이 명확해진다.

이번 작업은 기존 응답 슬림화가 목표이므로 대안 2를 선택한다.

## 검증 계획

- DTO/Controller/Service 테스트를 슬림화된 응답 계약 기준으로 수정한다.
- route 서비스 테스트에서 `comparisonSummary`, `recommendationTags` 동작은 유지되고 `nationalOilPriceSummary` 응답 필드만 제거되는지 확인한다.
- 프론트 JS 테스트와 lint를 실행해 기능별 API 호출 방식이 유지되는지 확인한다.
- 전체 Gradle 테스트와 하네스 `code`, `verify`, `commit` 게이트를 통과한다.

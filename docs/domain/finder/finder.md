---
domain: finder
aliases: ["휴게소 찾기", "finder 페이지", "이름·거리로 찾기", "목적지로 추천받기"]
paths:
  - "src/main/resources/templates/finder.html"
  - "src/main/resources/static/css/finder.css"
related_domains: ["rest-stop", "route", "oil-price", "ev-charger", "rest-stop-content", "place-search-and-map-config"]
sources: []
---

# finder (휴게소 찾기 모바일 페이지)

## 0. 문서 구성

이 문서는 목적·범위와 화면 상태만 소유하고, 나머지는 아래 group이 나눠 갖는다.

| group | 문서 | 다루는 것 |
|---|---|---|
| concepts | `concepts/terms.md` | 용어, 배지 종류, `RestStopInterest`/`FuelType`, 비슷한 이름의 필드 구분 |
| flows | `flows/screen-flows.md` | 진입 → 팝업 → 목록까지의 화면 흐름, 목적지 지정 두 갈래, 실패·빈 결과 |
| flows | `flows/state-lifecycle.md` | origin/interest 보관 위치, sessionStorage 기억, 요청 경합, 캐시 없음 |
| policies | `policies/badges-and-filters.md` | 배지 슬롯 규칙, 조건 필터 대응, 먹거리·화장실을 뺀 이유 |
| contracts | `contracts/api-contracts.md` | 호출하는 API 계약과 소유 도메인 |
| contracts | `contracts/code-map.md` | JS 모듈 경계와 진입점, 백엔드 소유 클래스 |

## 1. 목적과 범위

`/finder` 모바일 전용 페이지. 지도 중심의 기존 화면(`index.html`/`rest-stops-map.js`, route 도메인)과 별개로,
"이름·거리로 찾기"와 "목적지로 추천받기" 두 진입 경로로 휴게소를 찾고, 각 결과 카드에 판단에 도움이 되는
배지 태그를 붙여 보여준다.

포함: 위치 권한 팝업, 연료/EV 관심 선택 팝업(두 화면 공유), 각 화면의 목록 조회·정렬·조건 필터, 배지
판정·색상 매핑, 결과 카드 클릭 시 지도 화면과 같은 상세 팝업 열기(마크업·모듈 재사용).
제외: 배지가 참조하는 원본 데이터의 동기화·계산 자체(각 도메인 소유), 지도 렌더링, 상세 정보
자체의 조회·계산([[rest-stop-content]]/[[rest-stop]] 소유 — 이 도메인은 재사용만 한다).

## 2. 용어와 핵심 엔티티

→ `concepts/terms.md`

## 3. 사용자·시스템 흐름

→ `flows/screen-flows.md`

## 4. 정책과 불변 조건

→ `policies/badges-and-filters.md`

## 5. 상태와 데이터 수명주기

→ `flows/state-lifecycle.md`

## 6. UI·오류·권한 상태

- **인증**: 전부 공개 페이지·공개 API. 관리자 권한 불필요.
- **위치 거부/미지원**: 이름·거리로 찾기는 Skip이 있어 위치 없이도 이름 검색만으로 계속 쓸 수 있다.
  목적지로 추천받기는 Allow 실패 시 에러 문구를 보여주고 같은 팝업에서 재시도만 가능하다(Skip 경로
  없음 — 목적지 경로 계산 자체에 출발 좌표가 필수). 연료 팝업의 "건너뛰기"는 에러가 아니라 정상
  경로로 취급되어 EV/유가 배지 없이 목록으로 바로 진입한다.

## 7. 외부 시스템과 계약

→ `contracts/api-contracts.md`

## 8. 코드 경계와 진입점

→ `contracts/code-map.md`

# Mobile Route Actions and Route Option Accessibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 모바일 도착지 검색 동작을 더 명확하게 강조하고 경로 선택 카드의 선택 가능성과 현재 상태를 시각·접근성 관점에서 보강한다.

**Architecture:** 기존 `index.html`의 버튼과 `rest-stops-map.js`의 경로 카드 렌더링 구조를 유지한다. 검색 버튼에는 Bootstrap icon과 전용 클래스를 추가하고 모바일 CSS에서 검색·지도 선택 버튼을 65:35 비율로 배치한다. 경로 카드는 기존 native button을 유지하면서 `aria-pressed`, 화살표, `focus-visible` 스타일을 추가한다.

**Tech Stack:** HTML, CSS, JavaScript ES module, Node test runner

## 작업 목표

- 모바일 도착지 검색 버튼을 주요 행동으로 명확하게 구분한다.
- 경로 선택 카드가 선택 가능한 요소임을 시각적으로 보강하고 현재 선택 상태를 보조기기에 전달한다.

## Global Constraints

- 모바일 버튼 높이는 동일하게 유지한다.
- 검색 버튼과 지도 선택 버튼 너비 비율은 65:35로 적용한다.
- 검색 버튼은 강조색을 사용하고 돋보기 아이콘은 `검색` 텍스트 바로 앞에 둔다.
- 경로 카드는 전체 영역이 클릭 가능한 native button 구조를 유지한다.
- 선택된 경로는 `aria-pressed=true`, 미선택 경로는 `aria-pressed=false`로 전달한다.
- 4번 상세정보 준비 중 상태는 이번 작업에 포함하지 않는다.

---

### Task 1: 모바일 검색·지도 선택 버튼

**Files:**
- Modify: `src/main/resources/templates/index.html:72-81`
- Modify: `src/main/resources/static/css/style.css:1898-1935`
- Test: `src/test/js/index-template.test.js`

**Interfaces:**
- Consumes: `#routeDestinationSearchButton`, `#routeDestinationMapButton`
- Produces: `.route-destination-search-button`, `.route-destination-map-button`와 검색 아이콘

- [x] **Step 1: 검색 버튼의 아이콘과 전용 클래스 계약을 검사하는 실패 테스트 작성**
- [x] **Step 2: `npm run test:js -- --test-name-pattern='destination route actions'`로 예상 실패 확인**
- [x] **Step 3: 템플릿에 돋보기 아이콘과 전용 클래스를 추가하고 모바일 grid를 65:35로 변경**
- [x] **Step 4: 집중 테스트 통과 확인**

### Task 2: 경로 선택 카드 접근성

**Files:**
- Modify: `src/main/resources/static/js/rest-stops-map.js:1884-1910`
- Modify: `src/main/resources/static/css/style.css:578-640`
- Test: `src/test/js/rest-stops-map.test.js`

**Interfaces:**
- Consumes: `renderRouteOptionCards(routes, selectedIndex)`
- Produces: 선택 상태 `aria-pressed`와 `.route-option-arrow`를 포함하는 native button 카드

- [x] **Step 1: 선택 상태와 화살표를 검사하는 실패 테스트 작성**
- [x] **Step 2: 집중 테스트로 예상 실패 확인**
- [x] **Step 3: 경로 카드 렌더링과 hover/focus-visible 표현을 최소 변경**
- [x] **Step 4: 집중 테스트 통과 확인**

### Task 3: 통합 검증 및 커밋

**Files:**
- Review: `src/main/resources/templates/index.html`
- Review: `src/main/resources/static/css/style.css`
- Review: `src/main/resources/static/js/rest-stops-map.js`
- Review: `src/test/js/index-template.test.js`
- Review: `src/test/js/rest-stops-map.test.js`

**Interfaces:**
- Consumes: Task 1과 Task 2 결과
- Produces: 검증된 단일 프론트엔드 커밋

- [x] **Step 1: `npm run lint`와 `npm run test:js` 실행**
- [x] **Step 2: 실제 브라우저에서 모바일 버튼과 경로 카드 상태 확인**
- [x] **Step 3: 단일 코드·문서 정합성 리뷰 기록**
- [x] **Step 4: 하네스 전체 검증 실행**
- [ ] **Step 5: `feat: improve mobile route actions`로 관련 파일만 커밋**

## 접근 방법과 Trade-off

- 기존 버튼과 렌더링 함수를 유지해 이벤트 흐름을 바꾸지 않는다.
- 고정 픽셀 너비 대신 `minmax(0, 13fr) minmax(0, 7fr)`를 사용해 65:35 비율과 좁은 화면 대응을 함께 확보한다.
- 카드에 별도 클릭 래퍼를 추가하지 않고 native button을 유지해 Enter·Space 키보드 동작을 그대로 활용한다.
- 화살표는 행동 가능성을 시각적으로 보강하지만, 선택 상태는 색상이나 화살표에만 의존하지 않고 `aria-pressed`로 함께 전달한다.

## 스펙 영향도

- 스펙 변경 없음 판단 이유: 기존 도착지 검색·지도 선택과 경로 선택 기능의 동작은 유지하고, 모바일 배치와 선택 상태 표현만 개선하기 때문이다.
- 내부 API: 변경 없음. 요청·응답 계약을 수정하지 않는다.
- 외부 API: 변경 없음. 지도와 경로 API 호출을 수정하지 않는다.
- 데이터: 변경 없음.
- 제품: 기존 기능의 발견 가능성과 접근성만 개선한다.
- 아키텍처: 변경 없음. 기존 템플릿·스타일·렌더링 함수 안에서 처리한다.
- 품질: Node 테스트의 RED→GREEN과 실제 브라우저 검증, 전체 Gradle·JaCoCo 하네스를 실행한다.

## Git

- 열린 경로상 휴게소 UI 개선의 후속 작업이므로 `codex/route-rest-stop-list-improvement` 브랜치에서 이어간다.

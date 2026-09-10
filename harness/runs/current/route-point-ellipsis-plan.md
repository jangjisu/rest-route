# Mobile Route Point Ellipsis Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 모바일 경로 입력 화면에서 긴 출발지와 도착지 문자열이 두 줄로 확장되지 않고 한 줄 말줄임표로 표시되게 한다.

**Architecture:** 기존 HTML과 JavaScript 데이터 흐름은 유지하고, 모바일 미디어 쿼리 안에서 경로 값 요소의 텍스트 오버플로 표현만 조정한다. 출발지는 `output.route-point-value`, 도착지는 `input.form-control`에 동일한 한 줄 제약을 적용한다.

**Tech Stack:** HTML, CSS, Spring Boot 정적 리소스, 브라우저 계산 스타일 검증

## 작업 목표

- 모바일 경로 입력 화면에서 출발지와 도착지 값을 한 줄로 유지한다.
- 컨테이너 너비를 넘는 문자열만 말줄임표로 표시한다.

## Global Constraints

- 변경 범위는 모바일 경로 입력 영역의 출발·도착 텍스트 표현으로 제한한다.
- 출발·도착 값의 원문과 JavaScript 상태는 자르지 않는다.
- 텍스트가 컨테이너 너비를 넘을 때만 말줄임표로 표시한다.
- 데스크톱 레이아웃과 접힌 경로 요약 영역은 변경하지 않는다.

---

### Task 1: 모바일 출발·도착 한 줄 말줄임

**Files:**
- Modify: `src/main/resources/static/css/style.css:1888-1920`
- Test: 실제 브라우저의 모바일 뷰포트 계산 스타일과 레이아웃 폭

**Interfaces:**
- Consumes: 기존 `.route-point-value`, `.route-point-row .form-control` 요소
- Produces: 모바일에서 `white-space: nowrap`, `overflow: hidden`, `text-overflow: ellipsis`가 적용된 출발·도착 표시

- [x] **Step 1: 변경 전 브라우저 상태 확인**

  390px 모바일 뷰포트에서 긴 출발지의 높이가 한 줄 높이를 초과하고 `white-space`가 `normal`인 현재 상태를 확인한다.

- [x] **Step 2: 최소 CSS 구현**

  `@media (max-width: 575.98px)` 안에 다음 규칙을 추가한다.

  ```css
  .route-point-value,
  .route-point-row .form-control {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
  }
  ```

- [x] **Step 3: 변경 후 브라우저 상태 확인**

  390px 모바일 뷰포트에서 두 요소가 한 줄을 유지하고 긴 값의 원문은 DOM 속성에 그대로 남는지 확인한다.

- [x] **Step 4: 자동 검증**

  ```bash
  npm run lint
  npm run test:js
  env HARNESS_STATE_FILE=harness/runs/current/route-point-ellipsis-state.env harness/harness.sh verify code
  env HARNESS_STATE_FILE=harness/runs/current/route-point-ellipsis-state.env harness/harness.sh verify verify
  ```

- [ ] **Step 5: 커밋**

  ```bash
  git add src/main/resources/static/css/style.css
  git commit -m "fix: truncate mobile route points"
  ```

## 접근 방법과 Trade-off

- 선택: 문자열 자체를 자르지 않고 CSS 말줄임표만 적용한다.
- 선택 이유: 전체 주소는 입력값과 상태에 그대로 보존하면서 모바일 화면 높이만 안정적으로 유지할 수 있다.
- Trade-off: 펼친 화면에서 주소 전체가 한 번에 보이지 않지만, 도착지는 입력 필드에서 이동해 확인할 수 있고 출발지는 다시 설정 화면에서 전체 후보를 확인할 수 있다.

## 스펙 영향도

- 스펙 변경 없음 판단 이유: 출발·도착 주소의 원문과 기능 동작은 유지하고 모바일 CSS 표현만 한 줄 말줄임으로 조정하기 때문이다.
- 내부 API: 변경 없음. 경로 요청과 응답을 수정하지 않는다.
- 외부 API: 변경 없음. 지도·경로 API 연동을 수정하지 않는다.
- 데이터: 변경 없음. 주소 문자열을 저장하거나 가공하는 방식을 수정하지 않는다.
- 제품: 기존 경로 설정 기능은 유지하고 모바일 표현만 한 줄로 제한한다.
- 아키텍처: 변경 없음. 기존 CSS 선택자와 미디어 쿼리 안에서 처리한다.
- 품질: 실제 모바일 뷰포트에서 변경 전후 계산 스타일을 확인하고 기존 프론트 테스트를 실행한다.

## Git

- 열린 경로상 휴게소 UI 개선의 후속 작업이므로 `codex/route-rest-stop-list-improvement` 브랜치에서 이어간다.

---
name: vroom-workflow
description: Use when implementing a feature, bugfix, refactor, API integration, backend change, or frontend behavior change in the vroom-tracker repository.
---

# Vroom Workflow

`vroom-tracker`의 구현 작업을 6단계 하네스와 작업당 한 번의 코드 리뷰로 진행한다.
설명, 아이디어 상담, 단순 문서 열람과 Git 조회에는 사용하지 않는다.

## 필수 원칙

- `AGENTS.md`와 `harness/WORKFLOW.md`를 먼저 읽는다.
- 백엔드/프론트엔드를 나누지 않고 요청을 하나의 작업 단위로 진행한다. 카테고리는 커밋을 나눌 때만 사용한다.
- 설계 승인 전 코드를 수정하지 않는다.
- 코드 리뷰는 전체 작업에서 한 번만 수행하고, 결과 파일이 있을 때만 `completed`로 기록한다.
- 코드 리뷰를 반복하거나 Compound Engineering 리뷰를 추가로 실행하지 않는다.
- 각 단계가 실패하면 hook의 `REGRESS_TO` 단계로 돌아간다.

## 1. 계획 생성

**REQUIRED SUB-SKILL:** Use `superpowers:brainstorming`.

영향이 있는 기준 문서만 읽는다.

- 제품 가치나 범위: `PRODUCT.md`
- 데이터 저장과 연결: `DATA.md`, 외부 API는 `API.md`
- 레이어와 책임: `ARCHITECTURE.md`
- 테스트와 품질: `QUALITY.md`

사용자와 요구사항, 대안과 trade-off를 확인한 뒤 계획 초안을 작성한다.
이 작업을 현재 브랜치에서 이어갈지 새 브랜치로 뗄지 정한다(열린 PR의 파생 작업이면 현재 브랜치, 무관한 새 작업이거나 이미 머지된 코드 이슈면 새 브랜치).

## 2. 스펙 영향도 확인

내부 API, 외부 API, 데이터, 제품, 아키텍처와 품질 영향을 확인한다.
변경이 없더라도 이유를 계획에 기록하고 `harness/harness.sh verify spec`을 통과한다.

## 3. 계획 확정 및 사용자 승인

스펙 영향도, 접근 방법과 trade-off를 반영한 계획을 사용자에게 보여주고 승인받는다.
역할별 계획 리뷰는 수행하지 않는다.
화면(UI) 변경이 있으면 화면 전체가 바뀔 때는 모바일/데스크톱 목업을 만들고, 일부만 바뀔 때는 계획 문서에 변경 스펙을 기록한다. 구현이 필요하지 않다면 계획(및 목업)만으로 종료할 수 있다.
승인 뒤 `verify review`를 실행한다.

## 4. 코드 작성

**REQUIRED SUB-SKILL:** Use `superpowers:test-driven-development`.
승인된 계획을 TDD로 구현한다. 구현 단위별 리뷰나 재리뷰는 수행하지 않는다.
집중 테스트를 사용해 개발하고 코드 작성이 끝난 뒤 `harness/harness.sh verify code`를 한 번 실행한다.

## 5. 문서 동기화, 단일 리뷰와 검증

1. 변경된 계약에 영향을 받는 `PRODUCT.md`, `DATA.md`, `API.md`, `ARCHITECTURE.md`, `QUALITY.md`를 현재 코드와 맞춘다.
2. 코드, 테스트, 관련 Markdown 문서의 정합성을 한 번만 리뷰한다.
3. 결과를 `harness/runs/current/reviews/`에 저장하고 `CODE_REVIEW_STATUS=completed`, `CODE_REVIEW_FILE=<path>`, `CODE_REVIEW_COUNT=1`로 기록한다.
4. 리뷰 결과에는 `문서 정합성` 또는 `Documentation consistency` 항목을 포함한다.
5. 확실한 지적을 수정하되 재리뷰하지 않는다.
6. `harness/harness.sh verify verify`로 테스트와 커버리지를 검증한다.

## 6. Commit

Commit 게이트를 통과한 뒤 작업 브랜치에 커밋한다.
원격 브랜치 push는 자동으로 수행하지 않는다. 사용자가 직접 검증한 뒤 명시적으로 요청하면 그때 push한다.
PR 생성과 기본 브랜치 병합은 사용자가 수행한다.

## 완료 보고

- 승인된 계획
- 작업 전체에서 한 번 수행한 코드·문서 정합성 리뷰 결과
- 테스트와 커버리지 결과
- 브랜치와 커밋 결과 (push는 사용자 요청 시 별도 수행)
- 사용자가 직접 확인해볼 만한 항목(있다면)

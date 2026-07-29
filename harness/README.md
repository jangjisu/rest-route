# 하네스

이 하네스는 하나의 작업을 다음 6단계 게이트로 나누어 진행한다.

1. 계획 생성
2. 스펙 영향도 확인
3. 계획 확정 및 사용자 승인
4. 코드 작성
5. 검증
6. Commit / Push

각 게이트는 안정적인 큰 단계이고, 각 단계 안에는 통과 조건을 검사하는 작은 shell hook들이 있다.

hook은 다음 단계로 넘어가도 되는지 판단하는 역할을 한다.

다만 포맷처럼 기계적으로 복구 가능한 항목은 hook이 자동 적용할 수 있다.
그 외 코드 의미를 바꾸는 수정은 hook이 직접 처리하지 않고 실패로 판단한다.

## 사용법

먼저 현재 작업 run의 상태 파일을 만든다.

```bash
cp harness/runs/current/state.env.example harness/runs/current/state.env
```

그 다음 원하는 단계를 검증한다.

```bash
harness/harness.sh list
harness/harness.sh verify plan
harness/harness.sh verify spec
harness/harness.sh verify review
harness/harness.sh verify code
harness/harness.sh verify verify
harness/harness.sh verify commit
```

전체 단계를 순서대로 검증하려면 다음 명령을 사용한다.

```bash
harness/harness.sh verify all
```

## Hook 종료 코드

| Exit | Meaning |
|---:|---|
| `0` | 통과 |
| `10` | 실패했지만 보통 하네스/AI가 자동으로 보완할 수 있음 |
| `20` | 실패했고 사용자 확인이 필요함 |
| `30` | 차단됨 |
| `99` | 알 수 없는 hook 실패 |

hook은 상태 파일과 실제 파일을 검사하고, 표준화된 결과를 출력한다.

기본 원칙은 hook이 검사자 역할을 한다는 것이다.
자동 포맷처럼 안전한 경우만 hook에서 처리하고, 보완 작업은 사용자 또는 AI가 수행한 뒤 같은 단계를 다시 검증한다.

## 리뷰 원칙

- 역할별 계획 리뷰는 실행하지 않는다.
- 코드 리뷰는 전체 작업에서 한 번만 실행한다.
- Compound Engineering과 교차 모델 리뷰는 실행하지 않는다.
- 단일 리뷰에서 코드, 테스트, 프로젝트 규칙과 관련 Markdown 기준 문서의 정합성을 함께 확인한다.
- 화면(UI) 변경이 있으면 review 단계에서 목업(전체 화면 변경) 또는 계획 문서의 변경 스펙(부분 변경)이 있는지 확인한다.
- 리뷰 지적을 수정한 뒤 재리뷰하지 않고 자동 검증으로 종료한다.
- 상태 파일에는 `CODE_REVIEW_STATUS=completed`, `CODE_REVIEW_FILE`, `CODE_REVIEW_COUNT=1`을 기록한다.

## 커밋과 push

- `commit` 단계는 작업 브랜치에 실제로 커밋이 실행됐는지까지 확인한다(스테이징 상태 또는 최근 커밋 제목과 커밋 메시지 초안 비교).
- push는 하네스가 자동으로 수행하지 않는다. 사용자가 결과를 직접 검증한 뒤 명시적으로 요청할 때만 수행한다.
- `harness/runs/current/state.env`는 경로가 고정돼 있어 다음 작업을 시작하면 덮어써진다(과거 이 때문에 리뷰가 스킵된 사고가 있었다 — 아래 "알려진 리스크" 참고). 커밋이 실제로 완료된 시점에 `04-archive-state.sh`가 그 시점의 `state.env`를 `harness/runs/history/<브랜치명>-<커밋 짧은 해시>.env`로 복사해 둔다. PR 번호는 커밋 시점에 아직 모르므로(브랜치와 PR은 보통 1:1) 브랜치명을 키에 쓰고, 한 브랜치에 커밋이 여러 개 쌓여도 서로 덮어쓰지 않도록 커밋 해시까지 함께 쓴다. git으로 추적하지 않는 로컬 전용 기록이다.

## 알려진 리스크

- 기간: 2026-07-01 09:15 ~ 2026-07-02 (README CI/CD 문서화 작업까지)
- 내용: `harness/runs/current/state.env`가 이전 작업(`mysql-compose-deploy`) 상태로 고정된 채 갱신되지 않았고, 이 구간 동안 `harness.sh verify`가 호출되지 않은 채 작업이 진행됐다. 이 구간에 머지된 작업들이 당시 필수였던 계획·코드 리뷰 게이트를 거치지 않았다. 원인은 같은 구간 동안 `harness.sh verify` 자체가 호출되지 않아 `review` 단계와 `verify` 단계 훅이 함께 스킵된 것이다.
- 원인 추정: 하네스 실행 비용을 줄이는 과정에서 당시 필수 리뷰까지 함께 생략됐다.
- 조치: 이 구간에 대한 소급 리뷰는 수행하지 않고 알려진 리스크로만 기록한다. 2026-07-02부터 `review`·`verify` 단계 게이트를 다시 적용한다.
- 재발 방지: `code`/`verify`/`commit` 단계 시작 시 `review` 단계 hook을 다시 실행하고(`hooks/{code,verify,commit}/00-check-review-gate.sh`), `commit` 단계 시작 시 `verify` 단계 hook도 다시 실행한다(`hooks/commit/00b-check-verify-gate.sh`). 따라서 `harness.sh verify review`나 `harness.sh verify verify`를 건너뛰어도 뒤 단계에서 미통과 상태를 감지하고 차단한다.
- 정리: 한 번도 실제로 사용되지 않던 `COMPOUND_KNOWLEDGE_STATUS`(재사용 학습 기록) 체크는 이번에 제거했다. `docs/solutions/` 같은 학습 문서가 한 건도 없이 매번 `not-required`로만 통과되던 항목이라, 유지 비용만 있고 실효가 없다고 판단했다.

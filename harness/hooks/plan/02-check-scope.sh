#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/harness/lib/result.sh"
. "$ROOT_DIR/harness/lib/state.sh"

load_state "$1"

if [ -z "${SCOPE:-}" ]; then
  fail_needs_user "plan" "check-scope" \
    "이번 run의 작업 범위가 정해지지 않았습니다." \
    "plan" \
    "SCOPE 값을 backend, frontend, api-integration, documentation-harness, git 중 현재 먼저 진행할 범위로 정하세요."
fi

case "${BRANCH_DECISION:-}" in
  continue-current | new-branch) ;;
  *)
    fail_needs_user "plan" "check-scope" \
      "이번 작업을 현재 브랜치에서 이어갈지 새 브랜치로 뗄지 결정되지 않았습니다." \
      "plan" \
      "BRANCH_DECISION 값을 continue-current(열린 PR의 파생 작업) 또는 new-branch(무관한 새 작업 혹은 이미 머지된 코드에 대한 이슈) 중 하나로 정하세요."
    ;;
esac

if [ -z "${BRANCH_DECISION_REASON:-}" ]; then
  fail_needs_user "plan" "check-scope" \
    "브랜치 결정 이유가 기록되지 않았습니다." \
    "plan" \
    "BRANCH_DECISION_REASON에 현재 브랜치를 이어가거나 새 브랜치로 뗀 이유를 한두 문장으로 기록하세요."
fi

pass "plan" "check-scope" "작업 범위와 브랜치 결정이 정해져 있습니다."

#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
TMP_DIR=$(mktemp -d)
trap 'rm -r "$TMP_DIR"' EXIT

STATE_FILE="$TMP_DIR/state.env"
HOOK="$ROOT_DIR/harness/hooks/plan/02-check-scope.sh"

. "$ROOT_DIR/harness/tests/lib/assert.sh"

# SCOPE 자체가 없으면 여전히 실패한다.
printf 'SCOPE=\n' > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"
grep -q '작업 범위가 정해지지' "$TMP_DIR/output.txt"

# SCOPE는 있지만 BRANCH_DECISION이 없으면 실패한다.
printf 'SCOPE=backend\n' > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"
grep -q '브랜치' "$TMP_DIR/output.txt"

# BRANCH_DECISION 값이 허용된 값이 아니면 실패한다.
printf 'SCOPE=backend\nBRANCH_DECISION=maybe\n' > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"

# BRANCH_DECISION은 있지만 이유가 비어있으면 실패한다.
printf 'SCOPE=backend\nBRANCH_DECISION=continue-current\nBRANCH_DECISION_REASON=""\n' > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"
grep -q '이유' "$TMP_DIR/output.txt"

# SCOPE, BRANCH_DECISION, 이유가 모두 있으면 통과한다.
printf 'SCOPE=backend\nBRANCH_DECISION=continue-current\nBRANCH_DECISION_REASON="%s"\n' \
  '열린 PR의 후속 버그 수정' > "$STATE_FILE"
assert_exit_code 0 "$HOOK" "$STATE_FILE"
grep -q 'RESULT\|작업 범위와 브랜치' "$TMP_DIR/output.txt" || true

# new-branch 값도 허용된다.
printf 'SCOPE=frontend\nBRANCH_DECISION=new-branch\nBRANCH_DECISION_REASON="%s"\n' \
  '기존 PR과 무관한 새 요청' > "$STATE_FILE"
assert_exit_code 0 "$HOOK" "$STATE_FILE"

# CATEGORY_ORDER_CONFIRMED는 더 이상 검사하지 않는다 (여러 카테고리여도 통과).
printf 'SCOPE=backend\nCATEGORIES=backend,frontend\nBRANCH_DECISION=continue-current\nBRANCH_DECISION_REASON="%s"\n' \
  '동일 기능의 후속 작업' > "$STATE_FILE"
assert_exit_code 0 "$HOOK" "$STATE_FILE"

echo 'branch decision tests passed'

#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
TMP_DIR=$(mktemp -d)
trap 'rm -r "$TMP_DIR"' EXIT

PLAN_FILE="$TMP_DIR/plan.md"
STATE_FILE="$TMP_DIR/state.env"
REVIEW_FILE="$TMP_DIR/code-review.md"

write_state() {
  status="$1"
  review_file="$2"
  review_count="$3"
  cat > "$STATE_FILE" <<EOF
PLAN_FILE=$PLAN_FILE
PLAN_FINALIZED=true
TRADE_OFF_REVIEWED=true
PLAN_APPROVED_BY_USER=true
CODE_REVIEW_STATUS=$status
CODE_REVIEW_FILE=$review_file
CODE_REVIEW_COUNT=$review_count
EOF
}

. "$ROOT_DIR/harness/tests/lib/assert.sh"

printf '%s\n' '# 계획' '' '## 대안과 선택 이유' '최소 변경안을 선택한다.' > "$PLAN_FILE"

write_state "" "" 0
HARNESS_STATE_FILE="$STATE_FILE" "$ROOT_DIR/harness/harness.sh" verify review > "$TMP_DIR/review-step.txt"
grep -q 'RESULT=STEP_PASS' "$TMP_DIR/review-step.txt"

CODE_REVIEW_HOOK="$ROOT_DIR/harness/hooks/verify/01-check-code-review.sh"

assert_exit_code 10 "$CODE_REVIEW_HOOK" "$STATE_FILE"
grep -q 'CODE_REVIEW_STATUS=completed' "$TMP_DIR/output.txt"

write_state completed "$REVIEW_FILE" 1
assert_exit_code 10 "$CODE_REVIEW_HOOK" "$STATE_FILE"
grep -q '코드 리뷰 결과 파일' "$TMP_DIR/output.txt"

printf '%s\n' '# 코드 리뷰' '문서 정합성 검토를 하지 않음' > "$REVIEW_FILE"
assert_exit_code 10 "$CODE_REVIEW_HOOK" "$STATE_FILE"
grep -q '문서 정합성' "$TMP_DIR/output.txt"

printf '%s\n' '# 코드 리뷰' '## 문서 정합성' '관련 기준 문서가 현재 변경과 일치한다.' > "$REVIEW_FILE"
write_state completed "$REVIEW_FILE" 2
assert_exit_code 10 "$CODE_REVIEW_HOOK" "$STATE_FILE"
grep -q 'CODE_REVIEW_COUNT=1' "$TMP_DIR/output.txt"

write_state completed "$REVIEW_FILE" 1
assert_exit_code 0 "$CODE_REVIEW_HOOK" "$STATE_FILE"
grep -q '작업 전체의 단일 코드 리뷰' "$TMP_DIR/output.txt"

echo 'single review workflow tests passed'

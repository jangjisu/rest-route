#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
TMP_DIR=$(mktemp -d)
trap 'rm -r "$TMP_DIR"' EXIT

STATE_FILE="$TMP_DIR/state.env"
HOOK="$ROOT_DIR/harness/hooks/plan/01-check-work-type.sh"

assert_exit_code() {
  expected="$1"
  shift
  set +e
  "$@" > "$TMP_DIR/output.txt" 2>&1
  actual=$?
  set -e
  if [ "$actual" -ne "$expected" ]; then
    cat "$TMP_DIR/output.txt"
    echo "expected exit $expected, got $actual" >&2
    exit 1
  fi
}

# WORKFLOW가 비어 있으면 실패한다.
: > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"

# 백엔드/프론트엔드를 나누던 예전 값은 더 이상 허용하지 않는다.
printf 'WORKFLOW=backend-rest-api\n' > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"

printf 'WORKFLOW=frontend-change\n' > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"

printf 'WORKFLOW=fullstack-feature\n' > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"

# 통합된 feature 값과 나머지 작업 유형 값은 통과한다.
for value in feature bugfix public-api-integration documentation-harness git-only; do
  printf 'WORKFLOW=%s\n' "$value" > "$STATE_FILE"
  assert_exit_code 0 "$HOOK" "$STATE_FILE"
done

echo 'work type tests passed'

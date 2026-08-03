#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
TMP_DIR=$(mktemp -d)
trap 'rm -r "$TMP_DIR"' EXIT

STATE_FILE="$TMP_DIR/state.env"
MOCKUP_FILE="$TMP_DIR/mockup.html"
HOOK="$ROOT_DIR/harness/hooks/review/03-check-ui-mockup.sh"

. "$ROOT_DIR/harness/tests/lib/assert.sh"

# 화면 변경이 없으면 통과한다.
printf 'UI_IMPACT=no\n' > "$STATE_FILE"
assert_exit_code 0 "$HOOK" "$STATE_FILE"

# UI_IMPACT=yes인데 UI_CHANGE_KIND가 없으면 실패한다.
printf 'UI_IMPACT=yes\n' > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"
grep -q 'UI_CHANGE_KIND' "$TMP_DIR/output.txt"

# full-screen인데 목업 파일 경로가 없으면 실패한다.
printf 'UI_IMPACT=yes\nUI_CHANGE_KIND=full-screen\n' > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"
grep -q '목업' "$TMP_DIR/output.txt"

# full-screen인데 목업 파일 경로는 있지만 실제 파일이 없으면 실패한다.
printf 'UI_IMPACT=yes\nUI_CHANGE_KIND=full-screen\nUI_MOCKUP_FILE=%s\n' "$MOCKUP_FILE" > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"

# full-screen이고 목업 파일이 실제로 있으면 통과한다.
printf '<html></html>' > "$MOCKUP_FILE"
assert_exit_code 0 "$HOOK" "$STATE_FILE"

# partial인데 스펙 기록이 안 되어 있으면 실패한다.
printf 'UI_IMPACT=yes\nUI_CHANGE_KIND=partial\n' > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"
grep -q 'UI_CHANGE_SPEC_RECORDED' "$TMP_DIR/output.txt"

# partial이고 스펙 기록이 true면 통과한다.
printf 'UI_IMPACT=yes\nUI_CHANGE_KIND=partial\nUI_CHANGE_SPEC_RECORDED=true\n' > "$STATE_FILE"
assert_exit_code 0 "$HOOK" "$STATE_FILE"

# UI_CHANGE_KIND가 허용된 값이 아니면 실패한다.
printf 'UI_IMPACT=yes\nUI_CHANGE_KIND=something-else\n' > "$STATE_FILE"
assert_exit_code 20 "$HOOK" "$STATE_FILE"

echo 'ui mockup tests passed'

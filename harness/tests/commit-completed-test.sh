#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
TMP_DIR=$(mktemp -d)
trap 'rm -r "$TMP_DIR"' EXIT

REPO_DIR="$TMP_DIR/repo"
mkdir -p "$REPO_DIR"
cd "$REPO_DIR"
git init --quiet
git config user.email "test@example.com"
git config user.name "test"
printf 'placeholder\n' > README.md
git add README.md
git commit --quiet -m "chore: init"

STATE_FILE="$TMP_DIR/state.env"
MESSAGE_FILE="$TMP_DIR/commit-message.txt"
HOOK="$ROOT_DIR/harness/hooks/commit/02-check-staged-files.sh"

. "$ROOT_DIR/harness/tests/lib/assert.sh"

# 커밋 메시지 초안 파일이 없으면 실패한다.
printf 'COMMIT_MESSAGE_FILE=%s\n' "$MESSAGE_FILE" > "$STATE_FILE"
assert_exit_code 10 "$HOOK" "$STATE_FILE"

# 스테이징된 파일이 있으면 "준비됨"으로 통과한다(커밋 전).
printf 'feat: 새 기능 추가\n' > "$MESSAGE_FILE"
printf 'change\n' >> "$REPO_DIR/README.md"
(cd "$REPO_DIR" && git add README.md)
assert_exit_code 0 "$HOOK" "$STATE_FILE"
grep -q '준비' "$TMP_DIR/output.txt"

# 스테이징도 없고 최근 커밋 제목도 초안과 다르면 실패한다(아직 커밋 안 됨, 재시도 가능).
(cd "$REPO_DIR" && git commit --quiet -m "chore: 관련 없는 커밋")
assert_exit_code 10 "$HOOK" "$STATE_FILE"
grep -q '아직' "$TMP_DIR/output.txt"

# 스테이징은 없지만 최근 커밋 제목이 초안과 일치하면 "완료됨"으로 통과한다(커밋 후).
(cd "$REPO_DIR" && git commit --quiet --allow-empty -m "feat: 새 기능 추가")
assert_exit_code 0 "$HOOK" "$STATE_FILE"
grep -q '완료\|실행되었습니다' "$TMP_DIR/output.txt"

echo 'commit completed tests passed'

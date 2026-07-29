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

BRANCH=$(git rev-parse --abbrev-ref HEAD)
ARCHIVE_FILE="$REPO_DIR/harness/runs/history/${BRANCH}.env"

STATE_FILE="$TMP_DIR/state.env"
MESSAGE_FILE="$TMP_DIR/commit-message.txt"
HOOK="$ROOT_DIR/harness/hooks/commit/04-archive-state.sh"

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

printf 'COMMIT_MESSAGE_FILE=%s\nWORKFLOW=state-archive-test\n' "$MESSAGE_FILE" > "$STATE_FILE"

# 커밋 메시지 초안 파일이 없으면 보관하지 않고 통과한다.
assert_exit_code 0 "$HOOK" "$STATE_FILE"
grep -q '보관하지 않' "$TMP_DIR/output.txt"
[ ! -f "$ARCHIVE_FILE" ]

# 스테이징된 파일이 있으면(아직 커밋 전) 보관하지 않고 통과한다.
printf 'feat: 새 기능 추가\n' > "$MESSAGE_FILE"
printf 'change\n' >> "$REPO_DIR/README.md"
(cd "$REPO_DIR" && git add README.md)
assert_exit_code 0 "$HOOK" "$STATE_FILE"
grep -q '보관하지 않' "$TMP_DIR/output.txt"
[ ! -f "$ARCHIVE_FILE" ]

# 스테이징도 없고 최근 커밋 제목도 초안과 다르면 보관하지 않고 통과한다.
(cd "$REPO_DIR" && git commit --quiet -m "chore: 관련 없는 커밋")
assert_exit_code 0 "$HOOK" "$STATE_FILE"
grep -q '보관하지 않' "$TMP_DIR/output.txt"
[ ! -f "$ARCHIVE_FILE" ]

# 최근 커밋 제목이 초안과 일치하면(커밋 완료) 브랜치명 기준으로 state.env를 보관한다.
(cd "$REPO_DIR" && git commit --quiet --allow-empty -m "feat: 새 기능 추가")
assert_exit_code 0 "$HOOK" "$STATE_FILE"
grep -q '보관했습니다' "$TMP_DIR/output.txt"
[ -f "$ARCHIVE_FILE" ]
grep -q 'WORKFLOW=state-archive-test' "$ARCHIVE_FILE"

echo 'state archive tests passed'

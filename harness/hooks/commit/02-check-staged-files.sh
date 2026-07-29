#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/harness/lib/result.sh"
. "$ROOT_DIR/harness/lib/state.sh"

load_state "$1"
cd "$(repo_root)"

message_file="${COMMIT_MESSAGE_FILE:-harness/runs/current/commit-message.txt}"

if [ ! -f "$message_file" ]; then
  fail_autofixable "commit" "check-staged-files" \
    "커밋 메시지 초안 파일이 없습니다: $message_file" \
    "commit" \
    "$message_file 파일에 커밋 제목을 작성하세요."
fi

staged=$(git diff --cached --name-only --diff-filter=ACMRTUXB)

if [ -n "$staged" ]; then
  pass "commit" "check-staged-files" "staged 파일이 있어 커밋할 준비가 되어 있습니다."
fi

draft_title=$(sed -n '1p' "$message_file")
head_title=$(git log -1 --format=%s 2>/dev/null || true)

if [ -n "$draft_title" ] && [ "$draft_title" = "$head_title" ]; then
  pass "commit" "check-staged-files" "커밋이 이미 실행되어 staged 파일이 없습니다. 커밋이 실제로 실행되었습니다."
fi

fail_autofixable "commit" "check-staged-files" \
  "staged 파일이 없고, 아직 커밋도 실행되지 않았습니다." \
  "commit" \
  "커밋할 파일을 계획 범위에 맞게 stage하거나, 이미 stage했다면 git commit -F $message_file 을 실행한 뒤 다시 검증하세요."

#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/harness/lib/result.sh"
. "$ROOT_DIR/harness/lib/state.sh"

state_file="$1"
load_state "$state_file"
cd "$(repo_root)"

message_file="${COMMIT_MESSAGE_FILE:-harness/runs/current/commit-message.txt}"

staged=$(git diff --cached --name-only --diff-filter=ACMRTUXB)
if [ -n "$staged" ]; then
  pass "commit" "archive-state" "아직 커밋 전이라 state.env를 보관하지 않았습니다."
fi

if [ ! -f "$message_file" ]; then
  pass "commit" "archive-state" "커밋 메시지 초안이 없어 state.env를 보관하지 않았습니다."
fi

draft_title=$(sed -n '1p' "$message_file")
head_title=$(git log -1 --format=%s 2>/dev/null || true)

if [ -z "$draft_title" ] || [ "$draft_title" != "$head_title" ]; then
  pass "commit" "archive-state" "아직 커밋이 확인되지 않아 state.env를 보관하지 않았습니다."
fi

branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
safe_branch=$(printf '%s' "$branch" | tr '/' '-')
short_sha=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
history_dir="harness/runs/history"
archive_file="$history_dir/${safe_branch}-${short_sha}.env"

mkdir -p "$history_dir"
cp "$state_file" "$archive_file"

pass "commit" "archive-state" "state.env를 $archive_file 에 보관했습니다. (같은 브랜치라도 커밋마다 별도 파일로 남으며, state.env 자체는 다음 작업에서 덮어써집니다.)"

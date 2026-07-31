#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/harness/lib/result.sh"
. "$ROOT_DIR/harness/lib/state.sh"

load_state "$1"
cd "$(repo_root)"

case "${SCOPE:-}" in
  backend)
    bad_files=$(
      changed_files |
        awk '
          /^src\/main\/java\// { next }
          /^src\/test\/java\// { next }
          /^src\/test\/resources\// { next }
          /^rules\/backend\// { next }
          /^docs\// { next }
          /^harness\// { next }
          /^build\.gradle$/ { next }
          /^settings\.gradle$/ { next }
          /^config\// { next }
          /\.md$/ { next }
          NF { print }
        '
    )
    if [ -n "$bad_files" ]; then
      printf '%s\n' "$bad_files" >&2
      fail_needs_user "code" "check-change-scope" \
        "backend 범위 밖의 변경 파일이 있습니다." \
        "plan" \
        "이번 run의 범위를 조정하거나, 범위 밖 변경을 별도 run으로 분리하세요."
    fi
    ;;
esac

pass "code" "check-change-scope" "변경 파일 범위가 현재 scope와 충돌하지 않습니다."

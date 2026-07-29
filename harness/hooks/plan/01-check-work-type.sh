#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/harness/lib/result.sh"
. "$ROOT_DIR/harness/lib/state.sh"

load_state "$1"

case "${WORKFLOW:-}" in
  feature | bugfix | public-api-integration | documentation-harness | git-only) ;;
  *)
    fail_needs_user "plan" "check-work-type" \
      "작업 유형이 정해지지 않았습니다." \
      "plan" \
      "WORKFLOW 값을 feature, bugfix, public-api-integration, documentation-harness, git-only 중 하나로 정하세요. (백엔드/프론트엔드 구분은 여기서 하지 않습니다 — 카테고리는 커밋을 나눌 때만 씁니다.)"
    ;;
esac

pass "plan" "check-work-type" "작업 유형이 정해져 있습니다."

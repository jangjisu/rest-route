#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/harness/lib/result.sh"
. "$ROOT_DIR/harness/lib/state.sh"

load_state "$1"

if [ "${UI_IMPACT:-no}" != "yes" ]; then
  pass "review" "check-ui-mockup" "화면 변경이 없어 목업 확인을 생략합니다."
fi

case "${UI_CHANGE_KIND:-}" in
  full-screen)
    if [ -z "${UI_MOCKUP_FILE:-}" ]; then
      fail_needs_user "review" "check-ui-mockup" \
        "화면 전체가 바뀌는 작업인데 목업 HTML 파일 경로가 기록되지 않았습니다." \
        "plan" \
        "예상 화면(모바일/데스크톱)을 보여주는 HTML 목업을 만들고 UI_MOCKUP_FILE에 경로를 기록하세요."
    fi
    if [ ! -f "${UI_MOCKUP_FILE}" ]; then
      fail_needs_user "review" "check-ui-mockup" \
        "UI_MOCKUP_FILE(${UI_MOCKUP_FILE})에 해당하는 파일이 존재하지 않습니다." \
        "plan" \
        "기록한 경로에 실제 목업 HTML 파일을 만드세요."
    fi
    ;;
  partial)
    if [ "${UI_CHANGE_SPEC_RECORDED:-false}" != "true" ]; then
      fail_needs_user "review" "check-ui-mockup" \
        "화면 일부만 바뀌는 작업인데 변경 내용 스펙이 계획 문서에 기록됐다는 확인이 없습니다." \
        "plan" \
        "계획 문서에 무엇이 어떻게 바뀌는지 적고 UI_CHANGE_SPEC_RECORDED=true로 기록하세요."
    fi
    ;;
  *)
    fail_needs_user "review" "check-ui-mockup" \
      "화면 변경 종류가 기록되지 않았습니다." \
      "plan" \
      "UI_CHANGE_KIND 값을 full-screen 또는 partial 중 하나로 정하세요."
    ;;
esac

pass "review" "check-ui-mockup" "화면 변경 기록(목업 또는 스펙)이 확인됩니다."

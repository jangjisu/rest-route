#!/usr/bin/env sh

# 사용하는 쪽에서 TMP_DIR을 먼저 정의해 둬야 한다(출력을 담을 스크래치 디렉터리).
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

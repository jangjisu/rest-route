#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/harness/lib/result.sh"
. "$ROOT_DIR/harness/lib/state.sh"

load_state "$1"
cd "$(repo_root)"

java_files=$(changed_java_files)

if [ -z "$java_files" ]; then
  pass "code" "run-code-quality-tools" "변경된 Java 파일이 없어 code quality tool 실행을 생략합니다."
fi

task_list=""
if [ -x ./gradlew ]; then
	if ! task_list=$(./gradlew tasks --all --console=plain); then
		fail_blocked "code" "run-code-quality-tools" \
			"Gradle task 목록을 조회하지 못해 code quality tool 실행 여부를 판단할 수 없습니다." \
			"code" \
			"Gradle 실행 오류를 먼저 해결한 뒤 다시 검사하세요."
	fi
fi
ran_tools=""
formatted_tools=""

task_exists() {
	task_name="$1"

	printf '%s\n' "$task_list" | grep -Eq "^[[:space:]]*${task_name}[[:space:]]"
}

run_format_task_if_exists() {
	task_name="$1"
	tool_name="$2"

	if task_exists "$task_name"; then
		if ! ./gradlew "$task_name" --console=plain; then
			fail_autofixable "code" "run-code-quality-tools" \
				"$tool_name 자동 포맷 적용에 실패했습니다." \
				"code" \
				"$tool_name 오류를 확인하고 포맷 적용이 가능하도록 수정하세요."
		fi
		formatted_tools="${formatted_tools}${tool_name} "
	fi
}

run_check_task_if_exists() {
	task_name="$1"
	tool_name="$2"

	if task_exists "$task_name"; then
		if ! ./gradlew "$task_name" --console=plain; then
			fail_autofixable "code" "run-code-quality-tools" \
				"$tool_name 검사에서 자동 포맷으로 해결되지 않는 문제가 감지되었습니다." \
				"code" \
				"$tool_name 결과를 확인하고 코드를 수정하세요."
		fi
		ran_tools="${ran_tools}${tool_name} "
	fi
}

run_format_cli_if_exists() {
	command_name="$1"
	tool_name="$2"
	shift 2

	if command -v "$command_name" >/dev/null 2>&1; then
		if ! "$command_name" "$@"; then
			fail_autofixable "code" "run-code-quality-tools" \
				"$tool_name 자동 포맷 적용에 실패했습니다." \
				"code" \
				"$tool_name 오류를 확인하고 포맷 적용이 가능하도록 수정하세요."
		fi
		formatted_tools="${formatted_tools}${tool_name} "
	fi
}

if task_exists "spotlessJavaApply"; then
	run_format_task_if_exists "spotlessJavaApply" "Spotless Java"
elif task_exists "spotlessApply"; then
	run_format_task_if_exists "spotlessApply" "Spotless"
fi

run_format_cli_if_exists "palantir-java-format" "Palantir Java Format" --replace $java_files

run_check_task_if_exists "checkstyleMain" "Checkstyle"
run_check_task_if_exists "checkstyleTest" "Checkstyle"
run_check_task_if_exists "palantirJavaFormatCheck" "Palantir Java Format"
run_check_task_if_exists "javaFormatCheck" "Java Format"
run_check_task_if_exists "formatJavaCheck" "Java Format"
run_check_task_if_exists "spotlessJavaCheck" "Spotless Java"
run_check_task_if_exists "spotlessCheck" "Spotless"

if [ -n "${SONAR_TOKEN:-}" ] || [ -n "${SONAR_HOST_URL:-}" ] || [ -f sonar-project.properties ]; then
	run_check_task_if_exists "sonar" "SonarQube"
	run_check_task_if_exists "sonarqube" "SonarQube"
fi

# rules/backend/module-design.md의 "얕은 모듈" 판단 기준 중 기계적으로 검증 가능한 부분
# (TooManyMethods/TooManyFields/CyclomaticComplexity)을 실제로 막는 게이트. 나머지 PMD
# 규칙(bestpractices/errorprone)은 pmdMain으로 계속 SonarCloud에만 advisory로 올라간다.
run_check_task_if_exists "pmdDepthGate" "PMD(모듈 depth)"

if [ -z "$formatted_tools" ] && [ -z "$ran_tools" ]; then
	pass "code" "run-code-quality-tools" "실행 가능한 Checkstyle/SonarQube/Palantir task 또는 CLI가 설정되어 있지 않습니다."
fi

pass "code" "run-code-quality-tools" "Code quality tools passed. formatted: $formatted_tools checked: $ran_tools"

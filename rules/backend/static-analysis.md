# 정적분석 도구(PMD/SpotBugs) 현황

## 실행 방법

```bash
./gradlew pmdMain        # PMD, main 소스만(테스트는 비활성화)
./gradlew spotbugsMain    # SpotBugs, main 소스만(테스트는 비활성화)
```

`harness/hooks/code/04-run-code-quality-tools.sh`나 CI(`./gradlew test`)는 이 두 task를 자동으로 돌리지 않는다 — 커밋/CI를 막지 않는 상태로 도구만 먼저 추가했다(2026-07-31). `./gradlew check`/`build`는 Gradle 기본 동작으로 이 task들에 의존하므로, 아래 잔여 findings가 남아있는 동안은 `check`가 실패한다. 이건 의도된 상태다 — 잔여 findings를 다 고치기 전에는 `check`/`build`를 커밋 게이트에 넣지 않는다.

## 테스트 소스는 대상에서 제외

`pmdTest`/`spotbugsTest`는 `build.gradle`에서 `enabled = false`로 꺼져 있다. 1차 스캔에서 `pmdTest`가 700건 이상(대부분 `UnitTestContainsTooManyAsserts`/`AvoidDuplicateLiterals` — 테스트 코드의 정상적인 관용구를 오탐 처리) 나와서, 테스트 코드는 애초에 다른 품질 기준을 적용하는 게 맞다고 판단했다.

## 껐다 켰다 판단 기준 — 이 프로젝트 스타일과 충돌하는 규칙은 끈다

- **SpotBugs `EI_EXPOSE_REP`/`EI_EXPOSE_REP2`**: record(불변 DTO)와 Spring 생성자 주입 필드에서 전부 걸림(1차 스캔 100여건). 두 패턴 다 이 프로젝트의 기본 스타일이라 방어적 복사가 의미 없어 규칙 자체를 껐다(`config/spotbugs/exclude.xml`).
- **SpotBugs `UWF_UNWRITTEN_FIELD`**: Jackson이 리플렉션으로 필드를 채우는 외부 API 응답 DTO에서만 발생 — SpotBugs가 Jackson 필드 기반 역직렬화를 인식 못 해서 나는 오탐.
- **PMD `AvoidFieldNameMatchingMethodName`**: record/이 프로젝트의 enum-with-accessor 패턴에서 필드명과 접근자 메서드명이 같은 건 관례이지 버그가 아니다.
- **PMD `MissingSerialVersionUID`**: 커스텀 예외 12개 전부 같은 JVM 프로세스 안에서만 던지고 잡는 `RuntimeException` 서브클래스라, 실제 자바 직렬화 위험이 없다(EI/EI2와 같은 판단 기준).

새로운 오탐 패턴을 발견하면 여기 이유를 적고 제외 목록에 추가한다.

## 잔여 백로그 (2026-07-31 기준, 아직 안 고침)

`./gradlew pmdMain`/`spotbugsMain`으로 재현 가능. 사용자 결정: 도구 추가와 커밋을 먼저 하고, 아래 항목은 별도 작업으로 진행한다.

| 규칙 | 건수 | 위치(대표) |
|---|---|---|
| SpotBugs `Nm`(오해 소지 있는 이름) | 1 | `RestStopDetailResponse.UpstreamException`이 `Exception`을 상속 안 함 |
| SpotBugs `RCN`(불필요한 null 체크) | 1 | `RestStopStartupInitializer.initializeEvChargers()` |
| PMD `AvoidDuplicateLiterals` | 6 | `SalesRankingCsvParser`의 CSV 헤더명 리터럴 반복 |
| PMD `AvoidLiteralsInIfCondition` | 6 | 여러 파일, 조건문에 매직 리터럴 |
| PMD `PreserveStackTrace` | 3 | 예외 재발생 시 원본 스택트레이스 유실 |
| PMD `LiteralsFirstInComparisons` | 3 | `x.equals("literal")` → `"literal".equals(x)` |
| PMD `ConstructorCallsOverridableMethod` | 3 | `EvChargerEntity`/`RestStopProductSalesRankEntity`/`RestStopStoreSalesRankEntity`의 `updateFrom()` |
| PMD `NullAssignment` | 2 | `RestOilEntity`/`RestOilPriceEntity` |
| PMD `UseLocaleWithCaseConversions` | 1 | `SalesRankingRestStopNameNormalizer` |

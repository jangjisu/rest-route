# 정적분석 도구(PMD/SpotBugs) 현황

## 실행 방법

```bash
./gradlew pmdMain        # PMD, main 소스만(테스트는 비활성화)
./gradlew spotbugsMain    # SpotBugs, main 소스만(테스트는 비활성화)
```

`harness/hooks/code/04-run-code-quality-tools.sh`나 CI(`./gradlew test`)는 이 두 task를 자동으로 돌리지 않는다 — 커밋/CI를 막지 않는 상태로 도구만 먼저 추가했다(2026-07-31). `./gradlew check`/`build`는 Gradle 기본 동작으로 이 task들에 의존하므로, 아래 잔여 findings가 남아있는 동안은 `check`가 실패한다. 이건 의도된 상태다 — 잔여 findings를 다 고치기 전에는 `check`/`build`를 커밋 게이트에 넣지 않는다.

## `pmdDepthGate` — 모듈 depth 관련 규칙만 실제로 막는 별도 게이트 (2026-08-02)

`pmdMain`(위)의 잔여 백로그를 다 고치기 전까지는 `pmdMain` 자체를 커밋 게이트에 넣지
않기로 한 결정은 유지한다. 대신 `rules/backend/module-design.md`가 다루는 "모듈이
얕아지는지" 판단과 직접 관련된 규칙(`config/pmd/ruleset-depth-gate.xml`)만 별도
Gradle task `pmdDepthGate`로 떼어내 `harness/hooks/code/04-run-code-quality-tools.sh`
에서 **실제로 커밋을 막는 게이트**로 연결했다. `pmdMain`(bestpractices/errorprone, 아래
잔여 백로그)과 분리한 이유: 전체를 한꺼번에 막으면 이번 작업과 무관한 오래된 빚(잔여
백로그 26건)까지 전부 갚아야 커밋이 가능해져 범위가 지나치게 커진다.

```bash
./gradlew pmdDepthGate
```

### `TooManyMethods` 대신 `TooManyPublicMethods`를 직접 정의한 이유

PMD 기본 `TooManyMethods`는 public/private 구분 없이 전체 메서드를 센다. 그런데
`module-design.md`의 depth 기준은 "공개 인터페이스 크기"다 — private 헬퍼가 많은 건
오히려 좋은 설계다(작은 공개 인터페이스 뒤에 구현이 숨어있는 것). 이 차이 때문에
`EvChargerStationMappingCalculator`(공개 메서드 1개 + private 13개, 알고리즘을 잘
쪼갠 모범 사례)가 기본 규칙으로는 오탐이었다. PMD Java AST의 `MethodDeclaration`이
`@Visibility` 속성을 노출하는 걸 확인하고, `category/java/design.xml/TooManyMethods`의
XPath를 그대로 가져와 `@Visibility = 'public'` 조건만 추가한 커스텀 규칙
`TooManyPublicMethods`를 `ruleset-depth-gate.xml`에 직접 정의했다.

이 기준으로 다시 스캔한 결과 `ExApiClient`(공개 8개)/`RestStopScheduler`(공개 2개)/
`EvChargerSyncService`(공개 2개)/`EvChargerStationMappingCalculator`(공개 1개)는 전부
임계치(10) 아래라 실제로는 문제가 아니었다 — 남은 건 `AdminActivityLogService`
(공개 17개, 액션별 log 메서드) 하나뿐이었는데, 이건 `ARCHITECTURE.md`에 이미 기록된
의도적 설계 결정(AOP 대신 액션별 명시적 호출, 문구 가독성 우선)이라 파일 단위로
제외했다.

### `TooManyFields` 제외 대상

`domain/**`(JPA `@Entity`), `client/response/**`(외부 API 응답 Jackson DTO)는 DB
컬럼/외부 응답 구조를 1:1로 반영하는 게 정상적인 형태라 필드 수가 자연히 많다 —
depth 문제와 무관해 제외했다.

### 첫 적용 사례 — `RouteRestStopService`

메서드 20개로 임계치를 넘어 있던 걸 카카오 좌표조회+후보탐색을 `RouteRestStopCandidateFinder`
(신규 `@Service`)로 분리해 해소했다. `resolveDestination()`의 `CyclomaticComplexity`(10)는
`destinationFromQuery()`로 갈래를 나눠 해소했고, 좌표 파싱 헬퍼(`coordinateParam`/
`parseCoordinate`)는 상태 없는 정적 유틸 `RouteCoordinateFormat`으로 빼서 Finder
자체가 다시 임계치를 넘지 않게 했다.

## 잔여 백로그(모듈 depth) — 현재 없음 (2026-08-02 기준)

`pmdDepthGate`가 깨끗하게 통과하는 상태를 유지한다. 앞으로 이 게이트에 새로 걸리는
항목은 `module-design.md` 기준으로 그 자리에서 판단한다.

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

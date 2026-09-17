## AS-IS

- **아무도 부르지 않는 휴게소 집계 조회 경로가 세 겹으로 살아 있었음.** `RestStopAggregateQueryService`에는 "서비스지역코드 목록과 override 조건으로 집계를 조회한다"는 진입점이 있었지만 프로덕션 호출부가 하나도 없었음. 그 메서드가 유일한 호출자였던 `RestStopQueryService`의 동명 메서드, 다시 그것이 유일한 호출자였던 `RestStopRepository`의 `@Query`까지 — 최상단을 지우면 아래 두 층이 연쇄로 죽는 구조였음
- 이 죽은 경로를 테스트 9곳이 지키고 있었음. 프로덕션에 소비자가 없는 동작을 테스트가 대신 붙들고 있으니, 지워도 되는 코드인지가 커버리지 숫자에 가려 보이지 않았음
- **집계 조회의 `adminOverridden` 파라미터가 서비스 레이어에서 한 번도 다른 값을 가진 적이 없었음.** 지도 화면·finder 목록·주변 검색 세 호출부가 모두 "거르지 않는다"만 넘겼고, 그 값은 관련정보 조회를 지나 리포지토리까지 그대로 흘렀음. 읽는 사람은 세 단계를 열어보고 나서야 이 값이 갈린 적이 없다는 걸 알 수 있었음
- 직전 PR에서 이 매직 `null`에 `ANY_ADMIN_OVERRIDDEN`이라는 이름을 붙였는데, 이름이 붙고 나니 **애초에 갈리지 않는 값을 파라미터로 들고 다닌다**는 사실이 더 또렷해졌음
- 메서드 이름 `findByRestStopsAndAdminOverridden`이 없는 선택지를 약속했음 — 이름은 override 여부로 거를 수 있다고 말하는데 실제로는 아무도 거르지 않았음
- 클래스 주석이 `RestStopServiceAreaCodeBackfillService`가 이 서비스를 통해 조합한다고 적고 있었으나, **그 클래스는 이 서비스를 참조조차 하지 않음.** 실제 호출부 셋 중 둘은 이름이 적혀 있지도 않았음

## TO-BE

- 휴게소 집계는 **이미 조회해둔 휴게소 목록을 건네받아 조립하는 한 가지 방식**만 남았다. 스스로 조회해오는 진입점은 없다
- 남은 진입점 이름이 `findByRestStops`다 — 넘기는 것이 휴게소 목록이고, 그 외에 고를 것이 없다는 사실이 이름과 시그니처에서 바로 보인다
- 서비스 레이어 어디에도 `Boolean adminOverridden` 파라미터가 없다. "관리자 재정의 여부로 거르지 않는다"는 정책은 `RestStopRelatedInfoQueryService` 안의 상수 한 곳이 갖고, 그 밖으로 새지 않는다
- **리포지토리의 `adminOverridden`은 그대로다.** `RestOilServiceAreaCodeBackfiller`가 `false`로 실제 사용 중이고, 그 경로는 서비스를 거치지 않으므로 이번 변경과 무관하다
- 집계 서비스가 아는 것이 줄었다: 의존성 8개 → 7개. 조회를 위임하려고 들고 있던 `RestStopQueryService`가 필요 없어졌다
- 클래스 주석은 이력을 몰라도 참인 것만 남았다. 호출부 목록은 늘고 줄 때마다 다시 틀리므로 열거하지 않는다

**동작은 하나도 바뀌지 않았다.** 인수 테스트 13개가 `src/e2e`를 **0줄 수정한 채** 통과하는 것이 그 증거다.

## 변경 사항

- 죽은 집계 조회 경로 3단을 걷어냄
  - `RestStopAggregateQueryService`에서 코드 목록으로 조회하는 진입점을 지웠다 — 프로덕션 호출부가 0이었다
  - 연쇄로 죽은 `RestStopQueryService`의 동명 메서드와 `RestStopRepository`의 `@Query`를 함께 지웠다
  - 조회를 위임하려고 들고 있던 `RestStopQueryService` 의존성이 집계 서비스에서 사라졌다
  - 집계 조합을 검증하던 테스트는 진입점만 갈아타서 살렸다 — 사라지는 것은 진입 경로이지 집계 로직이 아니다
  - 순수 위임만 확인하던 케이스와 지운 쿼리를 검증하던 케이스는 함께 지웠다
- 서비스 레이어에서 쓰이지 않는 `adminOverridden`을 제거
  - 집계 조회와 관련정보 조회의 파라미터를 없애고, "거르지 않는다"는 뜻의 상수로 리포지토리에 넘긴다
  - `findByRestStopsAndAdminOverridden`을 `findByRestStops`로 바꿨다 — 없는 파라미터를 이름이 약속하지 않도록
  - 세 호출부에서 매번 같은 값을 넘기던 인자가 사라졌고, finder 목록의 `ANY_ADMIN_OVERRIDDEN` 상수도 함께 사라졌다
  - `false` 전달을 검증하던 테스트는 지우지 않고 "항상 거르지 않는다"를 고정하도록 다시 썼다 — 어느 리포지토리가 잠금 상태를 소유하는지는 여전히 유효한 경계다
- 집계 서비스 주석의 사실과 다른 서술을 정정
  - 이 서비스를 참조하지 않는 클래스가 호출부로 적혀 있던 문장을 지웠다

## 검증

| 명령 | 결과 | 어디서 |
| --- | --- | --- |
| `./gradlew test` | 1131개 통과 / 실패 0 | CI + 로컬 |
| `./gradlew jacocoTestCoverageVerification` | 통과 (LINE 95% 게이트 유지) | 로컬 |
| `./gradlew checkstyleMain checkstyleTest` | 통과 | 로컬 |
| `./gradlew e2e` | 13개 통과 / 실패 0 | 로컬 |

CI 워크플로는 `./gradlew test`만 실행한다 — 커버리지 검증(`check`에 걸려 있다)과 e2e, checkstyle은 CI 단계에 없어 로컬에서만 돌렸다. 이 PR이 만든 상태가 아니라 기존 조건이고, 범위 밖이라 여기서 손대지 않는다.

리팩토링의 판정 기준으로 인수 테스트를 썼다.

- `git diff --stat main...HEAD -- src/e2e` 결과가 **비어 있다** — 인수 테스트를 한 줄도 고치지 않았다는 뜻이고, 고치지 않은 채 13개가 통과하므로 `/api/route-rest-stops/list`의 외부 동작이 그대로임이 확인된다
- 단위 테스트는 `@Test` 9개가 사라지고 새로 추가된 것은 0개다(`git diff main...HEAD -- src/test` 기준). 지운 케이스의 내역(위임 검증 3 + 리포지토리 쿼리 3 + 죽은 진입점 3)과 일치하고, 나머지는 전부 살아서 통과한다
- 삭제 근거는 매번 호출부 조회로 확인했다. 작업 후 `grep -rn 'findByServiceAreaCodesAndAdminOverridden' src/main` 결과는 0건이고, `AndAdminOverridden`은 리포지토리 3개와 그 직접 호출부(관련정보 조회, 주유소 백필)로만 한정된다

## 영향 범위

- **동작·API 계약 변경 없음.** 엔드포인트, 요청 파라미터, 응답 형식이 그대로다. 프론트엔드 변경 없음
- **DB 변경 없음.** 스키마와 데이터 모두 그대로이고, 지운 것은 아무도 실행하지 않던 JPQL 하나다
- 휴게소 도메인의 조회 서비스 셋과, 그 집계를 쓰는 경로 도메인 서비스 셋이 대상이다
- 관리자 재정의(override) 기능 자체는 영향받지 않는다 — 엔티티 플래그, 동기화 배치의 잠금 규칙, 관리자 API 모두 그대로다. 이번에 사라진 것은 **조회 서비스가 그 플래그로 거를 수 있다는 쓰이지 않던 선택지**뿐이다

## 참고 사항

- 근거로 삼은 규칙은 두 가지다. `rules/backend/service.md`의 *"없는 조회 방식을 미리 만들어두지는 않는다 — 실제로 그 형태의 소비자가 생겼을 때 추가한다"*, 그리고 `rules/backend/module-design.md`의 deletion test — 지웠을 때 복잡도가 호출부로 퍼지지 않으면 있으나 마나였던 것이다. 이번 대상은 어디로도 퍼지지 않았다
- 남은 정리 후보: `RestStopAggregateQueryService.toAggregate`가 파라미터를 8개 받는다. 파라미터 제거와는 성격이 다른 조립 자체의 문제라 이번 범위에서 뺐다
- 총 변경량은 **+49 / −232**다. 이 PR은 기능을 더하지 않고 빼기만 한다

🤖 Generated with [Claude Code](https://claude.com/claude-code)

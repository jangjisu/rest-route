# 휴게소 집계 조회 정리 — 구현 계획

> **작업자용:** 태스크 단위로 진행하고 각 태스크 끝에서 테스트를 돌린다. 단계는 체크박스로 추적한다.

**목표:** 아무도 쓰지 않는 조회 경로와, 한 번도 다른 값을 가진 적 없는 파라미터를 걷어낸다.

**접근:** 이번 변경은 기능을 **더하지 않고 빼기만** 한다. 지우는 근거는 전부 "프로덕션 호출부가 없다"이며, 각 삭제 전에 호출부 0을 확인한다.

**안전망:** E2E 13개 + 단위 테스트 1140개. 지운 것이 실제로 안 쓰이던 것이라면 이들이 그대로 초록불이어야 한다.

---

## 왜 지우는가

`rules/backend/service.md`:

> **없는 조회 방식을 미리 만들어두지는 않는다 — 실제로 그 형태의 소비자가 생겼을 때 추가한다(YAGNI).**

`rules/backend/module-design.md`의 deletion test:

> 이 메서드를 지웠다고 상상해본다. 복잡도가 사라지면 pass-through(있으나 마나)였던 것이고, 호출부 여러 곳으로 다시 퍼지면 제 역할을 하고 있던 것이다.

아래 대상들은 지워도 **어디로도 퍼지지 않는다.**

---

## 조사 결과

### ① 3단 래핑이 통째로 죽어 있다

```
RestStopAggregateQueryService.findByServiceAreaCodesAndAdminOverridden   프로덕션 호출부 0 (테스트 9곳만)
  ↓ 유일한 호출자
RestStopQueryService.findByServiceAreaCodesAndAdminOverridden            위 하나만 호출
  ↓ 유일한 호출자
RestStopRepository.findByServiceAreaCodesAndAdminOverridden (@Query)     위 하나만 호출
```

최상단이 안 쓰이므로 세 층이 연쇄로 죽는다.

### ② `adminOverridden`이 서비스 레이어에서 한 번도 다른 값을 가진 적이 없다

```
RouteOptionAssemblyService      ─┐
RouteRestStopListQueryService   ─┼─ 전부 null ─→ findByRestStopsAndAdminOverridden
RestStopNearbyQueryService      ─┘                       ↓ 그대로 전달
                                          findAllByRestStops(restStops, null)   ← 호출부 위 하나뿐
                                                         ↓
                                          detail/oil/food 리포지토리
```

**리포지토리 레벨에서는 살아 있다** — `RestOilServiceAreaCodeBackfiller`가
`restOilRepository.findByRestStopServiceAreaCodesAndAdminOverridden(null, false)`로 직접 쓴다.
그 경로는 서비스를 거치지 않으므로 이번 변경과 무관하다.

### ③ 주석 두 곳이 같은 사실과 다른 말을 한다

- `RestStopAggregateQueryService`: *"`RestStopServiceAreaCodeBackfillService`가 이 서비스를 통해 조합한다"* — 그 클래스는 이 서비스를 import조차 하지 않는다.
- `RestStopQueryService`: *"backfill은 코드 제한 없이 override=false인 행만 조회한다"* — 그 메서드를 backfill이 부르지 않는다.

---

## 이번 작업 범위

| | 무엇을 |
|---|---|
| **Task 1** | 죽은 3단 래핑 체인 삭제 |
| **Task 2** | 서비스 레이어의 `adminOverridden` 파라미터 제거 |
| **Task 3** | 사실과 다른 주석 정정 |

## 이번 범위 밖

| 무엇을 | 왜 |
|---|---|
| 리포지토리의 `adminOverridden` 파라미터 | `RestOilServiceAreaCodeBackfiller`가 `false`로 실제 사용 중이다 |
| `RestStopRelatedInfoQueryService`의 좁은 단건 조회들(`findDetail`, `findThemes` 등) | 여러 서비스가 실제로 쓴다. 이번 대상이 아니다 |
| `RestStopAggregateQueryService`의 `toAggregate` 파라미터 8개 | 집계 조립 자체의 문제로, 파라미터 제거와 성격이 다르다 |

---

## Task 1: 죽은 3단 래핑 체인 삭제

**Files:**
- Modify: `src/main/java/com/restroute/reststop/service/RestStopAggregateQueryService.java`
- Modify: `src/main/java/com/restroute/reststop/service/RestStopQueryService.java`
- Modify: `src/main/java/com/restroute/reststop/repository/RestStopRepository.java`
- Test: `RestStopAggregateQueryServiceTest`, `RestStopQueryServiceTest`, `RestStopRepositoryTest`

- [ ] **Step 1: 지우기 전에 호출부 0을 다시 확인한다**

```bash
grep -rn 'findByServiceAreaCodesAndAdminOverridden' src/main --include='*.java'
```

기대: 세 파일의 **정의와 내부 위임 3줄만** 나온다. 그 외 호출부가 하나라도 있으면 멈추고 그 호출부부터 확인한다.

- [ ] **Step 2: 최상단부터 지운다**

`RestStopAggregateQueryService`에서 아래 메서드 전체를 지우고, 그 메서드만 쓰던 `restStopQueryService` 의존성 필드와 import도 함께 지운다.

```java
    @Transactional(readOnly = true)
    public Map<String, RestStopAggregate> findByServiceAreaCodesAndAdminOverridden(
            Collection<String> serviceAreaCodes, Boolean adminOverridden) {
        List<RestStopEntity> restStops =
                restStopQueryService.findByServiceAreaCodesAndAdminOverridden(serviceAreaCodes, adminOverridden);
        return findByRestStopsAndAdminOverridden(restStops, adminOverridden);
    }
```

- [ ] **Step 3: 가운데 층을 지운다**

`RestStopQueryService`에서 아래를 통째로 지운다(주석 포함 — 그 주석이 ③의 거짓 서술 중 하나다).

```java
    /**
     * serviceAreaCodes가 null이거나 비어 있으면 코드로 거르지 않고, adminOverridden이 null이면
     * override 여부로 거르지 않는다. 예: 경로 조회는 특정 코드만, override 여부는 상관없이(null)
     * 조회하고, backfill은 코드 제한 없이(null) override=false인 행만 조회한다.
     */
    @Transactional(readOnly = true)
    public List<RestStopEntity> findByServiceAreaCodesAndAdminOverridden(
            Collection<String> serviceAreaCodes, Boolean adminOverridden) {
        Collection<String> normalizedCodes =
                (serviceAreaCodes == null || serviceAreaCodes.isEmpty()) ? null : serviceAreaCodes;
        return restStopRepository.findByServiceAreaCodesAndAdminOverridden(normalizedCodes, adminOverridden);
    }
```

`Collection` import가 이 메서드에서만 쓰였다면 함께 지운다.

- [ ] **Step 4: 리포지토리 쿼리를 지운다**

`RestStopRepository`에서 아래를 지운다.

```java
    @Query("""
            select r from RestStopEntity r
            where (:serviceAreaCodes is null or r.serviceAreaCode in :serviceAreaCodes)
            and (:adminOverridden is null or r.adminOverridden = :adminOverridden)
            """)
    List<RestStopEntity> findByServiceAreaCodesAndAdminOverridden(
            @Param("serviceAreaCodes") Collection<String> serviceAreaCodes,
            @Param("adminOverridden") Boolean adminOverridden);
```

`@Query`/`@Param`/`Collection` import가 다른 메서드에서 안 쓰이면 함께 지운다.

- [ ] **Step 5: 세 테스트에서 해당 케이스를 지운다**

지운 메서드를 검증하던 테스트는 함께 사라진다 — 프로덕션에 없는 동작을 지키는 테스트는 남을 이유가 없다.

- `RestStopAggregateQueryServiceTest`: `findByServiceAreaCodesAndAdminOverridden`을 호출하는 케이스. 다만 **집계 조합 자체를 검증하는 케이스는 `findByRestStops...`로 갈아타서 살린다** — 죽는 건 진입 경로이지 집계 로직이 아니다. `restStopQueryService` mock과 그 stubbing도 지운다.
- `RestStopQueryServiceTest`: 해당 메서드 테스트 3개.
- `RestStopRepositoryTest`: 해당 쿼리 테스트 3개.

- [ ] **Step 6: 검증**

Run: `./gradlew test jacocoTestCoverageVerification`
Expected: BUILD SUCCESSFUL. 커버리지가 95% 아래로 내려가면 살려야 할 케이스를 지운 것이므로 되돌아가 확인한다.

Run: `./gradlew e2e --rerun`
Expected: 13개 통과, 무수정

- [ ] **Step 7: 커밋**

```bash
git add src/main src/test
git commit -m "refactor(rest-stop): 아무도 쓰지 않는 집계 조회 경로 3단을 걷어냄"
```

---

## Task 2: 서비스 레이어의 `adminOverridden` 제거

**Files:**
- Modify: `src/main/java/com/restroute/reststop/service/RestStopAggregateQueryService.java`
- Modify: `src/main/java/com/restroute/reststop/service/RestStopRelatedInfoQueryService.java`
- Modify: `src/main/java/com/restroute/route/service/RouteOptionAssemblyService.java`
- Modify: `src/main/java/com/restroute/route/service/RouteRestStopListQueryService.java`
- Modify: `src/main/java/com/restroute/reststop/service/RestStopNearbyQueryService.java`
- Test: 위 클래스들의 테스트 + `RestStopRelatedInfoQueryServiceTest`

**Interfaces:**
- Produces: `RestStopAggregateQueryService.findByRestStops(List<RestStopEntity>)` → `Map<String, RestStopAggregate>`
- Produces: `RestStopRelatedInfoQueryService.findAllByRestStops(List<RestStopEntity>)` → `Map<String, RestStopRelatedInfo>`

- [ ] **Step 1: 집계 서비스의 파라미터를 없애고 이름을 맞춘다**

이름에 `AndAdminOverridden`이 남으면 없는 파라미터를 약속하게 되므로 함께 고친다.

```java
    /**
     * 호출부가 이미 조회해둔 {@link RestStopEntity} 목록으로 집계한다.
     * restStops를 어떤 기준으로 걸렀는지는 호출부 책임이고, 여기서는 그대로 신뢰해서 쓴다.
     */
    @Transactional(readOnly = true)
    public Map<String, RestStopAggregate> findByRestStops(List<RestStopEntity> restStops) {
        if (restStops.isEmpty()) {
            return Map.of();
        }

        List<String> codes =
                restStops.stream().map(RestStopEntity::getServiceAreaCode).toList();
        Map<String, RestStopRelatedInfo> relatedInfoByCode =
                restStopRelatedInfoQueryService.findAllByRestStops(restStops);
```

이하 본문은 그대로 둔다.

- [ ] **Step 2: 관련정보 조회의 파라미터를 없앤다**

`RestStopRelatedInfoQueryService.findAllByRestStops`에서 파라미터를 지우고, 리포지토리에는 "거르지 않는다"를 뜻하는 값을 상수로 넘긴다.

```java
    /** 관리자 재정의 여부로 거르지 않는다는 뜻. */
    private static final Boolean ANY_ADMIN_OVERRIDDEN = null;
```

```java
    @Transactional(readOnly = true)
    public Map<String, RestStopRelatedInfo> findAllByRestStops(List<RestStopEntity> restStops) {
```

본문의 `findByRestStopServiceAreaCodesAndAdminOverridden(serviceAreaCodes, adminOverridden)` 세 곳을
`findByRestStopServiceAreaCodesAndAdminOverridden(serviceAreaCodes, ANY_ADMIN_OVERRIDDEN)`으로 바꾼다.

메서드 주석에서 adminOverridden 문단을 지우고, 아래 문장만 남긴다.

```java
    /**
     * findDetail/findHighwayServiceAreaInfos 등 이 클래스의 좁은 조회 메서드를 후보마다 반복
     * 호출하면 후보 수만큼 쿼리가 나가므로, 여러 휴게소를 한 번에 다루는 호출부는 이 배치 메서드를 쓴다.
     *
     * <p>주유 가격은 `rest_oil`에서 파생된 연결을 사용하고 theme/event는 override 개념이 없다.
     */
```

- [ ] **Step 3: 호출부 세 곳을 고친다**

```java
// RouteOptionAssemblyService
return restStopAggregateQueryService.findByRestStops(selected);

// RouteRestStopListQueryService — ANY_ADMIN_OVERRIDDEN 상수도 함께 지운다
return restStopAggregateQueryService.findByRestStops(selected);

// RestStopNearbyQueryService
restStopAggregateQueryService.findByRestStops(restStops);
```

- [ ] **Step 4: 테스트를 맞춘다**

- 호출부 테스트 4개(`RouteOptionAssemblyServiceTest`, `RouteRestStopServiceTest`, `RouteRestStopListQueryServiceTest`, `RestStopNearbyQueryServiceTest`): `findByRestStopsAndAdminOverridden(any(), any())` → `findByRestStops(any())`, `isNull()` 인자 검증은 인자가 사라졌으므로 제거한다.
- `RestStopRelatedInfoQueryServiceTest`: 리포지토리 stubbing의 두 번째 인자는 `null`로 고정된다. `adminOverridden=false`를 넘기던 테스트는 서비스에서 그 경로가 사라졌으므로 지운다 — 리포지토리 자체의 필터 동작은 `RestOilRepositoryTest`·`RestStopDetailRepositoryTest`가 이미 덮는다.

- [ ] **Step 5: 남은 참조가 없는지 확인한다**

```bash
grep -rn 'AndAdminOverridden' src/main --include='*.java'
```

기대: 리포지토리 인터페이스 3개(`RestOilRepository`, `RestStopDetailRepository`, `RestFoodRepository`)와 `RestStopRelatedInfoQueryService` 내부 호출, `RestOilServiceAreaCodeBackfiller`만 남는다. 서비스 레벨 public 메서드에는 남지 않는다.

- [ ] **Step 6: 검증**

Run: `./gradlew test jacocoTestCoverageVerification`
Expected: BUILD SUCCESSFUL

Run: `./gradlew e2e --rerun`
Expected: 13개 통과, 무수정

- [ ] **Step 7: 커밋**

```bash
git add src/main src/test
git commit -m "refactor(rest-stop): 서비스 레이어에서 쓰이지 않는 adminOverridden 파라미터 제거"
```

---

## Task 3: 사실과 다른 주석 정정

**Files:**
- Modify: `src/main/java/com/restroute/reststop/service/RestStopAggregateQueryService.java`
- Modify: `docs/domain/rest-stop.md` (해당 서술이 있으면)

- [ ] **Step 1: 클래스 주석에서 거짓 서술을 걷어낸다**

`RestStopServiceAreaCodeBackfillService`는 이 서비스를 쓰지 않으므로 지운다. 삭제된 진입 경로를 설명하던 문장도 함께 지운다.

```java
/**
 * REST_STOP_SERVICE_AREA_CODE로 연결되는 휴게소 관련 정보(상세/주유/음식/테마/이벤트/EV차저/이미지)를
 * 한 번에 조합해 반환한다.
 *
 * <p>여러 QueryService를 각자 호출해 서비스 코드 기준으로 직접 짜맞추는 일은 호출부가 아니라
 * 여기서만 한다.
 */
```

- [ ] **Step 2: 도메인 문서에 같은 서술이 있는지 확인한다**

```bash
grep -rn 'adminOverridden\|AggregateQueryService' docs/domain/
```

집계 서비스의 진입점이나 `adminOverridden` 파라미터를 설명하는 문장이 있으면 현재에 맞춘다.

- [ ] **Step 3: 검증 후 커밋**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL

```bash
git add src/main docs/domain
git commit -m "docs(rest-stop): 집계 서비스 주석의 사실과 다른 서술 정정"
```

---

## 완료 기준

- `./gradlew test` 통과, 커버리지 95% 유지
- `./gradlew e2e` 13개 **무수정** 통과 — 지운 것이 실제로 안 쓰이던 것이라는 증거
- `grep -rn 'findByServiceAreaCodesAndAdminOverridden' src/main` 결과 **0건**
- `grep -rn 'AndAdminOverridden' src/main`이 리포지토리 3개와 그 직접 호출부(`RestStopRelatedInfoQueryService`, `RestOilServiceAreaCodeBackfiller`)로만 한정됨
- `RestStopAggregateQueryService` 의존성이 8개 → 7개
- 서비스 레벨 public 메서드 시그니처에 `Boolean adminOverridden`이 남지 않음

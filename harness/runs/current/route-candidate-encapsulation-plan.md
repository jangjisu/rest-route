# 경로 위 후보 찾기 캡슐화 — 구현 계획

> **작업자용:** 태스크 단위로 진행하고 각 태스크 끝에서 테스트를 돌린다. 단계는 체크박스로 추적한다.

**목표:** "출발지·목적지 좌표를 주면 그 경로 위 후보 휴게소가 나온다"를 인터페이스 하나 뒤로 감춘다.

**이건 중복 제거 작업이 아니다.** 두 곳에 같은 코드가 있는 것은 증상이고, 원인은 **하나의 도메인 행위가 이름과 경계를 갖지 못한 채 호출부에 펼쳐져 있다**는 것이다. 결과적으로 중복이 사라지지만, 판단 기준은 "같은 코드가 두 번 있나"가 아니라 "이 행위가 제 이름을 갖고 있나"다.

**근거 규칙:** `rules/backend/module-design.md`

> 1. 목적지 좌표 계산 2. 카카오 길찾기 호출 3. **경로 근처 휴게소 후보 탐색** 4. 연관 정보 조합
> 1~3번은 아직 한 클래스 안에 뒤섞여 있다. 다음에 이 메서드를 손볼 일이 생기면(예: **경로 탐색 방식이 하나 더 추가되거나**, 후보 탐색 로직이 복잡해지면) 1~3번도 같은 기준으로 분리 대상인지 판단한다.
>
> **어댑터 1개는 가상의 seam, 2개부터 진짜 seam이다.**

`/api/route-rest-stops/list`가 두 번째 경로 탐색 방식이다. 문서가 지정한 분리 시점에 이미 도달해 있다.

**용어:** `CONTEXT.md`의 **경로 위 후보(Route Rest Stop Candidate)** — *"경로 좌표 주변 반경 안에 있어 응답에 포함될 가능성이 있는 휴게소. 아직 관련 정보(집계)가 조합되기 전 단계."* 이 정의가 seam 위치를 말해준다 — **집계 조합 직전**이 경계다.

**안전망:** E2E 13개 + 단위 테스트 1135개. 동작을 바꾸지 않으므로 이들이 무수정으로 초록불을 유지하는 것이 검증이다.

---

## Seam을 어디에 긋는가

호출부가 알아야 하는 것을 최소로 줄인 자리가 경계다.

```
지금 — 호출부가 4단계를 다 안다
  resolveDestinationAndRoute(...) → 원본 경로
  restStopQueryService.findAll()  → 휴게소 전체
  좌표 축소 (경로마다)
  매칭 (경로마다)

목표 — 호출부는 좌표 두 개만 안다
  routeCandidateFinder.find(originLat, originLng, destination) → RouteCandidates
```

**Deletion test:** 이 모듈을 지우면 위 4단계가 두 호출부로 다시 퍼진다 → pass-through가 아니라 제 역할을 하는 모듈이다.

**Depth:** 인터페이스는 좌표 2개 + 목적지, 구현은 길찾기 호출·휴게소 조회·좌표 축소·반경 매칭·방향 판정·빈 경로 정책. 인터페이스보다 구현이 훨씬 두껍다 → deep.

두 서비스에 남는 차이는 **목적지를 어떻게 정하나**와 **결과를 어떻게 조립하나** 둘뿐이다.

---

## 이번 작업 범위

| | 무엇을 |
|---|---|
| **Task 1** | 매칭 반경을 요청 파라미터에서 서버 설정으로 |
| **Task 2** | `RouteCandidateFinder` 신설 (아직 아무도 쓰지 않음) |
| **Task 3** | 두 서비스가 그 모듈을 쓰게 교체 |
| **Task 4** | 집계 조회의 매직 `null`에 이름 부여 |

## 이번 범위 밖

| 무엇을 | 왜 |
|---|---|
| `RouteCandidates`가 `allRestStops`를 함께 반환하는 것 | 매칭 결과가 엔티티를 잃어 호출부가 다시 찾아야 하는 구조 때문이다. 근본 해결은 `RouteOptionAssemblyService`의 계약까지 바꿔야 해서 이번 diff와 섞으면 읽을 수 없다. 이번엔 그 사정을 값 객체 한 곳에 모으는 데까지만 한다 |
| `RouteRestStopListQueryService`의 응답 조립 분리 (`toItem` 파라미터 8개, `evChargerCount` 판정) | Task 3이 끝나면 이 클래스 의존성이 10개 → 7개가 된다. 줄어든 상태를 보고 판단한다 |
| `RouteResolverService`가 목적지 해석과 길찾기 두 책임을 갖는 것 | 이번에 `resolveDestinationAndRoute`가 사라지면서 드러나지만, 쪼개면 지도 화면 계약까지 건드린다 |
| 파라미터 6~7개 → 값 객체 | 컨트롤러 계약 변경이라 별도 판단 |

---

## 파일 구조

**새로 만들 것**

- `route/service/RouteCandidateFinder.java` — 좌표 두 개와 목적지를 받아 경로 위 후보를 찾는다. 길찾기 호출·휴게소 조회·좌표 축소·매칭·빈 경로 정책을 소유한다.
- `route/service/dto/RouteCandidates.java` — 경로별 후보 목록과, 그 후보를 찾는 데 쓴 휴게소 전체.

**고칠 것**

- `route/service/RouteRestStopMatcher.java` — 반경을 생성자로 주입받는다.
- `route/service/RouteResolverService.java` — `resolveDestination`을 public으로 올리고 `resolveDestinationAndRoute`를 지운다.
- `route/service/RouteRestStopService.java` — 4단계가 `find` 한 줄이 된다.
- `route/service/RouteRestStopListQueryService.java` — 같은 모듈을 쓰고 첫 후보만 고른다.
- `route/controller/RouteRestStopController.java` — 두 엔드포인트에서 `radiusMeters` 제거.
- `resources/application.properties` — `route.match-radius-meters` 추가.

**따라 고칠 것**

- `RouteRestStopMatcherTest` / `RouteRestStopServiceTest` / `RouteRestStopListQueryServiceTest` / `RouteRestStopControllerTest`
- `API.md`, `docs/domain/route.md`, `docs/domain/finder/contracts/api-contracts.md`

---

## Task 1: 매칭 반경을 서버 설정으로

프론트는 이 파라미터를 보내지 않는다. 매칭 반경은 요청이 정할 값이 아니라 **매칭 알고리즘의 설정**이므로, 그 알고리즘을 가진 클래스가 직접 갖는다.

**Files:**
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/java/com/restroute/route/service/RouteRestStopMatcher.java`
- Modify: `src/main/java/com/restroute/route/service/RouteRestStopService.java`
- Modify: `src/main/java/com/restroute/route/service/RouteRestStopListQueryService.java`
- Modify: `src/main/java/com/restroute/route/controller/RouteRestStopController.java`
- Test: `RouteRestStopMatcherTest`, `RouteRestStopServiceTest`, `RouteRestStopListQueryServiceTest`, `RouteRestStopControllerTest`

**Interfaces:**
- Produces: `RouteRestStopMatcher.match(RoutePath path, List<RestStopEntity> allRestStops)` — Task 2가 이 시그니처를 쓴다.

- [ ] **Step 1: 프로퍼티 추가**

`src/main/resources/application.properties`의 Feign 설정 블록 앞에 넣는다.

```properties
# 경로에서 휴게소를 포함할 반경(m). 요청이 정하는 값이 아니라 매칭 알고리즘의 설정이다.
route.match-radius-meters=1000
```

- [ ] **Step 2: 테스트를 새 시그니처로 바꾼다**

`RouteRestStopMatcherTest`에서 matcher를 만드는 부분과 호출부를 바꾼다.

```java
private final RouteRestStopMatcher matcher = new RouteRestStopMatcher(1000);

// 호출: matcher.match(path, restStops)   ← 반경 인자 제거
```

- [ ] **Step 3: 실패를 확인한다**

Run: `./gradlew compileTestJava`
Expected: FAIL — `constructor RouteRestStopMatcher in class RouteRestStopMatcher cannot be applied to given types`

- [ ] **Step 4: matcher가 반경을 갖게 한다**

```java
@Component
public class RouteRestStopMatcher {

    private static final int AMBIGUITY_CHECK_MIN_GROUP_SIZE = 2;
    private static final int SINGLE_REACHABLE_MATCH_COUNT = 1;

    private final int radiusMeters;

    public RouteRestStopMatcher(@Value("${route.match-radius-meters}") int radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public List<RouteRestStopItem> match(RoutePath path, List<RestStopEntity> allRestStops) {
        List<RouteRestStopCandidate> matched = matchRestStopsToPath(path, allRestStops);
        return removeUnreachableSide(matched, path);
    }
```

`matchRestStopsToPath`와 `matchOne`의 `int radiusMeters` 파라미터도 지우고 필드를 직접 쓴다. 클래스 주석의 "의존성 없는 순수 알고리즘" 서술은 그대로 둔다 — 설정값 주입은 협력 객체 의존이 아니다.

- [ ] **Step 5: 두 서비스에서 반경을 걷어낸다**

`RouteRestStopService.matchRestStopsByDirection`과 `RouteRestStopListQueryService.findRouteRestStops`에서 `match(...)` 호출의 반경 인자를 지우고, 두 메서드의 `int radiusMeters` 파라미터도 지운다.

- [ ] **Step 6: 컨트롤러에서 파라미터를 없앤다**

두 엔드포인트에서 아래 세 가지를 모두 지운다.

```java
private static final int DEFAULT_RADIUS_METERS = 1000;
@RequestParam(required = false, defaultValue = "" + DEFAULT_RADIUS_METERS) int radiusMeters
* @param radiusMeters 경로에서 휴게소를 포함할 반경(m). 예: 1000
```

서비스 호출에서도 인자를 뺀다.

- [ ] **Step 7: 나머지 테스트를 맞춘다**

`RouteRestStopServiceTest`·`RouteRestStopListQueryServiceTest`는 `findRouteRestStops(...)` 호출에서 `1000`을 빼고 matcher 생성을 `new RouteRestStopMatcher(1000)`으로 바꾼다. `RouteRestStopControllerTest`는 스텁의 `eq(1000)` 인자를 뺀다.

- [ ] **Step 8: 검증**

Run: `./gradlew test jacocoTestCoverageVerification`
Expected: BUILD SUCCESSFUL, 실패 0

Run: `./gradlew e2e --rerun`
Expected: 13개 통과. **E2E는 `radiusMeters`를 보낸 적이 없으므로 무수정 통과해야 한다.** 여기서 깨지면 파라미터를 잘못 지운 것이다.

- [ ] **Step 9: 문서를 맞춘다**

`API.md`의 두 엔드포인트 파라미터 표에서 `radiusMeters` 행을 지운다. `docs/domain/route.md` §8 진입점 파라미터 목록, `docs/domain/finder/contracts/api-contracts.md`의 `/list` 파라미터 목록에서도 지운다.

- [ ] **Step 10: 커밋**

```bash
git add src/main src/test API.md docs/domain
git commit -m "refactor(route): 매칭 반경을 요청 파라미터에서 서버 설정으로 옮김"
```

---

## Task 2: `RouteCandidateFinder` 신설

이 태스크가 끝나도 아무도 이 모듈을 쓰지 않는다. 모듈이 혼자 서고 자기 테스트로 증명되는 것까지가 이 태스크의 산출물이다.

**Files:**
- Create: `src/main/java/com/restroute/route/service/dto/RouteCandidates.java`
- Create: `src/main/java/com/restroute/route/service/RouteCandidateFinder.java`
- Test: `src/test/java/com/restroute/route/service/RouteCandidateFinderTest.java`

**Interfaces:**
- Consumes: `RouteRestStopMatcher.match(RoutePath, List<RestStopEntity>)` (Task 1), `RouteResolverService.resolveRoute(double, double, Destination)`
- Produces: `RouteCandidateFinder.find(double originLatitude, double originLongitude, Destination destination)` → `RouteCandidates`. Task 3의 두 서비스가 이것만 호출한다.

- [ ] **Step 1: 값 객체를 만든다**

```java
package com.restroute.route.service.dto;

import com.restroute.reststop.domain.RestStopEntity;
import java.util.List;

/**
 * 경로별 후보 휴게소와, 그 후보를 찾는 데 쓴 휴게소 전체.
 *
 * <p>휴게소 전체를 함께 담는 건 후보에 담긴 {@code RouteRestStopItem}이 엔티티가 아니어서,
 * 뒤이어 집계를 조회하는 쪽이 코드로 엔티티를 다시 찾아야 하기 때문이다. 호출부가 같은 조회를
 * 반복하지 않도록 여기 함께 돌려준다.
 */
public record RouteCandidates(List<RouteCandidate> candidates, List<RestStopEntity> allRestStops) {

    /** 대안 경로를 쓰지 않는 호출부가 첫 경로만 고를 때 쓴다. */
    public RouteCandidate first() {
        return candidates.get(0);
    }
}
```

- [ ] **Step 2: 실패하는 테스트를 쓴다**

```java
package com.restroute.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.restroute.common.client.response.KakaoDirectionsResponse;
import com.restroute.common.client.response.KakaoDirectionsResponse.Road;
import com.restroute.common.client.response.KakaoDirectionsResponse.Route;
import com.restroute.common.client.response.KakaoDirectionsResponse.Section;
import com.restroute.common.client.response.KakaoDirectionsResponse.Summary;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.RestStopQueryService;
import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.service.RouteResolverService.RawRouteResult;
import com.restroute.route.service.dto.RouteCandidates;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteCandidateFinderTest {

    private static final List<Double> VERTEXES = List.of(127.0, 37.0, 127.5, 37.5);
    private static final Destination DESTINATION = Destination.of("부산역", 35.0, 129.0);

    @Mock
    private RouteResolverService routeResolverService;

    @Mock
    private RestStopQueryService restStopQueryService;

    private RouteCandidateFinder finder;

    @BeforeEach
    void setUp() {
        finder = new RouteCandidateFinder(
                routeResolverService,
                restStopQueryService,
                new RouteCoordinateReducer(),
                new RouteRestStopMatcher(1000));
    }

    @Test
    @DisplayName("경로마다 인덱스를 매겨 후보를 만든다")
    void find_indexesEachRoute() {
        stubRoutes(route(VERTEXES), route(VERTEXES));
        when(restStopQueryService.findAll()).thenReturn(List.of());

        RouteCandidates result = finder.find(37.0, 127.0, DESTINATION);

        assertThat(result.candidates()).extracting(candidate -> candidate.routeIndex())
                .containsExactly(0, 1);
    }

    @Test
    @DisplayName("반경 안 휴게소를 그 경로의 후보로 담는다")
    void find_attachesMatchedRestStops() {
        stubRoutes(route(VERTEXES));
        RestStopEntity nearby = restStop("A", "127.0001", "37.0001");
        when(restStopQueryService.findAll()).thenReturn(List.of(nearby));

        RouteCandidates result = finder.find(37.0, 127.0, DESTINATION);

        assertThat(result.first().items()).extracting(item -> item.serviceAreaCode())
                .containsExactly("A");
    }

    @Test
    @DisplayName("후보를 찾는 데 쓴 휴게소 전체를 함께 돌려준다")
    void find_returnsAllRestStopsItSearched() {
        stubRoutes(route(VERTEXES));
        RestStopEntity faraway = restStop("B", "128.9", "38.9");
        when(restStopQueryService.findAll()).thenReturn(List.of(faraway));

        RouteCandidates result = finder.find(37.0, 127.0, DESTINATION);

        assertThat(result.allRestStops()).containsExactly(faraway);
        assertThat(result.first().items()).isEmpty();
    }

    @Test
    @DisplayName("좌표가 하나도 없는 경로만 오면 NotFound이고 휴게소를 조회하지 않는다")
    void find_throwsWhenEveryRoutePathIsEmpty() {
        stubRoutes(route(List.of()));

        assertThatThrownBy(() -> finder.find(37.0, 127.0, DESTINATION))
                .isInstanceOf(RouteRestStopNotFoundException.class);

        verifyNoInteractions(restStopQueryService);
    }

    private void stubRoutes(Route... routes) {
        lenient()
                .when(routeResolverService.resolveRoute(anyDouble(), anyDouble(), any()))
                .thenReturn(new RawRouteResult(DESTINATION, List.of(routes)));
    }

    private Route route(List<Double> vertexes) {
        return new Route(0, new Summary(100L, 200L, null), List.of(new Section(List.of(new Road(vertexes)))));
    }

    private RestStopEntity restStop(String code, String lng, String lat) {
        RestStopEntity entity = mock(RestStopEntity.class);
        lenient().when(entity.getServiceAreaCode()).thenReturn(code);
        lenient().when(entity.getUnitName()).thenReturn(code + "휴게소");
        lenient().when(entity.getRouteName()).thenReturn("경부선");
        lenient().when(entity.getRouteNo()).thenReturn("0010");
        lenient().when(entity.getXValue()).thenReturn(lng);
        lenient().when(entity.getYValue()).thenReturn(lat);
        return entity;
    }
}
```

- [ ] **Step 3: 실패를 확인한다**

Run: `./gradlew test --tests '*RouteCandidateFinderTest'`
Expected: FAIL — `cannot find symbol: class RouteCandidateFinder`

- [ ] **Step 4: 모듈을 만든다**

좌표를 줄이는 것이 먼저이고 휴게소 조회가 나중인 순서를 지킨다 — 경로가 비어 끝날 요청에서 불필요한 전체 조회를 하지 않는다(위 네 번째 테스트가 이 순서를 고정한다).

```java
package com.restroute.route.service;

import com.restroute.common.client.response.KakaoDirectionsResponse;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.service.RestStopQueryService;
import com.restroute.route.controller.response.RouteRestStopResponse.Destination;
import com.restroute.route.controller.response.RouteRestStopResponse.RouteRestStopItem;
import com.restroute.route.service.RouteResolverService.RawRouteResult;
import com.restroute.route.service.dto.ResolvedRoute.RouteGeometry;
import com.restroute.route.service.dto.RouteCandidate;
import com.restroute.route.service.dto.RouteCandidates;
import com.restroute.route.service.exception.RouteRestStopNotFoundException;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 출발지와 목적지를 주면 그 경로 위 후보 휴게소를 찾는다. 길찾기 호출부터 반경·방향 매칭까지가
 * 이 인터페이스 뒤에 있고, 호출부는 좌표와 목적지만 안다.
 *
 * <p>후보는 집계가 조합되기 전 단계다 — 어떤 정보를 얹어 응답으로 만들지는 호출부가 정한다.
 */
@Service
@RequiredArgsConstructor
public class RouteCandidateFinder {

    private final RouteResolverService routeResolverService;
    private final RestStopQueryService restStopQueryService;
    private final RouteCoordinateReducer routeCoordinateReducer;
    private final RouteRestStopMatcher routeRestStopMatcher;

    public RouteCandidates find(double originLatitude, double originLongitude, Destination destination) {
        RawRouteResult raw = routeResolverService.resolveRoute(originLatitude, originLongitude, destination);
        List<RouteGeometry> routes = reduceToNonEmptyPaths(raw.routes());

        List<RestStopEntity> allRestStops = restStopQueryService.findAll();
        List<RouteCandidate> candidates = IntStream.range(0, routes.size())
                .mapToObj(routeIndex -> toCandidate(routeIndex, routes.get(routeIndex), allRestStops))
                .toList();

        return new RouteCandidates(candidates, allRestStops);
    }

    private List<RouteGeometry> reduceToNonEmptyPaths(List<KakaoDirectionsResponse.Route> rawRoutes) {
        List<RouteGeometry> routes = rawRoutes.stream()
                .map(routeCoordinateReducer::reduce)
                .filter(geometry -> !geometry.path().isEmpty())
                .toList();
        if (routes.isEmpty()) {
            throw RouteRestStopNotFoundException.emptyRoutePath();
        }
        return routes;
    }

    private RouteCandidate toCandidate(int routeIndex, RouteGeometry geometry, List<RestStopEntity> allRestStops) {
        List<RouteRestStopItem> items = routeRestStopMatcher.match(geometry.path(), allRestStops);
        return new RouteCandidate(routeIndex, geometry, items);
    }
}
```

- [ ] **Step 5: 통과를 확인한다**

Run: `./gradlew test --tests '*RouteCandidateFinderTest'`
Expected: PASS 4개

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/restroute/route/service/RouteCandidateFinder.java \
        src/main/java/com/restroute/route/service/dto/RouteCandidates.java \
        src/test/java/com/restroute/route/service/RouteCandidateFinderTest.java
git commit -m "feat(route): 경로 위 후보를 찾는 모듈 추가"
```

---

## Task 3: 두 서비스가 그 모듈을 쓰게 교체

**Files:**
- Modify: `src/main/java/com/restroute/route/service/RouteResolverService.java`
- Modify: `src/main/java/com/restroute/route/service/RouteRestStopService.java`
- Modify: `src/main/java/com/restroute/route/service/RouteRestStopListQueryService.java`
- Test: `RouteRestStopServiceTest`, `RouteRestStopListQueryServiceTest`, `RouteResolverServiceTest`

**Interfaces:**
- Consumes: `RouteCandidateFinder.find(...)` → `RouteCandidates` (Task 2)
- Produces: `RouteResolverService.resolveDestination(String query, Double lat, Double lng, String name)` → `Destination` — 지도 화면이 검색어를 좌표로 바꿀 때 쓴다.

- [ ] **Step 1: `resolveDestination`을 공개하고 합성 메서드를 지운다**

`RouteResolverService`에서 `private Destination resolveDestination(...)`을 `public`으로 바꾸고, `resolveDestinationAndRoute`를 통째로 지운다. 지금 이 메서드의 유일한 호출자는 `RouteRestStopService`이고 Step 2에서 대체된다.

```java
    public Destination resolveDestination(
            String destinationQuery, Double destinationLatitude, Double destinationLongitude, String destinationName) {
        if (destinationLatitude == null || destinationLongitude == null) {
            return destinationFromQuery(destinationQuery);
        }
        String name = destinationName == null || destinationName.isBlank() ? "목적지" : destinationName;
        return Destination.of(name, destinationLatitude, destinationLongitude);
    }
```

클래스 주석에서 `resolveDestinationAndRoute`를 가리키던 문장도 함께 고친다.

```java
/**
 * 목적지를 정하고, 카카오 길찾기를 호출해서 대안 경로까지 포함한 원본 경로 응답을 받아온다.
 * 좌표열 축약(다운샘플링)은 여기서 하지 않는다 — 원본 그대로 돌려준다.
 * 길찾기 실패는 여기서 바로 예외로 끝낸다.
 */
```

- [ ] **Step 2: `RouteRestStopService`가 모듈을 쓰게 한다**

`reduceCoordinates`·`matchRestStopsByDirection` 두 private 메서드를 지우고, 의존성에서 `routeCoordinateReducer`·`routeRestStopMatcher`·`restStopQueryService`를 빼고 `routeCandidateFinder`를 넣는다. 번호 주석(`// 1. …`)도 지운다 — 메서드 이름이 같은 말을 한다.

```java
@Service
@RequiredArgsConstructor
public class RouteRestStopService {

    private final RouteResolverService routeResolverService;
    private final RouteCandidateFinder routeCandidateFinder;
    private final NationalOilPriceService nationalOilPriceService;
    private final RouteOptionAssemblyService routeOptionAssemblyService;

    public RouteRestStopResponse findRouteRestStops(
            double originLatitude,
            double originLongitude,
            String destinationQuery,
            Double destinationLatitude,
            Double destinationLongitude,
            String destinationName) {
        Destination destination = routeResolverService.resolveDestination(
                destinationQuery, destinationLatitude, destinationLongitude, destinationName);
        RouteCandidates found = routeCandidateFinder.find(originLatitude, originLongitude, destination);

        Optional<NationalOilPriceSummary> nationalOilPriceSummary = nationalOilPriceService.getTodaySummary();
        List<RouteOption> routeOptions = routeOptionAssemblyService.attachDetails(
                found.candidates(), found.allRestStops(), nationalOilPriceSummary);

        return RouteRestStopResponse.of(destination, routeOptions);
    }
}
```

클래스 주석도 새 흐름에 맞춘다.

```java
/**
 * 지도 화면의 경로 휴게소 검색. 목적지를 정하고, 경로 위 후보를 찾고, 후보에 상세 정보를 얹는다.
 */
```

- [ ] **Step 3: `RouteRestStopListQueryService`가 같은 모듈을 쓰게 한다**

`firstReducedRoute`를 지우고 의존성에서 `routeCoordinateReducer`·`routeRestStopMatcher`·`restStopQueryService`를 빼고 `routeCandidateFinder`를 넣는다. 조회 앞부분이 아래로 바뀐다.

```java
        Destination destination =
                destinationResolver.resolve(destinationLatitude, destinationLongitude, destinationName);
        RouteCandidates found = routeCandidateFinder.find(originLatitude, originLongitude, destination);

        List<RouteRestStopItem> matched = found.first().items();
        if (matched.isEmpty()) {
            return List.of();
        }

        Map<String, RestStopAggregate> aggregatesByServiceAreaCode = aggregatesFor(matched, found.allRestStops());
```

`first()`가 안전한 이유: `find`는 좌표가 남은 경로가 하나도 없으면 예외로 끝나므로 빈 목록을 돌려주지 않는다.

- [ ] **Step 4: 두 서비스 테스트를 맞춘다**

두 테스트의 생성자 인자에서 `new RouteCoordinateReducer()`·`new RouteRestStopMatcher(...)`·`restStopQueryService`를 빼고, 그 자리에 finder를 넣는다.

```java
        RouteCandidateFinder finder = new RouteCandidateFinder(
                new RouteResolverService(kakaoMapClient),
                restStopQueryService,
                new RouteCoordinateReducer(),
                new RouteRestStopMatcher(1000));
```

`RouteResolverServiceTest`에서 `resolveDestinationAndRoute`를 부르던 테스트는 `resolveDestination` + `resolveRoute` 두 호출로 나누거나, 목적지 해석만 검증하도록 좁힌다. 기존 검증 내용(빈 검색 결과 → NotFound, 좌표 우선, 표시명 기본값)은 전부 유지한다.

- [ ] **Step 5: 검증**

Run: `./gradlew test jacocoTestCoverageVerification`
Expected: BUILD SUCCESSFUL

Run: `./gradlew e2e --rerun`
Expected: 13개 통과. **E2E를 한 줄이라도 고쳐야 한다면 동작이 바뀐 것이므로 리팩토링이 아니다** — 그 경우 되돌리고 원인을 찾는다.

- [ ] **Step 6: 도메인 문서를 맞춘다**

`docs/domain/route.md` §3 흐름에서 1~3단계 소유자를 `RouteCandidateFinder`로 바꾸고, §8 코드 경계의 협력자 목록에 추가한다. `RouteResolverService`가 목적지 해석과 길찾기 두 진입점을 갖는다는 것도 반영한다.

- [ ] **Step 7: 커밋**

```bash
git add src/main src/test docs/domain
git commit -m "refactor(route): 두 경로 조회가 후보 찾기 모듈을 통해 지나가게 함"
```

---

## Task 4: 집계 조회의 매직 `null`

**Files:**
- Modify: `src/main/java/com/restroute/route/service/RouteRestStopListQueryService.java`

- [ ] **Step 1: 두 번째 인자의 의미를 확인한다**

Run: `grep -n "findByRestStopsAndAdminOverridden" -A 10 src/main/java/com/restroute/reststop/service/RestStopAggregateQueryService.java`

`null`이 "관리자 재정의 여부로 거르지 않음"을 뜻하는지 시그니처와 본문으로 확인한다. 다른 의미라면 아래 상수 이름을 그 의미에 맞게 짓는다.

- [ ] **Step 2: 이름을 준다**

```java
    /** 관리자 재정의 여부로 거르지 않는다. */
    private static final Boolean ANY_ADMIN_OVERRIDDEN = null;
```

```java
        return restStopAggregateQueryService.findByRestStopsAndAdminOverridden(selected, ANY_ADMIN_OVERRIDDEN);
```

- [ ] **Step 3: 검증 후 커밋**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL

```bash
git add src/main
git commit -m "refactor(route): 집계 조회의 매직 null에 이름을 줌"
```

---

## 완료 기준

- `./gradlew test` 통과, 커버리지 95% 유지
- **`./gradlew e2e` 13개가 한 줄도 수정하지 않고 통과** — 이게 동작 보존의 증거다
- `./gradlew check`가 이번 변경으로 새로 깨지지 않음(`spotbugsMain`은 이전부터 실패 중)
- `grep -rn "radiusMeters" src/main`이 `RouteRestStopMatcher` 안으로만 한정됨
- 두 서비스에서 `routeCoordinateReducer`·`routeRestStopMatcher`·`restStopQueryService` 직접 의존이 사라짐
- `RouteRestStopService`가 4개 의존성으로 줄어듦(현재 6개)
- **Deletion test:** `RouteCandidateFinder`를 지웠다고 상상했을 때 길찾기·조회·축소·매칭이 두 호출부로 다시 퍼지는가? 퍼진다면 제 역할을 하는 모듈이다

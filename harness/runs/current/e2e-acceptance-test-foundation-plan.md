# E2E 인수 테스트 기반 구축 — 계획

작성일: 2026-09-09
상태: 설계 확정 대기 (코드 미착수)

---

## 1. 왜 지금 이걸 하는가

현재 테스트 자산은 이렇다.

| 종류 | 개수 |
|---|---|
| Java 테스트 파일 | 237 |
| JS 테스트 파일 | 30+ |
| `@DataJpaTest` | 23 |
| `MockMvc` 사용 | 29 |
| **실제 서버를 띄워 HTTP로 호출하는 테스트** | **0** (`TestRestTemplate` 0건) |
| Playwright / Cypress / Selenium | 없음 |
| WireMock / Testcontainers | 없음 |

부품 단위 검증은 촘촘하지만, **조립된 상태로 굴러가는지 확인하는 테스트가 하나도 없다.**

그리고 이 빈칸은 회복탄력성 작업의 전제조건이기도 하다. "외부 API가 죽으면
어떻게 되는가"를 관찰하려면 먼저 **서버를 진짜로 띄우고 외부 API 자리를
내가 조종할 수 있어야** 한다. 그게 곧 E2E 뼈대다.

목표는 두 개를 동시에 만족시키는 것:

1. 지금 없는 E2E 테스트 계층을 만든다.
2. 그 위에 장애 시나리오를 얹을 수 있는 구조로 만든다.

---

## 2. 첫 대상 API 선정

### 후보 비교

| 후보 | 엔드포인트 | 외부 API | 응답 크기 | 판단 |
|---|---|---|---|---|
| A | `GET /api/route-rest-stops` | 카카오 로컬 + 모빌리티 | **큼** — 대안 경로 전체, 이미지, 먹거리, 비교/추천 조립 | 검증 표면이 넓어 첫 판으론 과함 |
| **B** | **`GET /api/route-rest-stops/list`** | **카카오 로컬 + 모빌리티** | **작음** — 첫 경로만, 거리순 목록 | **선정** |
| C | `GET /api/flight-search` 계열 | Travelpayouts ×최대 20 | 중간 | fan-out 검증용. 2차로 미룸 |
| D | 휴게소 근처 검색 | **없음** | 작음 | 외부 API를 안 타서 WireMock 검증이 안 됨 |

### B를 고른 이유

**① 외부 API 체인이 실제로 있다**

`RouteResolverService`가 두 단계를 순서대로 탄다.

```
카카오 로컬 (지오코딩)  →  카카오 모빌리티 (길찾기)
  searchKeyword()          getDirections()
```

앞단이 죽으면 뒷단은 아예 호출되지 않는다. 회복탄력성에서 보고 싶은
"체인 중간 실패"가 그대로 들어있다.

**② A보다 검증 표면이 훨씬 작다**

A(`RouteRestStopService`)는 `RouteOptionAssemblyService`를 거쳐 대안 경로
전체, 휴게소 이미지, 먹거리, 비교 요약, 추천 태그까지 조립한다. 첫 E2E가
이걸 다 검증하려 들면 fixture만 수십 줄이 된다.

B(`RouteRestStopListQueryService`)는 대안 경로를 계산하지 않고 **첫 번째
경로만** 쓴다(`firstReducedRoute`). WireMock 응답에 경로 1개만 담으면 된다.

**③ 실패했을 때 사용자 영향이 가장 직접적이다**

모바일 finder의 "목적지로 추천받기"가 이 엔드포인트를 쓴다. 이게 죽으면
모바일 핵심 기능 하나가 통째로 죽는다. 나중에 fallback을 설계할 때
1순위가 될 지점이라, 여기에 먼저 계측을 걸어두는 게 맞다.

**④ 지금 코드에 대비책이 전혀 없는 게 확인됐다**

`RouteResolverService` 클래스 주석: *"길찾기 실패는 여기서 바로 예외로
끝낸다."* 카카오가 죽으면 `GlobalExceptionHandler`가 에러 응답으로 바꿔
사용자에게 그대로 내보낸다. AS-IS를 기록할 대상으로 명확하다.

### 첫 판에서 `fuelType` 파라미터는 안 쓴다

`fuelType`이 있으면 `NationalOilPriceService.getTodaySummary()`가 호출되고
유가 등급 계산 경로가 붙는다. 첫 테스트는 **경로 조회 → 휴게소 매칭 →
목록 반환**만 검증한다. 유가는 다음 판에서 추가한다.

---

## 3. 무엇을 검증하는가

### 첫 테스트 (정상 케이스) 1개

```
given  DB에 휴게소 2개가 있고
       가짜 카카오가 정상 응답을 준비한 상태에서
when   GET /api/route-rest-stops/list?originLat=..&originLng=..&destinationQuery=부산역
then   200이 오고
       경로 반경 안의 휴게소만 거리순으로 담겨 있다
```

**같이 검증되는 것들** (별도 단언 없이도 이게 깨지면 테스트가 실패한다):

- Spring 컨텍스트가 실제로 뜬다
- MySQL 스키마가 엔티티대로 생성된다 (`ddl-auto=update`)
- Feign 클라이언트가 프로퍼티에서 URL을 읽어 실제 HTTP를 보낸다
- 카카오 응답 JSON이 `KakaoDirectionsResponse` 레코드로 역직렬화된다
- 컨트롤러 → 서비스 → 리포지토리 배선이 맞다
- 응답이 `ApiResponse` 포맷으로 감싸진다

**이게 기존 237개로는 검증되지 않는 이유:** Mockito가 `KakaoMapClient`를
가짜로 바꾸면 그 아래(Feign 설정, 타임아웃, HTTP, JSON 파싱)는 실행 자체가
안 된다. WireMock은 네트워크 건너편만 자르므로 그 구간이 전부 진짜로 돈다.

---

## 4. 구성

### 파일 구조

```
build.gradle                        e2e 스위트 블록 추가 + sonar.tests 한 줄 수정
src/e2e/java/com/restroute/
├── AcceptanceTest.java             베이스 클래스
├── support/
│   ├── DatabaseCleaner.java        테스트 간 데이터 격리
│   ├── KakaoApiStubs.java          WireMock 스텁 정의
│   └── RestStopFixture.java        휴게소 테스트 데이터
└── route/
    └── RouteRestStopListAcceptanceTest.java
src/e2e/resources/
└── application-e2e.properties      WireMock 주소 주입용 기본값
```

`src/test/java`의 237개 파일은 **한 줄도 건드리지 않는다.**

### build.gradle 추가분

```groovy
testing {
    suites {
        e2e(JvmTestSuite) {
            dependencies {
                implementation project()
                implementation 'org.springframework.boot:spring-boot-starter-test'
                implementation 'org.springframework.boot:spring-boot-testcontainers'
                implementation 'org.testcontainers:mysql'
                implementation 'org.testcontainers:junit-jupiter'
                implementation 'io.rest-assured:rest-assured'
                implementation 'org.wiremock:wiremock-standalone:3.13.1'
            }
            targets.configureEach {
                testTask.configure { shouldRunAfter(test) }
            }
        }
    }
}
```

- 버전 없이 쓴 것들은 Spring Boot BOM이 관리한다. WireMock만 BOM 밖이라 명시.
- **`check`에는 붙이지 않는다.** 붙이면 로컬 `./gradlew check`마다 Docker로
  MySQL을 띄우게 돼서 느려진다.
- `sonar.tests`를 `'src/test/java,src/e2e/java'`로 고쳐야 Sonar가 인식한다.

### 베이스 클래스

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("e2e")
@Testcontainers
public abstract class AcceptanceTest {

    @Container
    @ServiceConnection                                   // ← 핵심
    static final MySQLContainer<?> DB = new MySQLContainer<>("mysql:8.0");

    static final WireMockServer KAKAO = new WireMockServer(options().dynamicPort());

    static { KAKAO.start(); }

    @DynamicPropertySource
    static void kakaoUrls(DynamicPropertyRegistry registry) {
        registry.add("kakao.local.url", KAKAO::baseUrl);
        registry.add("kakao.navi.url", KAKAO::baseUrl);
    }

    @LocalServerPort int port;
    @Autowired DatabaseCleaner cleaner;

    @BeforeEach
    void setUpAcceptance() {
        RestAssured.port = port;
        KAKAO.resetAll();
        cleaner.clear();
    }
}
```

**참고했던 템플릿에서 바꾼 4가지와 그 이유:**

| 바꾼 것 | 이유 |
|---|---|
| `PostgreSQLContainer` → `MySQLContainer` | 이 프로젝트 운영 DB가 MySQL 8. Postgres로는 "실제와 같은 환경"이라는 Testcontainers의 존재 이유가 사라진다 |
| `@ServiceConnection` 추가 | 없으면 `spring.datasource.url`을 손으로 주입해야 하고, JUnit이 클래스 종료 시 컨테이너를 내리는데 Spring은 컨텍스트를 캐싱해서 **죽은 컨테이너를 가리키는 커넥션 풀**이 남는다 (Spring Boot 공식 문서가 다루는 알려진 함정) |
| `KAKAO.start()` + `@DynamicPropertySource` | 원본은 객체만 만들고 시작하지 않았고, 동적 포트를 Spring에 알려주는 코드도 없었다. 그대로면 서버가 **진짜 카카오를 호출한다** |
| `@Tag("e2e")` → 별도 소스셋 | 아래 §5 참조 |

### WireMock 스텁

실제 Feign 인터페이스에서 확인한 경로와 파라미터:

**① 카카오 로컬** — `GET /v2/local/search/keyword.json?query={query}`

```json
{ "documents": [
    { "x": "129.0403", "y": "35.1148",
      "place_name": "부산역", "address_name": "부산 동구 초량동" } ] }
```

**② 카카오 모빌리티** — `GET /v1/directions?origin=&destination=&priority=&road_details=&alternatives=`

```json
{ "routes": [ {
    "result_code": 0,
    "summary": { "distance": 1500, "duration": 300 },
    "sections": [ { "roads": [
        { "traffic_state": 1,
          "vertexes": [126.9780,37.5665, 126.9880,37.5565, 126.9980,37.5465] } ] } ] } ] }
```

주의할 계약 사항:
- `result_code`가 **0이 아니면** `failedToRoute()`가 true가 되어
  `RouteRestStopNotFoundException`이 난다. 정상 케이스는 반드시 0.
- `vertexes`는 `[경도, 위도, 경도, 위도, ...]` 평탄화 배열이다.
- `RouteCoordinateReducer`는 정점이 `MINIMUM_POINTS=300` 이하면 축소하지
  않고 그대로 쓴다. 그래서 **정점 3개짜리 짧은 경로로 충분하다.**

### 테스트 데이터 설계

여기가 이 테스트에서 제일 까다로운 부분이다. `RouteRestStopMatcher`가
경로 정점에서 `radiusMeters`(기본 1000m) 안에 있는 휴게소만 남기므로,
**가짜 경로의 좌표와 심는 휴게소의 좌표를 맞춰야 한다.**

```
경로 정점:        (37.5665, 126.9780) ── (37.5565, 126.9880) ── (37.5465, 126.9980)
                                              │
휴게소 A ─────────────────────────────────────┘  정점 위에 배치 → 거리 ~0m, 반드시 매칭
휴게소 B  (37.9000, 127.5000)                    경로에서 수십 km → 반드시 제외
```

A는 포함되고 B는 빠지는 걸 확인하면, **반경 필터가 실제로 돌았다**는 게
증명된다. 휴게소를 1개만 두면 "필터가 아예 안 돌아도" 테스트가 통과하므로
반드시 2개를 둔다.

엔티티 생성은 `RestStopEntity.createByAdmin(unitName, routeNo, routeName,
xValue, yValue)` 정적 팩토리를 쓴다 (public 생성자가 없다).
`xValue`가 경도, `yValue`가 위도이고 **둘 다 String**이다.

**이름은 서로 다르게 짓는다.** `RouteRestStopMatcher`는 같은 이름의 상·하행
페어가 잡히면 진행방향 판정(`sideOfTravel`)으로 한쪽을 제거하는데, 첫
테스트에서 이 로직까지 끌어들이면 실패 원인 분리가 어려워진다. 방향 판정은
별도 테스트로 뺀다.

### 테스트 메서드 네이밍

기존 코드베이스 관례를 따른다 — `<조건>_<기대결과>` + 한글 `@DisplayName`
(215개 파일이 이미 이 형태).

```java
@Test
@DisplayName("경로 반경 안의 휴게소만 거리순으로 반환한다")
void restStopsWithinRadius_areReturnedInDistanceOrder() { ... }
```

---

## 5. 왜 `@Tag`가 아니라 별도 소스셋인가

`@Tag("e2e")`만 붙여도 `./gradlew test`는 **그냥 다 같이 돌린다.** 분리하려면
어차피 build.gradle을 손대야 한다. 그러면 소스셋을 나누는 쪽이 낫다.

결정적인 이유는 커버리지다.

```groovy
jacocoTestCoverageVerification {
    minimum = 0.95          // build.gradle:136
}
```

E2E는 요청 하나로 컨트롤러 → 서비스 → 리포지토리를 전부 훑는다. 같은
소스셋에 두면 **커버리지가 공짜로 올라가고**, 237개 단위 테스트로 지켜온
95%라는 숫자가 무엇을 의미하는지 알 수 없게 된다.

소스셋을 나누면 `jacocoTestCoverageVerification`이 `test` 태스크 결과만
보므로 자동으로 격리된다.

부수 효과로 의존성도 갈린다 — Testcontainers/RestAssured/WireMock이 기존
237개의 클래스패스에 섞이지 않는다.

---

## 6. 이번에 하지 않는 것

- **장애 시나리오** (500 / 지연 / 429 / 무응답) — 뼈대가 초록불이 된 다음
- **CI 연결** (`ci.yml`) — 로컬 `./gradlew e2e` 확인까지만
- **actuator / 메트릭** — 별도 작업
- **`GET /api/route-rest-stops`** (지도용) — 계약이 커서 2차
- **항공권 fan-out** — 이 뼈대가 자리잡은 뒤 같은 패턴으로 확장
- **계약 테스트** (진짜 카카오 호출) — 필요해지면 별도 태스크로 분리

---

## 7. 예상 리스크

| 리스크 | 내용 | 대응 |
|---|---|---|
| `createByAdmin`의 부작용 | `serviceAreaCode`가 `ADMIN-{uuid}`로 자동 생성되고 `adminOverridden=true`가 된다. `RestStopAggregateQueryService.findByRestStopsAndAdminOverridden(selected, null)`가 aggregate를 못 찾아 null을 줄 수 있고, `toItem()`이 이를 견디는지 미확인 | 첫 실행에서 NPE가 나면 fixture를 리포지토리 직접 저장으로 바꾸거나 aggregate를 같이 심는다 |
| 컨텍스트 캐싱 | `@DynamicPropertySource`로 WireMock 포트가 매번 달라지면 Spring 컨텍스트가 재생성될 수 있다 | WireMock을 `static` + `static {}` 블록으로 JVM당 1회만 띄워 포트를 고정 |
| 첫 실행 시간 | MySQL 이미지 최초 pull에 수십 초 | 1회성. 이후 캐시 |
| Checkstyle/PMD 경고 | `checkstyleE2e` / `pmdE2e` 태스크가 자동 생성되어 E2E 코드 스타일에 프로덕션 규칙이 적용됨 | 만들어보고 실제로 시끄러우면 그때 조정. 미리 풀지 않는다 |

---

## 8. 참고 자료

이 설계는 아래 조각들의 조합이다.

**태그·태스크 분리**
- [JUnit 5 User Guide — Tagging and Filtering](https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering)
- [Baeldung — Tagging and Filtering JUnit Tests](https://www.baeldung.com/junit-filtering-tests)

**Gradle 소스셋 분리**
- [Gradle — The JVM Test Suite Plugin](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html) — 현재 권장 방식
- [Gradle Blog — Introducing Test Suites](https://blog.gradle.org/introducing-test-suites) — 예전 수동 sourceSet 방식 대신 이걸 쓰는 이유
- [Tom Gregory — Running integration tests in Gradle](https://tomgregory.com/gradle-integration-tests)

**Testcontainers + Spring Boot**
- [Spring Boot Reference — Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html) — 컨텍스트 캐싱 함정 포함
- [Spring Blog — Improved Testcontainers Support in Spring Boot 3.1](https://spring.io/blog/2023/06/23/improved-testcontainers-support-in-spring-boot-3-1/) — `@ServiceConnection` 도입 배경
- [SivaLabs — Run Spring Boot Testcontainers Tests at Jet Speed](https://www.sivalabs.in/blog/run-spring-boot-testcontainers-tests-at-jet-speed/) — 싱글톤 컨테이너 패턴

**WireMock**
- [WireMock — JUnit 5+ Jupiter](https://wiremock.org/docs/junit-jupiter/)

**프로젝트 내부 규약**
- `jisu-dev:backend-testing` 스킬 — 레이어별 도구 선택, BDD 네이밍
  (`<대상>_<조건>_<기대결과>`), "`@SpringBootTest`는 최소화하되 여러 레이어가
  맞물려야 드러나는 문제엔 정당하다"
- `docs/domain/route.md` §8 — 이 도메인의 진입점과 협력자 구성

---

## 9. 완료 기준

```bash
./gradlew e2e
```

- 초록불
- `./gradlew test` 결과와 소요 시간이 이전과 동일
- `./gradlew check` 결과가 이전과 동일 (E2E가 끼어들지 않음)
- JaCoCo 커버리지 수치가 변하지 않음

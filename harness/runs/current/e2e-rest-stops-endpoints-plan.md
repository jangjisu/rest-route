# 휴게소 공개 API E2E — 구현 계획

> **작업자용:** 태스크 단위로 진행하고 각 태스크 끝에서 `./gradlew e2e`를 돌린다.

**목표:** `/api/rest-stops` 계열 공개 API 13개를 인수 테스트로 덮는다.

**왜 지금 하는가:** 단위 테스트 통합류 28개 파일이 전부 **H2**에서 돈다. 운영은 **MySQL**이다. DB를 부르는 이 13개는 테스트에서 진짜 MySQL을 한 번도 밟은 적이 없고, E2E만 Testcontainers로 MySQL을 띄운다.

**무엇이 실제로 메워지는가**

| | 갭 |
| --- | --- |
| **한글 정렬** | `ORDER BY` 이름이 22곳. H2와 MySQL의 collation이 다르다 |
| **JSON 직렬화 계약** | `@WebMvcTest`가 3개뿐이라 대부분 컨트롤러는 실제 JSON을 만들지 않는다 |
| **HTTP 계층** | 이미지 2종의 ETag·304·204·`image/webp`는 실제 서버가 아니면 안 돈다 |
| **트랜잭션 밖 lazy loading** | 요청 경계 밖 접근은 목 환경에서 안 터진다 |

**메워지지 않는 것(정직하게)**: 스키마 정합성. 운영도 `ddl-auto=update`이고 마이그레이션 도구가 없으며, e2e도 `create-drop`이라 스키마는 양쪽 다 Hibernate가 만든다.

**이미 덮여 있어 다시 하지 않는 것**: admin 인가. `SecurityConfigTest`(`@SpringBootTest`+MockMvc)가 `/api/admin/**` **패턴 단위**로 검증한다.

---

## 대상 13개

| 묶음 | 엔드포인트 | 이 계층에서 볼 것 |
| --- | --- | --- |
| **목록·검색** | `GET /`, `GET /search`, `GET /nearby` | 정렬·필터가 MySQL에서 같은 결과인가 |
| **상세 6종** | `GET /{code}`, `/basic-info`, `/facilities`, `/foods`, `/events`, `/sales-rankings`, `/oil-info` | `Optional`→200/404 계약과 JSON 형태 |
| **이미지 2종** | `GET /{code}/images/detail`, `/images/list` | ETag·304·204·Content-Type |
| **유가 갱신** | `POST /{code}/oil-price/refresh` | 외부 ExApi 실패 시 무엇이 나가는가 |

상세 6종은 전부 같은 모양이다 — 서비스가 `Optional`을 주고 컨트롤러가 200/404로 가른다. "휴게소가 없으면 404"는 6개가 동일하므로 한 번에 묶어 검증한다.

---

## Task 1: 휴게소 픽스처 확장

**Files:**
- Create: `src/e2e/java/com/restroute/support/RestStopFixtures.java`

**Interfaces:**
- Produces: `saveRestStop(...)`, `saveDetail(...)`, `saveFood(...)`, `saveEvent(...)`, `saveOilStation(...)`, `saveImage(...)` 등 — 실제 시그니처는 엔티티 팩토리를 확인해 맞춘다

- [ ] **Step 1: 엔티티가 제공하는 생성 경로를 먼저 확인한다**

```bash
grep -n 'public static .* from(\|public static .* createByAdmin(\|public static .* of(' \
  src/main/java/com/restroute/reststop/domain/*.java \
  src/main/java/com/restroute/reststopcontent/domain/*.java \
  src/main/java/com/restroute/oilprice/domain/*.java
```

리포지토리에 직접 저장하고, 엔티티가 노출한 팩토리만 쓴다 — 리플렉션으로 필드를 밀어넣지 않는다.

- [ ] **Step 2: 시나리오 단위 헬퍼를 만든다**

개별 엔티티 저장보다 **"상세 정보가 다 갖춰진 휴게소 하나"** 같은 시나리오 단위가 쓰기 쉽다. 다만 무엇이 심겼는지가 테스트에서 보여야 하므로, 묶음 헬퍼와 낱개 헬퍼를 함께 둔다.

`RouteFixtures`와 합치지 않는다 — 그쪽은 좌표들 사이의 관계가 의미인 반면 이쪽은 연관 데이터의 유무가 의미라, 한 파일에 두면 둘 다 읽기 어려워진다.

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew compileE2eJava`

---

## Task 2: 13개 응답 관찰

**Files:**
- Create(임시): `src/e2e/java/com/restroute/observation/RestStopEndpointObservationTest.java`
- Create: `harness/runs/current/rest-stop-endpoint-as-is.md`

산출물은 테스트가 아니라 **기록**이다. 끝나면 테스트는 지운다.

- [ ] **Step 1: 두 상태에서 13개를 전부 찍는다**

빈 DB, 그리고 데이터를 심은 상태. 단언하지 않고 status와 body만 출력한다.

```java
    private void observe(String label, Response response) {
        System.out.printf("%n@@OBS %s | status=%d | ct=%s | body=%s%n",
                label, response.statusCode(), response.contentType(), abbreviated(response.asString()));
    }
```

이미지 2종은 본문이 바이너리이므로 길이와 헤더(`ETag`, `Cache-Control`)만 찍는다.

- [ ] **Step 2: 실행하고 출력을 읽는다**

Gradle 콘솔에는 stdout이 안 나오므로 결과 XML에서 읽는다.

```bash
./gradlew e2e --tests '*RestStopEndpointObservationTest*'
python3 - <<'PY'
import glob, html
for p in glob.glob('build/test-results/e2e/*Observation*.xml'):
    for line in html.unescape(open(p, encoding='utf-8').read()).splitlines():
        if '@@OBS' in line:
            print(line.split('@@OBS ',1)[1])
PY
```

- [ ] **Step 3: 관찰을 문서로 고정하고 임시 테스트를 지운다**

**여기 적힌 값만 이후 태스크의 단언에 쓴다.**

---

## Task 3: 목록·검색 인수 테스트

**Files:**
- Create: `src/e2e/java/com/restroute/reststop/RestStopListAcceptanceTest.java`

세 엔드포인트가 같은 목록 성격이고 픽스처를 공유하므로 한 파일에 둔다. (엔드포인트당 한 파일 원칙의 예외이며, 이유를 클래스 주석에 남긴다.)

- [ ] **Step 1: 전체 목록 — 심은 것이 그대로 나오는가**

- [ ] **Step 2: 이름 검색 — 부분 일치·대소문자 무시**

`searchByName`은 `findByUnitNameContainingIgnoreCase`를 탄다. **한글에서 MySQL의 `LIKE`와 collation이 H2와 갈리는 지점**이므로, 대소문자 섞인 영문과 한글 부분 일치를 함께 넣는다.

- [ ] **Step 3: 공백뿐인 검색어는 리포지토리를 타지 않고 빈 목록**

단위 테스트가 이미 있으나(`verifyNoInteractions`), 여기서는 **HTTP 응답이 빈 배열인지**를 본다.

- [ ] **Step 4: nearby — 좌표를 주면 거리순, 안 주면 거리 없이**

거리순 정렬이 실제로 도는 것을 보이려면 **멀고 가까운 휴게소를 둘 이상** 심어야 한다.

- [ ] **Step 5: nearby — 이름·유종 필터가 겹쳐도 동작**

- [ ] **Step 6~8: 검증 → 판별력 확인(일부러 깨뜨리기) → 커밋**

---

## Task 4: 상세 6종 인수 테스트

**Files:**
- Create: `src/e2e/java/com/restroute/reststop/RestStopDetailAcceptanceTest.java`

- [ ] **Step 1: 없는 코드는 6개 모두 404다**

여섯이 같은 계약이므로 `@ParameterizedTest`로 경로만 바꿔 한 번에 검증한다.

```java
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "/api/rest-stops/UNKNOWN",
        "/api/rest-stops/UNKNOWN/basic-info",
        "/api/rest-stops/UNKNOWN/facilities",
        "/api/rest-stops/UNKNOWN/foods",
        "/api/rest-stops/UNKNOWN/events",
        "/api/rest-stops/UNKNOWN/sales-rankings",
        "/api/rest-stops/UNKNOWN/oil-info"
    })
    @DisplayName("휴게소가 없으면 404와 NOT_FOUND가 나간다")
    void unknownServiceAreaCode_responds404(String path) { … }
```

- [ ] **Step 2~7: 각 상세의 성공 응답을 하나씩 고정한다**

관찰 기록에 근거해 필드가 실제로 실리는지 본다. **연관 데이터가 없을 때 404인지 빈 값 200인지**가 엔드포인트마다 갈릴 수 있으므로 관찰 결과대로 적는다.

- [ ] **Step 8~10: 검증 → 판별력 확인 → 커밋**

---

## Task 5: 이미지 2종 인수 테스트

**Files:**
- Create: `src/e2e/java/com/restroute/reststop/RestStopImageAcceptanceTest.java`

이 파일이 이번 작업에서 **단위 테스트로는 대체 불가능한 부분이 가장 큰** 곳이다. ETag 계산·조건부 GET·304는 실제 HTTP 왕복이 있어야 돈다.

- [ ] **Step 1: 이미지가 있으면 200 + `image/webp` + ETag**

- [ ] **Step 2: 같은 ETag를 `If-None-Match`로 다시 보내면 304, 본문 없음**

```java
        String eTag = get(PATH).then().statusCode(200).extract().header("ETag");

        given().header("If-None-Match", eTag)
                .when()
                .get(PATH)
                .then()
                .statusCode(304)
                .header("ETag", equalTo(eTag));
```

- [ ] **Step 3: `If-None-Match: *`도 304다**

컨트롤러가 `"*"`를 따로 분기하고 있어 별도 갈래다.

- [ ] **Step 4: 이미지가 없으면 204 No Content**

- [ ] **Step 5: 두 크기(detail·list)가 서로 다른 바이트를 준다**

같은 이미지를 돌려주면 목록이 상세용 큰 이미지를 받게 되므로, **서로 다름**이 계약이다.

- [ ] **Step 6~8: 검증 → 판별력 확인 → 커밋**

---

## Task 6: 유가 갱신 인수 테스트

**Files:**
- Modify: `src/e2e/java/com/restroute/AcceptanceTest.java`
- Create: `src/e2e/java/com/restroute/support/ExApiStubs.java`
- Create: `src/e2e/java/com/restroute/reststop/RestStopOilPriceRefreshAcceptanceTest.java`

13개 중 유일하게 **요청 시점에 외부 API를 부르는** 엔드포인트다.

- [ ] **Step 1: `AcceptanceTest`에 ExApi용 WireMock을 추가한다**

카카오 서버를 재사용하지 않고 **별도 서버**를 세운다 — 카카오 스텁이 서 있는 서버에 다른 도메인 경로를 얹으면 어느 외부가 죽었는지 시나리오가 섞인다.

```java
    /** 고속도로 공공 API(ExApi). 카카오와 섞이지 않도록 서버를 나눈다. */
    protected static final WireMockServer EX_API = new WireMockServer(options().dynamicPort());
```

`static` 블록에서 함께 띄우고, `@DynamicPropertySource`에 `ex.api.url`을 등록하고, `@BeforeEach`에서 `resetAll()` 한다.

이때 `application-e2e.properties`의 `ex.api.url=http://localhost:1`(이중 안전장치) 주석이 더 이상 정확하지 않으므로 함께 고친다.

- [ ] **Step 2: `ExApiStubs`를 만든다**

먼저 실제 Feign 인터페이스에서 경로와 응답 형태를 확인한다.

```bash
grep -n 'PATH\|@GetMapping\|@RequestParam' src/main/java/com/restroute/common/client/ExApiFeignClient.java
```

- [ ] **Step 3: 성공 — 갱신된 유가가 응답에 실린다**

- [ ] **Step 4: 외부 실패 — 관찰 기록대로 고정한다**

`refreshByServiceAreaCode`가 `Optional`을 주므로 404일 수도, `EXTERNAL_API_UNAVAILABLE`일 수도 있다. **추측하지 말고 Task 2 관찰 결과를 쓴다.**

- [ ] **Step 5: 휴게소가 없으면 외부를 부르지 않고 404**

- [ ] **Step 6~8: 검증 → 판별력 확인 → 커밋**

---

## Task 7: 문서 갱신

**Files:**
- Modify: `API.md`, `docs/domain/rest-stop*.md`

- [ ] **Step 1: 관찰로 확인한 것과 문서가 어긋난 곳을 고친다**

특히 이미지 2종의 304·204 계약과, 상세 6종의 "연관 데이터가 없을 때" 동작.

- [ ] **Step 2: 200줄 규칙 확인**

```bash
wc -l docs/domain/*.md | sort -rn | head
```

넘으면 같은 PR에서 group으로 분할한다.

- [ ] **Step 3: 커밋**

---

## 완료 기준

- `./gradlew e2e` 전부 통과, **기존 37개는 한 줄도 수정되지 않았다**
- 13개 엔드포인트가 전부 최소 한 번씩 실제 MySQL을 거쳐 검증된다
- 실패·경계 케이스의 기대값이 전부 관찰 기록에 근거한다 — 추측한 값이 없다
- 각 묶음의 대표 성공 케이스가 일부러 깨뜨렸을 때 실제로 실패한다
- `./gradlew test`와 커버리지 게이트가 영향받지 않는다

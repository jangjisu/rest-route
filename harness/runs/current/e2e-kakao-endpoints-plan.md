# 카카오 경유 엔드포인트 E2E — 구현 계획

> **작업자용:** 태스크 단위로 진행하고 각 태스크 끝에서 `./gradlew e2e`를 돌린다. 단계는 체크박스로 추적한다.

**목표:** 카카오를 요청 시점에 호출하는 나머지 두 엔드포인트를 인수 테스트로 덮는다.

**접근:** 기존 `AcceptanceTest` 기반을 그대로 쓴다 — 외부 *서버*만 WireMock으로 바꾸므로 Feign 설정·타임아웃·HTTP·JSON 역직렬화는 실제로 돈다. 새로 만드는 것은 **장소 검색 스텁**뿐이다.

**실패 케이스의 기대값은 설계 의도가 아니라 관찰된 현재 동작이다.** 추측해서 적지 않는다 — Task 2에서 먼저 관찰하고, 관찰 결과로 Task 3·4의 단언을 채운다.

---

## 대상

| 엔드포인트 | 외부 호출 | 이미 덮인 것 |
| --- | --- | --- |
| `GET /api/place-search` | 카카오 장소 검색 1회 | 없음 |
| `GET /api/route-rest-stops` | 장소 검색 0~1회 + 길찾기 1회 | 없음 |
| ~~`GET /api/route-rest-stops/list`~~ | 길찾기 1회 | **13개(PR #136)** |

`/list`와 `/api/route-rest-stops`는 목적지 해석 방식이 다르다 — `/list`는 서버가 아는 이름(`PopularDestination`)만 받고, 지도 화면은 **검색어를 받아 카카오로 지오코딩**한다. 그 차이가 이번에 새로 덮는 지점이다.

## 이번 범위 밖

| 무엇을 | 왜 |
| --- | --- |
| 항공편 검색(Travelpayouts 병렬 팬아웃) | 스텁을 새로 만들어야 해 기반 작업이 별도로 필요하다. 다음 PR |
| 국가 유가·유가 갱신(Opinet·ExApi) | 위와 같다 |
| DB 조회 전용 엔드포인트, admin 38개 | 외부 API가 없어 이 계층의 값이 가장 낮다. 뒤로 미룬다 |
| 실패 동작을 **고치는** 일 | 이번 작업은 현재 동작을 고정할 뿐이다. 회복탄력성 작업이 동작을 바꾸면 이 테스트들이 빨간불로 알려준다 |

---

## Task 1: 장소 검색 스텁 추가

**Files:**
- Modify: `src/e2e/java/com/restroute/support/KakaoApiStubs.java`

**Interfaces:**
- Produces: `stubKeywordSearch(WireMockServer, String documentsJson)`, `stubKeywordSearchDocuments(WireMockServer, String... documents)`, `keywordDocument(String placeName, String addressName, String x, String y)`, `stubKeywordSearchStatus`, `stubKeywordSearchConnectionReset`, `stubKeywordSearchNonJson`, `verifyDirectionsNotCalled`, `verifyKeywordSearchCalled`

- [ ] **Step 1: 문서 하나를 만드는 헬퍼를 추가한다**

카카오 계약의 필드명(`place_name`, `address_name`)을 그대로 쓴다 — 우리 DTO의 `@JsonProperty` 매핑이 실제로 도는지가 이 계층에서 확인할 값 중 하나다.

```java
    /** 장소 검색 결과 문서 하나. 필드명은 카카오 계약 그대로여야 역직렬화가 실제로 검증된다. */
    public static String keywordDocument(String placeName, String addressName, String longitude, String latitude) {
        return """
                { "place_name": %s, "address_name": %s, "x": %s, "y": %s }
                """
                .formatted(quoted(placeName), quoted(addressName), quoted(longitude), quoted(latitude));
    }

    private static String quoted(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value + "\"";
    }
```

- [ ] **Step 2: 문서 목록으로 성공 응답을 세우는 스텁을 추가한다**

```java
    /** 장소 검색 — 주어진 문서들을 담은 200 응답. 문서를 하나도 주지 않으면 빈 결과가 된다. */
    public static void stubKeywordSearchDocuments(WireMockServer kakao, String... documents) {
        kakao.stubFor(get(urlPathEqualTo(KEYWORD_SEARCH_PATH))
                .willReturn(okJson("{ \"documents\": [%s] }".formatted(String.join(",", documents)))));
    }

    /** 장소 검색 — documents 키 자체가 없다. 빈 배열과 갈래가 다르므로 따로 둔다. */
    public static void stubKeywordSearchWithoutDocuments(WireMockServer kakao) {
        kakao.stubFor(get(urlPathEqualTo(KEYWORD_SEARCH_PATH)).willReturn(okJson("{}")));
    }
```

- [ ] **Step 3: 실패 갈래 스텁을 길찾기와 같은 모양으로 추가한다**

```java
    /** 장소 검색 — 서버가 상태코드로 실패를 알린다. */
    public static void stubKeywordSearchStatus(WireMockServer kakao, int status) {
        kakao.stubFor(get(urlPathEqualTo(KEYWORD_SEARCH_PATH))
                .willReturn(aResponse().withStatus(status).withBody("{\"errorType\":\"stub\"}")));
    }

    /** 장소 검색 — 상태코드는 실패인데 본문이 JSON이 아니다. */
    public static void stubKeywordSearchNonJson(WireMockServer kakao, int status) {
        kakao.stubFor(get(urlPathEqualTo(KEYWORD_SEARCH_PATH))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html><body>Service Unavailable</body></html>")));
    }

    /** 장소 검색 — 연결이 끊긴다. 상태코드조차 받지 못하는 갈래다. */
    public static void stubKeywordSearchConnectionReset(WireMockServer kakao) {
        kakao.stubFor(get(urlPathEqualTo(KEYWORD_SEARCH_PATH))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }
```

- [ ] **Step 4: 호출 여부 확인 헬퍼를 짝으로 맞춘다**

기존에 `verifyKeywordSearchNotCalled`만 있다. 지도 화면은 "검색어를 주면 장소 검색을 **부른다**"가 검증 대상이므로 반대쪽도 필요하다.

```java
    /** 장소 검색이 정확히 한 번 호출됐음을 확인한다. */
    public static void verifyKeywordSearchCalled(WireMockServer kakao, String query) {
        kakao.verify(1, getRequestedFor(urlPathEqualTo(KEYWORD_SEARCH_PATH)).withQueryParam("query", equalTo(query)));
    }

    /** 길찾기가 호출되지 않았음을 확인한다 — 목적지 해석에서 끝났다는 뜻. */
    public static void verifyDirectionsNotCalled(WireMockServer kakao) {
        kakao.verify(0, getRequestedFor(urlPathEqualTo(DIRECTIONS_PATH)));
    }
```

클래스 주석의 *"길찾기 스텁만 둔다"* 문장은 더 이상 참이 아니므로 함께 고친다.

- [ ] **Step 5: 컴파일 확인**

Run: `./gradlew compileE2eJava`
Expected: BUILD SUCCESSFUL

---

## Task 2: 실패 동작 관찰

**Files:**
- Create(임시): `src/e2e/java/com/restroute/observation/KakaoFailureObservationTest.java`
- Create: `harness/runs/current/kakao-endpoint-failure-as-is.md`

이 태스크의 산출물은 **테스트가 아니라 기록**이다. 관찰이 끝나면 테스트 파일은 지운다.

- [ ] **Step 1: 두 엔드포인트의 실패 갈래에서 실제 응답을 찍는 테스트를 쓴다**

기대값을 단언하지 않고 상태코드와 본문을 출력만 한다.

```java
    private void observe(String label, Response response) {
        System.out.printf("%n=== %s ===%nstatus=%d%nbody=%s%n", label, response.statusCode(), response.asString());
    }

    @Test
    void observePlaceSearchFailures() {
        stubKeywordSearchStatus(KAKAO, 500);
        observe("place-search / 장소검색 500", given().param("query", "부산역").when().get(PLACE_SEARCH));

        KAKAO.resetAll();
        stubKeywordSearchConnectionReset(KAKAO);
        observe("place-search / 연결 끊김", given().param("query", "부산역").when().get(PLACE_SEARCH));

        KAKAO.resetAll();
        stubKeywordSearchNonJson(KAKAO, 503);
        observe("place-search / 비-JSON 본문", given().param("query", "부산역").when().get(PLACE_SEARCH));

        KAKAO.resetAll();
        observe("place-search / query 누락", given().when().get(PLACE_SEARCH));
    }
```

지도 화면도 같은 방식으로 찍는다 — 장소 검색 실패, 장소 검색 결과 없음, 길찾기 실패, 국가 유가 조회 실패(무스텁).

- [ ] **Step 2: 관찰을 실행하고 출력을 남긴다**

Run: `./gradlew e2e --tests '*KakaoFailureObservationTest*' --info`
Expected: 통과(단언이 없으므로). 출력에서 각 갈래의 status와 body를 읽는다.

- [ ] **Step 3: 관찰 결과를 문서로 고정한다**

`kakao-endpoint-failure-as-is.md`에 갈래별 status·code·message를 표로 적는다. **여기 적힌 값만 Task 3·4의 단언에 쓴다.**

- [ ] **Step 4: 관찰용 테스트를 지운다**

기록이 남았으므로 파일은 남기지 않는다.

```bash
rm src/e2e/java/com/restroute/observation/KakaoFailureObservationTest.java
```

---

## Task 3: 장소 검색 인수 테스트

**Files:**
- Create: `src/e2e/java/com/restroute/placesearch/PlaceSearchAcceptanceTest.java`

한 엔드포인트당 한 파일 원칙을 따른다. 경로와 요청 조립은 **이 파일의 private 멤버**로 둔다 — 무엇을 테스트하는지가 파일의 정체성이라 공용 자리로 빼지 않는다.

- [ ] **Step 1: 파일 뼈대와 요청 조립을 쓴다**

```java
class PlaceSearchAcceptanceTest extends AcceptanceTest {

    private static final String PATH = "/api/place-search";

    private Response search(String query) {
        return given().param("query", query).when().get(PATH);
    }
}
```

- [ ] **Step 2: 성공 케이스를 쓴다 — 카카오 필드가 우리 계약으로 옮겨지는지**

`place_name`/`address_name`/`x`/`y`가 각각 `name`/`address`/`longitude`/`latitude`로 가는지 본다. **x가 경도, y가 위도**라는 축 바뀜이 이 계층에서 확인할 핵심이다.

```java
    @Test
    @DisplayName("카카오 검색 결과를 후보 목록으로 옮겨준다")
    void keywordSearchResults_becomeCandidates() {
        stubKeywordSearchDocuments(
                KAKAO,
                keywordDocument("부산역", "부산 동구 초량동", "129.0403", "35.1148"),
                keywordDocument("부산역환승센터", "부산 동구", "129.0410", "35.1150"));

        search("부산역")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data", hasSize(2))
                .body("data[0].name", equalTo("부산역"))
                .body("data[0].address", equalTo("부산 동구 초량동"))
                .body("data[0].latitude", equalTo(35.1148f))
                .body("data[0].longitude", equalTo(129.0403f));
    }
```

- [ ] **Step 3: 장소명이 비면 주소명으로 대체되는 규칙을 쓴다**

```java
    @Test
    @DisplayName("장소명이 비어 있으면 주소명을 이름으로 쓴다")
    void blankPlaceName_fallsBackToAddressName() {
        stubKeywordSearchDocuments(KAKAO, keywordDocument("", "부산 동구 초량동", "129.0403", "35.1148"));

        search("부산역").then().statusCode(SUCCESS_STATUS).body("data[0].name", equalTo("부산 동구 초량동"));
    }
```

- [ ] **Step 4: 좌표를 읽을 수 없는 후보만 빠지는 것을 쓴다**

좋은 후보를 함께 두는 것이 중요하다 — 하나만 두면 전부 버려도 통과해 아무것도 증명하지 못한다.

```java
    @Test
    @DisplayName("좌표를 숫자로 읽을 수 없는 후보만 빠지고 나머지는 남는다")
    void unparsableCoordinates_dropOnlyThatCandidate() {
        stubKeywordSearchDocuments(
                KAKAO,
                keywordDocument("좌표없음", "주소", null, "35.1148"),
                keywordDocument("좌표깨짐", "주소", "abc", "35.1148"),
                keywordDocument("부산역", "부산 동구 초량동", "129.0403", "35.1148"));

        search("부산역")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data", hasSize(1))
                .body("data[0].name", equalTo("부산역"));
    }
```

- [ ] **Step 5: 빈 결과 두 갈래를 쓴다**

```java
    @Test
    @DisplayName("검색 결과가 없으면 실패가 아니라 빈 목록이 나간다")
    void noSearchResult_respondsEmptyList() {
        stubKeywordSearchDocuments(KAKAO);

        search("없는곳").then().statusCode(SUCCESS_STATUS).body("code", equalTo("SUCCESS")).body("data", hasSize(0));
    }

    @Test
    @DisplayName("documents 키가 아예 없어도 빈 목록으로 견딘다")
    void missingDocumentsKey_respondsEmptyList() {
        stubKeywordSearchWithoutDocuments(KAKAO);

        search("없는곳").then().statusCode(SUCCESS_STATUS).body("data", hasSize(0));
    }
```

- [ ] **Step 6: 실패 갈래를 쓴다 — 단언값은 Task 2의 관찰 기록에서 가져온다**

500 / 429 / 비-JSON / 연결 끊김 / `query` 누락. **관찰하지 않은 값을 쓰지 않는다.**

- [ ] **Step 7: 검증**

Run: `./gradlew e2e --tests '*PlaceSearchAcceptanceTest*'`
Expected: 전부 통과

- [ ] **Step 8: 테스트가 실제로 판별하는지 확인한다**

일부러 깨뜨려 본다 — `keywordDocument`의 x와 y를 맞바꾸면 Step 2가 실패해야 한다. 실패를 확인한 뒤 되돌린다.

- [ ] **Step 9: 커밋**

```bash
git add src/e2e
git commit -m "test(place-search): 장소 검색 엔드포인트 인수 테스트 추가"
```

---

## Task 4: 지도 화면 경로 휴게소 인수 테스트

**Files:**
- Create: `src/e2e/java/com/restroute/route/RouteRestStopAcceptanceTest.java`

`/list`와 같은 패키지에 나란히 둔다. 겹치는 기대값은 `RouteFixtures`가 이미 갖고 있으므로 그대로 쓴다.

- [ ] **Step 1: 파일 뼈대와 요청 조립을 쓴다**

`/list`와 달리 **검색어로 목적지를 정하는 방식**이 있으므로 조립도 그만큼 갈린다.

```java
class RouteRestStopAcceptanceTest extends AcceptanceTest {

    private static final String PATH = "/api/route-rest-stops";

    private Response get(Map<String, Object> params) {
        return given().params(params).when().get(PATH);
    }

    /** 목적지를 검색어로 지정한다 — 서버가 카카오 장소 검색으로 좌표를 얻는다. */
    private Map<String, Object> destinationQueryParams(String query) {
        return Map.of("originLat", ORIGIN_LATITUDE, "originLng", ORIGIN_LONGITUDE, "destinationQuery", query);
    }

    /** 목적지를 좌표로 지정한다 — 장소 검색이 필요 없다. */
    private Map<String, Object> destinationCoordinateParams() {
        return Map.of(
                "originLat", ORIGIN_LATITUDE,
                "originLng", ORIGIN_LONGITUDE,
                "destinationLat", DESTINATION_LATITUDE,
                "destinationLng", DESTINATION_LONGITUDE,
                "destinationName", KNOWN_DESTINATION_NAME);
    }
}
```

- [ ] **Step 2: 검색어 경로 — 장소 검색을 실제로 부르고 그 좌표로 길찾기까지 간다**

`/list`가 `verifyKeywordSearchNotCalled`로 고정한 것의 정반대다. 두 엔드포인트의 성격 차이가 여기서 드러난다.

```java
    @Test
    @DisplayName("목적지 검색어를 주면 장소 검색으로 좌표를 얻어 경로 위 휴게소를 돌려준다")
    void destinationQuery_geocodesThenFindsRestStops() {
        saveOnRouteRestStop(restStopRepository);
        saveOffRouteRestStop(restStopRepository);

        stubKeywordSearchDocuments(
                KAKAO, keywordDocument("부산역", "부산 동구 초량동", "129.0403", "35.1148"));
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        get(destinationQueryParams("부산"))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data.destination.name", equalTo("부산역"))
                .body("data.routes", hasSize(1))
                .body("data.routes[0].restStops", hasSize(1))
                .body("data.routes[0].restStops[0].unitName", equalTo(ON_ROUTE_NAME));

        verifyKeywordSearchCalled(KAKAO, "부산");
    }
```

- [ ] **Step 3: 좌표 경로 — 장소 검색을 부르지 않는다**

```java
    @Test
    @DisplayName("목적지 좌표를 직접 주면 장소 검색 없이 경로를 찾는다")
    void destinationCoordinates_skipPlaceSearch() {
        saveOnRouteRestStop(restStopRepository);
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        get(destinationCoordinateParams())
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.destination.name", equalTo(KNOWN_DESTINATION_NAME))
                .body("data.routes[0].restStops[0].unitName", equalTo(ON_ROUTE_NAME));

        verifyKeywordSearchNotCalled(KAKAO);
    }
```

- [ ] **Step 4: 경로 요약이 실려 나가는 것을 쓴다**

`/list`에는 없는 필드다 — 지도 화면만 거리·시간·경로선을 쓴다.

```java
    @Test
    @DisplayName("경로 요약에 거리와 경로선이 함께 실린다")
    void routeSummary_carriesDistanceAndPath() {
        stubKeywordSearchDocuments(KAKAO, keywordDocument("부산역", "부산 동구", "129.0403", "35.1148"));
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        get(destinationQueryParams("부산"))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.routes[0].summary.distanceMeters", equalTo((int) ROUTE_DISTANCE_METERS))
                .body("data.routes[0].summary.path", hasSize(ROUTE_VERTEXES.length / 2));
    }
```

- [ ] **Step 5: 목적지 해석 실패 — 길찾기까지 가지 않는다**

```java
    @Test
    @DisplayName("장소 검색 결과가 없으면 길찾기를 부르지 않고 404가 나간다")
    void noPlaceSearchResult_responds404WithoutDirections() {
        stubKeywordSearchDocuments(KAKAO);

        get(destinationQueryParams("없는곳")).then().statusCode(NOT_FOUND_STATUS);

        verifyDirectionsNotCalled(KAKAO);
    }
```

구체적 message는 Task 2의 관찰 기록에서 가져온다.

- [ ] **Step 6: 국가 유가 조회가 실패해도 응답이 나가는 것을 쓴다**

e2e 프로파일에서 Opinet 주소가 닫혀 있어(`localhost:1`) 이 호출은 항상 실패한다. 그런데도 200이 나가는 것이 **부분 실패를 견디는 설계**의 증거다.

```java
    @Test
    @DisplayName("국가 유가 조회가 실패해도 경로 휴게소 응답은 그대로 나간다")
    void nationalOilPriceUnavailable_doesNotBreakResponse() {
        saveOnRouteRestStop(restStopRepository);
        stubKeywordSearchDocuments(KAKAO, keywordDocument("부산역", "부산 동구", "129.0403", "35.1148"));
        stubDirections(KAKAO, ROUTE_DISTANCE_METERS, ROUTE_VERTEXES);

        get(destinationQueryParams("부산"))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data.routes[0].restStops", hasSize(1));
    }
```

- [ ] **Step 7: 실패 갈래를 쓴다 — 단언값은 관찰 기록에서 가져온다**

장소 검색 실패(500·연결 끊김)와 길찾기 실패(500·result_code)를 나눠서 쓴다. **두 외부 호출이 갈리는지 아니면 같은 응답으로 뭉개지는지**가 기록할 값이다.

- [ ] **Step 8: 검증**

Run: `./gradlew e2e`
Expected: 기존 13개 + 새 테스트 전부 통과

- [ ] **Step 9: 테스트가 실제로 판별하는지 확인한다**

경로 밖 휴게소를 경로 위로 옮기면 Step 2가 실패해야 한다. 확인 후 되돌린다.

- [ ] **Step 10: 커밋**

```bash
git add src/e2e
git commit -m "test(route): 지도 화면 경로 휴게소 엔드포인트 인수 테스트 추가"
```

---

## Task 5: 문서 갱신

**Files:**
- Modify: `docs/domain/` 중 해당 엔드포인트를 다루는 문서
- Modify: `API.md`

- [ ] **Step 1: `API.md`에 두 엔드포인트 계약이 최신인지 확인하고 어긋난 곳을 고친다**

인수 테스트를 쓰면서 확인한 실제 응답과 문서가 다르면 문서를 고친다 — 이번에 관찰로 확인한 것이 사실이다.

- [ ] **Step 2: 200줄 규칙을 확인한다**

```bash
wc -l docs/domain/*.md | sort -rn | head
```

건드린 문서가 200줄을 넘으면 group으로 분할한다(같은 PR에서 처리한다).

- [ ] **Step 3: 커밋**

---

## 완료 기준

- `./gradlew e2e`가 전부 통과하고, **기존 13개는 한 줄도 수정되지 않았다**
- 두 엔드포인트 각각 파일 하나가 담당하고, 경로와 요청 조립이 그 파일 안에 있다
- 실패 케이스의 기대값이 전부 관찰 기록에 근거한다 — 추측한 값이 없다
- 각 성공 케이스가 일부러 깨뜨렸을 때 실제로 실패한다(Task 3 Step 8, Task 4 Step 9)
- `./gradlew test`와 커버리지 게이트가 영향받지 않는다(E2E는 별도 소스셋)

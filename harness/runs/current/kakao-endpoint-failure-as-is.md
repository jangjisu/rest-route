# 카카오 경유 엔드포인트 — 현재 동작 관찰 기록

**측정일:** 2026-09-10 / 브랜치 `pr/21-e2e-kakao-endpoints` / 커밋 `21a6847` 기준

**방법:** `AcceptanceTest` 위에서 실제 서버를 띄우고 카카오 자리에 WireMock을 세운 뒤, 단언 없이 status와 body만 출력했다. 아래 값은 **설계 의도가 아니라 관찰된 사실**이다. 인수 테스트의 기대값은 전부 이 표에서 가져온다.

---

## `GET /api/place-search`

| 갈래 | status | code | 비고 |
| --- | --- | --- | --- |
| 정상 | 200 | `SUCCESS` | 후보 배열 |
| `documents` 빈 배열 | 200 | `SUCCESS` | `data: []` |
| `documents` 키 없음 | 200 | `SUCCESS` | `data: []` — 빈 배열과 같은 결과 |
| 장소검색 500 | 200 | `EXTERNAL_API_UNAVAILABLE` | `data: null` |
| 장소검색 429 | 200 | `EXTERNAL_API_UNAVAILABLE` | 500과 **구분되지 않는다** |
| 비-JSON 본문(503) | 200 | `EXTERNAL_API_UNAVAILABLE` | 파싱 실패로 새지 않는다 |
| 연결 끊김 | 200 | `EXTERNAL_API_UNAVAILABLE` | 상태코드 실패와 같은 응답 |
| `query` 누락 | **400** | `INVALID_PARAMETER` | 스프링 바인딩 단계에서 걸린다 |
| `query` 빈 문자열 | 200 | `SUCCESS` | **검증 없이 카카오를 부른다**(아래 참고) |

**응답 매핑** — 카카오 필드가 우리 계약으로 옮겨진다.

```
place_name → name      address_name → address      x → longitude      y → latitude
```

문서 3개(정상 / `place_name` 공백 / `x`가 `"abc"`)를 한 번에 넣었을 때 후보 **2개**가 나왔다.

- `place_name`이 비면 `address_name`이 `name`이 된다
- 좌표를 숫자로 읽을 수 없는 후보 **하나만** 빠지고 나머지는 남는다

## `GET /api/route-rest-stops`

| 갈래 | status | code | message |
| --- | --- | --- | --- |
| 정상 | 200 | `SUCCESS` | — |
| 장소검색 결과 없음 | **404** | `NOT_FOUND` | `목적지 검색 결과가 없습니다: {검색어}` |
| 길찾기 `result_code=104` | **404** | `NOT_FOUND` | `출발지와 도착지가 너무 가까워요. …` |
| 장소검색 500 | 200 | `EXTERNAL_API_UNAVAILABLE` | — |
| 장소검색 연결 끊김 | 200 | `EXTERNAL_API_UNAVAILABLE` | — |
| 길찾기 500 | 200 | `EXTERNAL_API_UNAVAILABLE` | — |
| 길찾기 연결 끊김 | 200 | `EXTERNAL_API_UNAVAILABLE` | — |
| `originLat` 누락 | **400** | `INVALID_PARAMETER` | — |
| 목적지 지정 자체가 없음 | 200 | `SUCCESS` | **검증 없이 카카오를 부른다**(아래 참고) |

**두 외부 호출이 갈리지 않는다.** 장소 검색이 죽었는지 길찾기가 죽었는지가 응답에서 구분되지 않고 똑같이 `EXTERNAL_API_UNAVAILABLE`로 나간다.

**성공 응답 형태**

```json
{"code":"SUCCESS","data":{
  "destination":{"name":"부산역","latitude":35.1148,"longitude":129.0403},
  "routes":[{"routeIndex":0,
    "summary":{"distanceMeters":1500,"durationSeconds":600,"tollFareWon":0,
               "path":[[126.978,37.5665],[126.988,37.5565],[126.998,37.5465]]},
    "restStops":[{"unitName":"경로위휴게소","distanceFromRouteMeters":0,
                  "nearbyTraffic":{"key":"jam","label":"정체"}, …}]}]}}
```

- `path`는 `[경도, 위도]` 쌍의 배열이다 — 카카오의 평탄화된 `vertexes`가 쌍으로 묶여 나간다
- `summary`(거리·시간·통행료·경로선)는 `/list`에 없는 필드다. 지도 화면만 쓴다

---

## 짚어둘 것 — 검증 없이 외부로 나가는 두 입력

둘 다 **정상 스텁을 세우고 다시 관찰해** 확인했다(스텁 없이 찍으면 404 때문에 `EXTERNAL_API_UNAVAILABLE`이 나와 결과가 오염된다).

**① `place-search`에 빈 검색어**

`query=""`는 400으로 걸리지 않는다. 스프링은 파라미터가 **있으므로** 통과시키고, 서비스도 검사하지 않아 그대로 카카오로 나간다. 관찰에서 장소 검색이 실제로 호출됐고 `SUCCESS`가 나왔다.

**② `route-rest-stops`에 목적지를 하나도 안 줌**

`destinationQuery`·`destinationLat`·`destinationLng`·`destinationName`이 전부 없어도 400이 아니다. 빈 검색어로 지오코딩을 시도하고, 카카오가 뭔가 돌려주면 그 좌표를 목적지로 삼아 **성공 응답까지 간다**.

두 경우 모두 사용자 입력 검증이 외부 API 호출 뒤로 밀려 있다 — 쿼터를 쓰고 지연을 먹은 뒤에야 결과가 정해진다. **이번 작업에서 고치지 않는다.** 인수 테스트로 현재 동작을 고정해두고, 바꿀 때 그 테스트가 빨간불로 알려주게 한다.

## 이 기록의 쓰임

회복탄력성 작업(재시도·서킷브레이커·타임아웃 조정)으로 위 동작을 바꾸면 인수 테스트가 실패하면서 **무엇이 달라졌는지** 드러난다. 표의 값이 바뀌는 것 자체는 문제가 아니고, 모르는 채로 바뀌는 것이 문제다.

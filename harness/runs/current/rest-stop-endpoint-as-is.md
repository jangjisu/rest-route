# 휴게소 공개 API 13개 — 현재 동작 관찰 기록

**측정일:** 2026-09-10 / 브랜치 `pr/21-e2e-endpoint-coverage` / main `21a6847` 기준

**방법:** `AcceptanceTest` 위에서 실제 서버를 띄우고 **MySQL 컨테이너**를 붙인 뒤, 단언 없이 status와 body만 출력했다. 아래 값은 **설계 의도가 아니라 관찰된 사실**이다. 인수 테스트의 기대값은 전부 이 문서에서 가져온다.

---

## 1. 목록·검색 3종

| 갈래 | status | 결과 |
| --- | --- | --- |
| `GET /` 빈 DB | 200 | `data: []` |
| `GET /` 데이터 있음 | 200 | **삽입 순서 그대로** — `findAll()`에 `ORDER BY`가 없다 |
| `GET /search?name=관찰` | 200 | 부분 일치 |
| `GET /search?name=bravo` | 200 | `Bravo휴게소` 반환 — 대소문자 무시가 동작(아래 주의) |
| `GET /search?name=   ` (공백) | 200 | `data: []` (리포지토리를 타지 않는다) |
| `GET /search` (파라미터 없음) | **400** | `INVALID_PARAMETER` |
| `GET /nearby` 좌표 없이 | 200 | 거리 없이 전체 |
| `GET /nearby` 좌표 포함 | 200 | `distanceMeters`가 실리고 **거리순 정렬** |

**주의 — 대소문자 무시는 이 계층에서 지켜지지 않는다.** 리포지토리에서 `IgnoreCase`를 빼고 돌려봤는데 E2E는 그대로 통과했다. MySQL의 기본 collation이 이미 대소문자를 구분하지 않아 `LIKE`가 알아서 무시하기 때문이다. 그 키워드를 지키는 것은 H2에서 도는 단위 테스트 쪽이고, E2E가 고정하는 것은 사용자에게 나가는 결과다. (H2 쪽에서 실제로 빨간불이 나는지는 확인하지 못했다 — 단위 테스트가 메서드 이름을 직접 참조해 컴파일 단계에서 먼저 깨진다.)

`nearby` 응답은 목록 항목보다 넓다 — `distanceMeters`, `sizeTier`, `topTrafficTier`, `hasTheme` 등이 더 붙는다.

## 2. 상세 7종 — **균일하지 않다**

휴게소는 있는데 **연관 데이터가 하나도 없을 때**의 동작이 갈린다. 이것이 이번 관찰의 가장 큰 수확이다.

| 엔드포인트 | 휴게소 없음 | 휴게소만 있고 연관 데이터 없음 |
| --- | --- | --- |
| `GET /{code}` | 404 | 200 |
| `GET /{code}/basic-info` | 404 | 200 (`address`·`telNo`·`brand`가 `null`, `evChargerCount: 0`) |
| `GET /{code}/facilities` | 404 | 200 (`convenienceFacilities: []`, 나머지 전부 `null`) |
| `GET /{code}/foods` | 404 | 200 (`{"menus":[],"sections":[]}`) |
| `GET /{code}/events` | 404 | 200 (`{"events":[]}`) |
| `GET /{code}/sales-rankings` | 404 | 200 (`baseYearMonth: null`, 빈 배열들) |
| **`GET /{code}/oil-info`** | 404 | **404** ← 혼자 다르다 |

여섯은 "휴게소가 있으면 200, 연관 데이터가 없으면 빈 값"인데 **`oil-info`만 연관 데이터가 없으면 404**다. 프론트는 섹션별로 `404 + NOT_FOUND`를 "해당 없음"으로 처리하므로 화면이 깨지지는 않지만, 계약이 엔드포인트마다 갈린다는 사실 자체는 기록해둘 값이 있다.

## 3. 이미지 2종 — HTTP 캐시 계약

| 갈래 | status | Content-Type | ETag | Cache-Control |
| --- | --- | --- | --- | --- |
| 휴게소 없음 | **404** | `application/json` | 없음 | (기본) |
| 휴게소는 있고 이미지 없음 | **204** | 없음 | 없음 | (기본) |
| 이미지 있음 | 200 | `image/webp` | `"{md5}"` | `public, no-cache` |
| `If-None-Match: {같은 ETag}` | **304** | 없음 | 같은 값 | `public, no-cache` |
| `If-None-Match: *` | **304** | 없음 | 같은 값 | `public, no-cache` |
| `If-None-Match: "other"` | 200 | `image/webp` | 같은 값 | `public, no-cache` |

`detail`과 `list`는 **서로 다른 바이트와 다른 ETag**를 준다. ETag는 본문의 MD5다.

이 구간이 이번 작업에서 단위 테스트로 대체 불가능한 정도가 가장 크다 — ETag 계산, 조건부 GET, 304의 본문 없음은 실제 HTTP 왕복이 있어야 돈다.

## 4. `POST /{code}/oil-price/refresh` — CSRF가 걸린다

토큰 없이 POST하면 **403 Forbidden**이고, 본문이 `ApiResponse`가 아니라 스프링 기본 오류 형식이다.

```json
{"timestamp":"…","status":403,"error":"Forbidden","path":"/api/rest-stops/…/oil-price/refresh"}
```

**버그가 아니라 설계다.** `SecurityConfig`가 `/api/**`를 `permitAll`로 열어두지만 **CSRF는 끄지 않았고**, 프론트는 `csrfHeadersFrom()`으로 토큰을 붙여 보낸다.

토큰이 흐르는 경로:

```
GET /  →  <meta name="_csrf" content="{토큰}">
          <meta name="_csrf_header" content="{헤더명}">
       →  프론트가 읽어서 POST 헤더에 실음 (세션 쿠키와 함께)
```

인수 테스트도 이 경로를 그대로 재현해야 한다 — 페이지를 한 번 받아 토큰과 세션 쿠키를 얻고, 그 둘을 POST에 실는다. **브라우저가 실제로 밟는 흐름을 통째로 검증하는 유일한 방법**이라 값이 크다.

### 토큰을 실었을 때 (ExApi WireMock 세운 뒤 관찰)

| 갈래 | status | code |
| --- | --- | --- |
| 외부 유가 조회 성공 | 200 | `SUCCESS` (`gasolinePrice`/`dieselPrice`/`lpgPrice`) |
| 휴게소 없음 | 404 | `NOT_FOUND` — 외부를 부르지 않는다 |
| 주유소 연결 없음 | 404 | `NOT_FOUND` — 조회 키가 없어 외부를 부르지 않는다 |
| **외부 HTTP 500** | **200** | **`EXTERNAL_API_UNAVAILABLE`** |
| **외부 연결 끊김** | **200** | **`EXTERNAL_API_UNAVAILABLE`** |
| **외부 조회 결과 비었음** | **404** | **`NOT_FOUND`** |

**외부 실패와 빈 결과가 갈린다.** 처음엔 셋 다 404일 거라 짐작하고 테스트를 썼는데 두 개가 빨간불이 나서 다시 관찰했다 — 추측이 틀린 것을 테스트가 잡아준 사례다.

갈리는 것이 맞는 동작이기도 하다. "지금은 유가를 못 가져왔다"(재시도하면 될 수도 있음)와 "이 휴게소엔 유가 정보가 없다"(재시도해도 소용없음)는 화면에서 다른 안내로 이어져야 한다.

경유 가격은 외부 JSON에서만 이름이 `diselPrice`다(공공 API 원본의 철자). 이 매핑은 클라이언트를 목으로 바꾸는 테스트에서는 실행되지 않는다.

---

## 이번 관찰이 바꾼 계획

1. **상세 6종을 한 덩어리로 묶으려던 것을 나눈다.** `oil-info`가 다른 계약이므로 같이 묶으면 그 차이가 지워진다.
2. **이미지 테스트에 "휴게소 없음 404"와 "이미지 없음 204"를 둘 다 넣는다.** 처음엔 204 하나만 생각했는데 갈래가 둘이다.
3. **`oil-price/refresh`에 CSRF 흐름이 추가된다.** 원래 계획엔 없던 작업이고, 토큰 없는 POST가 403이라는 것 자체도 고정할 값이 있다(CSRF 보호가 실제로 걸려 있다는 증거).

## 이 기록의 쓰임

정렬·검색이 H2에서만 통과하고 MySQL에서 깨지는 종류의 회귀, 그리고 캐시 헤더 계약이 조용히 바뀌는 종류의 회귀를 인수 테스트가 잡아준다. 표의 값이 바뀌는 것 자체는 문제가 아니고, 모르는 채로 바뀌는 것이 문제다.

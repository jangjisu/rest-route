# API.md — 외부 연동과 내부 API 계약

> 한국도로공사 베이스 URL: `https://data.ex.co.kr`
> 오피넷 베이스 URL: `https://www.opinet.co.kr`
> 현재 코드에 실제로 연결된 외부 API와 화면에서 사용하는 내부 API만 기록한다.

---

## 공통 요청 헤더

한국도로공사 OpenAPI를 호출하는 `ex-api` Feign Client는 모든 요청에 다음 헤더를 보낸다.

```text
User-Agent: rest-route
Accept: application/json
```

동일 endpoint를 호출해 다음 결과를 실측했다.

| 요청 조건 | 결과 |
|---|---|
| 기본 `curl/8.7.1` User-Agent | `400 Request Blocked` |
| User-Agent 없음 | `400 Request Blocked` |
| `User-Agent: vroom-tracker` | `200 OK` |

HTTP/1.1 강제 여부는 결과에 영향을 주지 않았다. endpoint, query parameter와 response 구조는 변경되지 않는다.

### 공통 실패 exception

`ExApiClient`는 빈 응답, API 실패 응답, Feign·네트워크·디코딩 예외를 다음 형식의 `ExApiException`으로 변환한다.

```text
Failed to fetch API. requestUrl=<실제 요청 URL>, message=<실패 원인>
```

`requestUrl`에는 endpoint와 실제 호출한 query parameter가 포함된다. 인증키가 포함된 query parameter는 로그에서 마스킹한다.

```text
Failed to fetch API. requestUrl=https://data.ex.co.kr/openapi/business/conveniServiceArea?key=%3Credacted%3E&type=json&numOfRows=99&pageNo=2, message=For input string: ""
```

API key는 애플리케이션 설정과 로그에 평문으로 남기지 않는다.

---

## 현재 연동 API

### getChargerInfo — 환경공단 전기자동차 충전소 정보

전국 전기자동차 충전소의 충전기 원본 정보를 조회한다. 현재는 고속도로 휴게소에 해당하는
`kindDetail=C001` 데이터만 저장하고 휴게소 매핑에 사용한다. 충전기 실시간 상태 API인
`getChargerStatus`는 아직 사용하지 않는다.

#### 엔드포인트

```text
GET https://apis.data.go.kr/B552584/EvCharger/getChargerInfo
```

#### 요청 헤더

```text
User-Agent: rest-route
Accept: application/json
```

#### 요청 파라미터

| 파라미터 | 값 | 설명 |
|---|---|---|
| `serviceKey` | `EV_API_KEY` | 환경변수로 주입하는 인증키 |
| `pageNo` | 페이지 번호 | 1부터 시작 |
| `numOfRows` | `200` | 페이지당 조회 건수 |
| `dataType` | `JSON` | JSON 응답 요청 |
| `kind` | `C0` | 충전소 유형 필터 |

응답의 `totalCount`와 `numOfRows`로 전체 페이지 수를 계산한다. 일부 페이지가
timeout이나 외부 API 오류로 실패해도 성공한 페이지의 데이터는 동기화하고 다음 페이지 조회를
계속한다. 실패한 페이지의 기존 DB 데이터는 삭제하지 않는다. 첫 페이지가 실패하거나 성공한
C001 데이터가 하나도 없으면 DB를 변경하지 않고 기존 충전기와 휴게소 매핑을 유지한다.

#### 주요 응답 구조

```json
{
  "resultCode": "00",
  "resultMsg": "NORMAL SERVICE.",
  "totalCount": 2401,
  "pageNo": 1,
  "numOfRows": 200,
  "items": {
    "item": [
      {
        "statNm": "기흥(부산) 휴게소",
        "statId": "ME178003",
        "chgerId": "01",
        "addr": "경기도 용인시 기흥구 공세로 173",
        "lat": "37.2356557",
        "lng": "127.105021",
        "stat": "2",
        "kind": "C0",
        "kindDetail": "C001",
        "delYn": "N",
        "maker": "채비"
      }
    ]
  }
}
```

`kindDetail=C001`만 저장·매핑 대상으로 취급하며 `C002`, `C003` 등 다른 시설 유형은 제외한다.
`statId + chgerId`를 충전기 자연키로 사용하고, 페이지별 요청 시작·성공·실패 로그에는
페이지 번호와 건수만 기록한다. 응답 payload와 `serviceKey`는 로그에 출력하지 않는다.

---

### avgAllPrice — 오피넷 주유소 전국 평균가격(현재)

전국 주유소의 현재 평균 판매가격을 반환한다. 경로 결과 화면의 평균가 요약과 휴게소별 가격의
전국 평균 대비 차이 계산에 사용한다.

#### 엔드포인트

```text
GET https://www.opinet.co.kr/api/avgAllPrice.do
```

#### 요청 파라미터

| 파라미터 | 조건 | 설명 |
|---|---|---|
| `out` | 필수 | 현재 앱에서는 `json`만 사용 |
| `code` | 필수 | 오피넷에서 발급한 무료 API 키 |

#### 2026-07-07 실측 결과

- HTTP 상태: `200 OK`
- 응답 최상위: `RESULT`
- 목록 필드: `RESULT.OIL`
- 별도 성공 코드는 없으며, 현재 코드는 `RESULT.OIL` 존재 여부로 성공을 판단한다.
- `TRADE_DT`는 `yyyyMMdd` 문자열이다.
- `PRICE`와 `DIFF`는 소수점 문자열이다. 현재 응답에서 자동차용부탄(`K015`)의 `DIFF`는 `+0.01`처럼 `+` 부호를 포함한다.
- 경로 결과에서 사용하는 제품은 휘발유(`B027`), 자동차용경유(`D047`), 자동차용부탄(`K015`)이다.

```json
{
  "RESULT": {
    "OIL": [
      {
        "TRADE_DT": "20260707",
        "PRODCD": "B027",
        "PRODNM": "휘발유",
        "PRICE": "1892.88",
        "DIFF": "-4.19"
      },
      {
        "TRADE_DT": "20260707",
        "PRODCD": "D047",
        "PRODNM": "자동차용경유",
        "PRICE": "1880.08",
        "DIFF": "-4.51"
      },
      {
        "TRADE_DT": "20260707",
        "PRODCD": "K015",
        "PRODNM": "자동차용부탄",
        "PRICE": "1135.19",
        "DIFF": "+0.01"
      }
    ]
  }
}
```

#### 코드 기준 처리

- `OPINET_API_KEY` 환경변수로 키를 주입한다.
- prod 프로필에서는 `OPINET_API_KEY`가 비어 있거나 placeholder면 부팅을 실패시킨다.
- `national_oil_price` 테이블에 `trade_date + product_code` 기준으로 일별 평균가를 저장한다.
- 경로 결과 조회 시 오늘 평균가가 DB에 없으면 오피넷을 호출해 해당 거래일 데이터를 저장한다.
- 오피넷 호출 실패 또는 필수 제품 평균가 누락 시 기존 경로 결과는 유지하고 평균가 요약과 평균 대비 차이값만 생략한다.

### conveniServiceArea — 휴게소 편의시설 정보

#### 서버 오류 응답

동일 요청에서 `Accept` 헤더에 따라 같은 upstream 서버 오류가 다른 형식으로 반환되는 것을 확인했다.

| 요청 조건 | HTTP 상태 | Content-Type | 본문 |
|---|---|---|---|
| `Accept` 없음 또는 HTML 선호 | `200 OK` | `text/html` | 시스템 장애 HTML |
| `Accept: application/json` | `200 OK` | `application/json` | `exception.message`를 포함한 JSON |

실측한 upstream 오류 메시지는 다음과 같다.

```text
For input string: ""
```

stack trace상 한국도로공사 서버의 `ConveniServiceAreaListVO`가 빈 문자열을 숫자로 변환하다 실패한 오류다. 오류인데도 HTTP 상태가 `200 OK`이므로 상태 코드만으로 성공을 판단하지 않고 `code`와 `exception.message`를 확인해야 한다.

현재 `RestStopDetailResponse.getErrorMessage()`는 다음 우선순위로 오류 메시지를 반환한다.

1. upstream 서버 오류의 `exception.message`
2. 인증키 오류 등 일반 API 실패 응답의 최상위 `message`

---

### locationinfoRest — 휴게소 위치 정보

전국 고속도로 휴게소의 위치 좌표와 기본 정보를 반환한다.

현재 코드에서는 다음 파일이 이 API를 담당한다.

- `src/main/java/com/vroomtracker/client/ExApiClient.java`
- `src/main/java/com/vroomtracker/client/ExApiFeignClient.java`
- `src/main/java/com/vroomtracker/client/response/RestStopResponse.java`
- `src/main/java/com/vroomtracker/client/response/RestStopItem.java`

### 엔드포인트

```text
GET https://data.ex.co.kr/openapi/locationinfo/locationinfoRest
```

### 요청 파라미터

현재 `ExApiClient`에 정의된 파라미터는 다음과 같다.

| 파라미터 | 조건 | 설명 |
|---|---|---|
| `key` | 필수 | 인증키 |
| `type` | 필수 | 현재 앱에서는 `json`만 사용 |
| `numOfRows` | 선택 | 페이지당 결과 수 |
| `pageNo` | 선택 | 페이지 번호 |

한국도로공사 문서상 `unitCode`, `routeNo`, `stdRestCd`, `serviceAreaCode` 등 추가 선택 파라미터가 있을 수 있으나, 현재 코드에는 아직 연결하지 않았다.

### 응답 구조

현재 응답 VO는 다음 필드를 가진다.

```json
{
  "code": "SUCCESS",
  "message": "인증키가 유효합니다.",
  "count": "203",
  "pageSize": "3",
  "list": [
    {
      "unitCode": "001",
      "unitName": "서울만남(부산)휴게소",
      "routeNo": "0010",
      "routeName": "경부선",
      "xValue": "127.042514",
      "yValue": "37.459939",
      "stdRestCd": "000001",
      "serviceAreaCode": "A00001"
    }
  ]
}
```

### 코드 기준 처리

- 성공 여부는 `RestStopResponse.isSuccess()`에서 판단한다.
- 현재 성공 코드는 `"SUCCESS"`로 판단한다.
- `ExApiClient` wrapper가 인증키, `json` 포맷, `numOfRows=99` 기본값을 적용한다.
- `pageSize`는 실제 응답 기준 총 페이지 수로 보고 `getTotalPageCount()`에서 정수 변환한다.
- `pageSize`가 숫자가 아니면 기본값 `1`을 반환한다.

---

### hiwaySvarInfoList — 휴게소 영업시설 정보

휴게소 방향, 주소, 대표 전화번호와 차종별 주차 대수를 반환한다.

#### 엔드포인트

```text
GET https://data.ex.co.kr/openapi/restinfo/hiwaySvarInfoList
```

현재 `ExApiClient`는 `key`와 `type=json`만 전달하며 전체 목록을 한 번에 조회한다. 응답은
`HighwayServiceAreaInfoResponse`와 `HighwayServiceAreaInfoItem`으로 역직렬화한 뒤
`highway_service_area_info` 테이블을 전체 교체한다.

상세 조회에서는 `businessFacilityCode`와 휴게소 `serviceAreaCode`를 연결해 방향, 주소와
소형·대형·장애인 주차 대수를 조합한다.

---

### restOilList — 휴게소 편의시설 현황(주유소)

전국 고속도로 주유소의 편의시설과 운영시간을 반환한다.

#### 엔드포인트

```text
GET https://data.ex.co.kr/openapi/restinfo/restOilList
```

#### 요청 파라미터

| 파라미터 | 조건 | 설명 |
|---|---|---|
| `key` | 필수 | 인증키 |
| `type` | 필수 | 현재 앱에서는 `json`만 사용 |
| `routeCd` | 선택 | 노선코드 |
| `routeNm` | 선택 | 노선명 |
| `stdRestCd` | 선택 | 휴게소/주유소코드 |
| `stdRestNm` | 선택 | 휴게소/주유소명 |

현재 `ExApiClient`는 전체 목록 조회를 위해 `key`, `type`만 전달한다.
페이지네이션 파라미터는 없으며 전체 목록이 한 응답으로 반환된다.

#### 2026-06-15 실측 결과

- HTTP 상태: `200 OK`
- Content-Type: `application/json; charset=utf-8`
- 성공 코드: `"SUCCESS"`
- `count`: 숫자 `429`
- 동일한 `stdRestCd`에 서로 다른 `psCode` 행이 여러 개 존재한다.
- `routeNm`과 `psDesc`는 `null`일 수 있다.
- 제공 문서에는 `count`가 string으로 표기되어 있지만 실제 JSON은 숫자다.

```json
{
  "list": [
    {
      "stdRestCd": "000002",
      "stdRestNm": "서울만남(부산)주유소",
      "stime": "00:00",
      "etime": "24:00",
      "redId": "MANJ03",
      "redDtime": "16/03/10",
      "lsttmAltrUser": "SYSTEM",
      "lsttmAltrDttm": "2026-06-15",
      "svarAddr": "서울시 서초구 원지동10-16",
      "routeCd": "0010",
      "routeNm": "경부선",
      "psCode": "07",
      "psName": "쉼터",
      "psDesc": "고객쉼터-안마기,컴퓨터,팩스,혈압계,신장및체중계,핸드폰충전기,핸드폰소독기"
    }
  ],
  "count": 429,
  "message": "인증키가 유효합니다.",
  "code": "SUCCESS"
}
```

#### 코드 기준 처리

- 성공 여부는 `RestOilResponse.isSuccess()`에서 판단한다.
- `count`는 실측 JSON 타입에 맞춰 `int`로 역직렬화한다.
- 원본 행은 `RestOilItem`에 그대로 보존하며 중복처럼 보이는 휴게소 코드를 합치지 않는다.
- 빈 응답, 실패 코드와 Feign 예외는 `ExApiClient` 공통 `fetch()`에서 처리한다.

---

### curStateStation — 주유소 가격 현황

전국 고속도로 주유소의 현재 가격과 정유사, LPG 여부, 전화번호, 주소를 반환한다.

#### 엔드포인트

```text
GET https://data.ex.co.kr/openapi/business/curStateStation
```

#### 요청 파라미터

| 파라미터 | 조건 | 설명 |
|---|---|---|
| `key` | 필수 | 인증키 |
| `type` | 필수 | 현재 앱에서는 `json`만 사용 |
| `numOfRows` | 선택 | 페이지당 결과 수 |
| `pageNo` | 선택 | 페이지 번호 |
| `oilCompany` | 선택 | 정유사 |
| `routeCode` | 선택 | 노선코드 |
| `serviceAreaCode` | 선택 | 영업부대시설코드 |
| `routeName` | 선택 | 노선명 |
| `serviceAreaCode2` | 선택 | 휴게소/주유소코드 |
| `serviceAreaName` | 선택 | 휴게소/주유소명 |
| `direction` | 선택 | 방향 |

현재 `ExApiClient`는 전체 목록 조회를 위해 `key`, `type`, `numOfRows=99`, `pageNo=1..3`만 전달한다.
단건 실시간 가격 갱신에서는 `pageNo=1`과 `serviceAreaCode2`를 함께 전달한다.

#### 2026-06-16 실측 결과

- HTTP 상태: `200 OK`
- Content-Type: `application/json; charset=utf-8`
- 성공 코드: `"SUCCESS"`
- `count`: 숫자 `226`
- `pageSize`: 숫자 `3`
- `pageNo`, `numOfRows`: 최상위는 숫자, list 내부의 동일 필드는 `null`
- `diselPrice`는 API 원문 오탈자 그대로 응답되며 코드에서는 `dieselPrice`로 매핑한다.
- 가격은 `"1,999원"` 같은 문자열이며 판매하지 않는 항목은 `"X"`로 내려온다.
- `serviceAreaCode2`는 주유소 코드이며 `restOilList.stdRestCd`와 연결된다.

```json
{
  "count": 226,
  "list": [
    {
      "direction": "부산",
      "pageNo": null,
      "numOfRows": null,
      "routeName": "경부선",
      "serviceAreaCode": "B00001",
      "serviceAreaName": "서울만남(부산)주유소",
      "telNo": "02-573-7430",
      "routeCode": "0010",
      "oilCompany": "AD",
      "lpgYn": "Y",
      "gasolinePrice": "1,999원",
      "diselPrice": "1,997원",
      "lpgPrice": "1,157원",
      "serviceAreaCode2": "000002",
      "svarAddr": "서울시 서초구 원지동10-16"
    }
  ],
  "pageNo": 1,
  "numOfRows": 99,
  "pageSize": 3,
  "message": "인증키가 유효합니다.",
  "code": "SUCCESS"
}
```

#### 코드 기준 처리

- 성공 여부는 `RestOilPriceResponse.isSuccess()`에서 판단한다.
- 원본 행은 `RestOilPriceItem`에 보존하고, 저장 시 문자열 값을 임의 변환하지 않는다.
- `RestOilPriceSyncService`는 페이지 1~3만 호출해 전체 교체 저장한다.
- `RestOilPriceRefreshService`는 `serviceAreaCode2`로 단건 조회한 첫 번째 list 항목만 저장한다.
- 페이지 중 하나라도 실패하면 DB 교체 트랜잭션을 실행하지 않아 기존 데이터를 보존한다.
- 빈 응답, 실패 코드와 Feign 예외는 `ExApiClient` 공통 `fetch()`에서 처리한다.

---

### restBestfoodList — 휴게소 음식 메뉴

전국 고속도로 휴게소의 음식 메뉴명, 가격, 설명, 추천/베스트/프리미엄/계절 구분을 반환한다.

#### 엔드포인트

```text
GET https://data.ex.co.kr/openapi/restinfo/restBestfoodList
```

#### 요청 파라미터

| 파라미터 | 조건 | 설명 |
|---|---|---|
| `key` | 필수 | 인증키 |
| `type` | 필수 | 현재 앱에서는 `json`만 사용 |
| `numOfRows` | 선택 | 페이지당 결과 수 |
| `pageNo` | 선택 | 출력 페이지 번호 |
| `routeCd` | 선택 | 노선코드 |
| `routeNm` | 선택 | 노선명 |
| `stdRestCd` | 선택 | 휴게소/주유소코드 |
| `stdRestNm` | 선택 | 휴게소/주유소명 |
| `recommendyn` | 선택 | 추천메뉴 구분 |
| `bestfoodyn` | 선택 | 베스트푸드 구분 |
| `premiumyn` | 선택 | 프리미엄메뉴 구분 |

`conveniServiceArea`/`curStateStation`과 동일하게 페이지네이션(`numOfRows`, `pageNo`)을 사용한다.

#### 2026-06-16 실측 결과

- HTTP 상태: `200 OK`
- Content-Type: `application/json; charset=utf-8`
- 성공 코드: `"SUCCESS"`
- `count`: 숫자 `7214`
- `pageSize`: 숫자 `73` (numOfRows=99 기준) — 전체 동기화 시 다중 페이지 순회 필요
- `pageNo`, `numOfRows`: 최상위는 숫자, list 내부의 동일 필드는 `null`
- 연결 키 `stdRestCd`는 휴게소 코드이며 `locationinfoRest.stdRestCd`와 직접 일치한다. (서울만남(부산)휴게소 = `000001`로 양쪽 동일) — 주유소(`restOilList`)와 달리 정규화 이름 매칭이 필요 없다.
- `restCd`(`S000001`)는 음식 API 전용 휴게소 코드로 `stdRestCd`와 다르다. 연결에는 `stdRestCd`를 사용한다.
- 대표메뉴 구분 플래그는 `recommendyn`(추천), `bestfoodyn`(베스트푸드), `premiumyn`(프리미엄) 세 가지로 분리되어 있다.
- `foodCost`는 `"7000"` 같은 순수 숫자 문자열이며 통화기호·콤마가 없다. (유가의 `"1,999원"`과 다름)
- `seasonMenu`는 `4`(사계절)/`S`(여름)/`W`(겨울) 구분이다.

```json
{
  "count": 7214,
  "list": [
    {
      "pageNo": null,
      "numOfRows": null,
      "stdRestCd": "000001",
      "stdRestNm": "서울만남(부산)휴게소",
      "restCd": "S000001",
      "routeCd": "0010",
      "routeNm": "경부선",
      "svarAddr": "서울 서초구 원지동10-16",
      "seq": "272",
      "foodNm": "농심어묵우동",
      "foodCost": "7000",
      "etc": "부산어묵꼬치를 첨가하여 ... 우동.",
      "foodMaterial": "냉동면 1ea\r\n육수 435ml\r\n...",
      "recommendyn": "N",
      "bestfoodyn": "N",
      "premiumyn": "N",
      "seasonMenu": "4",
      "app": "Y",
      "lsttmAltrUser": "SYSTEM",
      "lsttmAltrDttm": "2026-06-16",
      "lastId": "dmsrud527",
      "lastDtime": "2025-08-28"
    }
  ],
  "pageNo": 1,
  "numOfRows": 99,
  "pageSize": 73,
  "message": "인증키가 유효합니다.",
  "code": "SUCCESS"
}
```

#### 코드 기준 처리

- 성공 여부는 `RestBestfoodResponse.isSuccess()`에서 `"SUCCESS"`로 판단한다.
- 원본 행은 그대로 보존하고 `foodCost` 등 문자열 값을 임의 변환하지 않는다.
- 전체 동기화는 `pageSize`만큼 페이지를 순회하며, 페이지 중 하나라도 실패하면 DB 교체 트랜잭션을 실행하지 않아 기존 데이터를 보존한다.
- 빈 응답, 실패 코드와 Feign 예외는 `ExApiClient` 공통 `fetch()`에서 처리한다.

---

### restThemeList — 테마휴게소 현황

휴게소별 테마(체험/관광/힐링 등) 명칭과 상세 설명을 반환한다.

#### 엔드포인트

```text
GET https://data.ex.co.kr/openapi/restinfo/restThemeList
```

현재 `ExApiClient`는 `key`와 `type=json`만 전달하며 전체 목록을 한 번에 조회한다(페이지네이션 미사용).

#### 2026-07-28 실측 결과

- HTTP 상태: `200 OK`
- 성공 코드: `"SUCCESS"`
- `count`: `144` — 페이지네이션 없이 한 번의 호출로 전체 목록이 반환됨을 확인
- `stdRestCd`는 `locationinfoRest.stdRestCd`와 직접 일치한다.
- 한 휴게소에 테마가 여러 개 붙을 수 있다(자연키는 `stdRestCd`+`itemNm`).

```json
{
  "count": 144,
  "list": [
    {
      "detail": "365일 꽃향기가 나는 휴게소입니다",
      "stdRestCd": "000001",
      "stdRestNm": "서울만남(부산)휴게소",
      "svarAddr": "서울 서초구 원지동10-16",
      "routeCd": "0010",
      "routeNm": "경부선",
      "itemNm": "4계절 꽃이 있는 휴게소",
      "regId": "MANN003",
      "regDtime": "07/12/2024 17:21:31."
    }
  ],
  "message": "인증키가 유효합니다.",
  "code": "SUCCESS"
}
```

#### 코드 기준 처리

- 성공 여부는 `RestThemeResponse.isSuccess()`에서 판단한다.
- 동기화는 `stdRestCd`+`itemNm` 자연키로 기존 행을 찾아 있으면 갱신, 없으면 새로 생성한다(삭제 로직 없음 — `HighwayServiceAreaInfoSyncService`와 동일 패턴).

---

### restEventList — 휴게소 이벤트 현황

휴게소별 진행 이벤트명, 내용, 기간(`stime`~`etime`)을 반환한다.

#### 엔드포인트

```text
GET https://data.ex.co.kr/openapi/restinfo/restEventList
```

현재 `ExApiClient`는 `key`와 `type=json`만 전달하며 전체 목록을 한 번에 조회한다(페이지네이션 미사용).

#### 2026-07-28 실측 결과

- HTTP 상태: `200 OK`
- 성공 코드: `"SUCCESS"`
- `count`: `898`
- 상당수 행이 오래전에 종료된 이벤트다(예: `stime`/`etime`이 2020~2021년). 화면에 노출할 때는 오늘 날짜가 `stime`~`etime` 사이인 것만 걸러야 한다 — 이번 백엔드 연동 단계에서는 원본을 그대로 저장만 하고, 필터링은 프론트엔드 연동 단계에서 구현한다.
- `eventSeq`가 한 휴게소 내 이벤트를 구분하는 일련번호다(자연키는 `stdRestCd`+`eventSeq`).

```json
{
  "count": 898,
  "list": [
    {
      "stdRestCd": "000001",
      "stime": "2020-01-01",
      "etime": "2027-12-31",
      "routeCd": "0010",
      "svarAddr": "서울 서초구 원지동10-16",
      "routeNm": "경부선",
      "stdRestNm": "서울만남(부산)휴게소",
      "lastId": "dmsrud527",
      "lastDtime": "20260621150549",
      "stdRestGubun": "S",
      "eventSeq": "1665",
      "eventDetail": "우리 휴게소에서 한식당 식사시 TEN+1 이벤트를 이용하실 수 있습니다.",
      "eventNm": "TEN+1 이벤트"
    }
  ],
  "message": "인증키가 유효합니다.",
  "code": "SUCCESS"
}
```

#### 코드 기준 처리

- 성공 여부는 `RestEventResponse.isSuccess()`에서 판단한다.
- 동기화는 `stdRestCd`+`eventSeq` 자연키로 기존 행을 찾아 있으면 갱신, 없으면 새로 생성한다(삭제 로직 없음).

---

## 내부 API

모든 JSON 응답은 `ApiResponse<T>` 형식을 사용한다.

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {}
}
```

### GET /api/map-config

브라우저가 네이버 지도 스크립트를 불러올 때 사용할 `naverMapsNcpKeyId`를 반환한다.

### GET /api/rest-stops

DB에 저장된 전체 휴게소 위치 목록을 반환한다. 각 항목에는 휴게소명, 노선, 좌표,
`stdRestCd`와 상세 조회에 사용하는 `serviceAreaCode`가 포함된다.

### GET /api/rest-stops/search?name={query}

휴게소명 부분일치(대소문자 무시)로 검색해 `GET /api/rest-stops`와 동일한 형태의 목록을 반환한다.
`name`이 비어있거나 공백만 있으면 DB를 조회하지 않고 빈 배열을 반환한다. 일치하는 휴게소가 없어도
HTTP 200과 빈 배열을 반환하며 에러로 취급하지 않는다.

### GET /api/rest-stops/{serviceAreaCode}

휴게소 위치를 기준으로 상세, 영업시설, 주유소 편의시설, 주유 가격과 먹거리 데이터를 조합해 반환한다.

주요 응답 영역은 다음과 같다.

- 위치: 노선, 좌표, 주소와 방향
- 시설: 입점 브랜드, 편의시설, 경정비·화물휴게소 운영 여부와 차종별 주차 수
- `oilInfo`: 정유사, 유종별 가격, 전화번호, 마지막 갱신 시각과 주유소 편의시설
- `foodMenu.menus`: 메뉴명, 가격, 설명, 추천 대표 메뉴 여부, 베스트/프리미엄/계절 구분
- `foodMenu.sections`: 화면에서 바로 사용할 추천 메뉴 그룹

`brand`는 한국도로공사 `conveniServiceArea.brand`를 저장한 값이다. 값이 없으면 `null`이다.
`foodMenu.menus[].representative`, `bestFood`, `premium`은 각각 `recommendyn`, `bestfoodyn`,
`premiumyn`이 `"Y"`일 때 `true`다. `season`은 `seasonMenu` 원본 코드(`4`, `S`, `W` 등)를 그대로 반환하고,
`seasonLabel`은 화면 표시용 라벨(`사계절`, `여름`, `겨울`)이다.
`foodMenu.sections`는 기존 메뉴 목록에서 파생한 화면용 그룹이며, `recommended`, `premium`, `seasonal`
순서로 값이 있는 그룹만 반환한다. `recommended`는 대표 또는 베스트 메뉴, `premium`은 프리미엄 메뉴,
`seasonal`은 여름(`S`) 또는 겨울(`W`) 계절 메뉴를 포함한다.

해당 `serviceAreaCode`가 없으면 HTTP 404와 `NOT_FOUND`를 반환한다.

### 휴게소 대표 이미지 API

`GET /api/rest-stops/{serviceAreaCode}/basic-info`의 `data.detailImageUrl`과
`GET /api/route-rest-stops`의 `data.restStops[].listImageUrl`은 이미지가 있을 때 각각 상세용·목록용
바이너리 URL을 반환하고, 이미지가 없으면 `null`이다. JSON 응답에는 이미지 BLOB을 포함하지 않는다.

#### GET /api/rest-stops/{serviceAreaCode}/images/detail

#### GET /api/rest-stops/{serviceAreaCode}/images/list

저장된 상세용 또는 목록용 WebP 바이너리를 `Content-Type: image/webp`로 반환한다. 성공 응답은
데이터 기반 `ETag`와 `Cache-Control: public, no-cache`를 포함하며, 일치하는 `If-None-Match` 요청에는
`304 Not Modified`를 반환한다. 휴게소는 있지만 이미지가 없으면 `204 No Content`, 휴게소 자체가 없으면
`404 Not Found`를 반환한다. 이 두 바이너리 응답은 `ApiResponse<T>` 형식을 사용하지 않는다.

#### PUT /api/admin/rest-stops/{serviceAreaCode}/image

관리자 인증과 CSRF 토큰이 필요한 등록·교체 API다. `multipart/form-data`의 `file` 필드로 최대 20MB
JPEG 또는 PNG 한 장을 보낸다. 서버는 상세용 1600px와 목록용 480px WebP로 변환해 기존 이미지가
있으면 교체하며 성공 시 `204 No Content`를 반환한다. 파일 형식·크기·이미지 해석이 유효하지 않으면
`400 Bad Request`, 휴게소가 없으면 `404 Not Found`를 반환한다.

#### DELETE /api/admin/rest-stops/{serviceAreaCode}/image

관리자 인증과 CSRF 토큰이 필요한 삭제 API다. 이미지가 있거나 이미 없는 경우 모두 멱등적으로
`204 No Content`를 반환하고, 휴게소 자체가 없으면 `404 Not Found`를 반환한다.

### GET /api/admin/dashboard

관리자 인증(`ROLE_ADMIN`)이 필요하다. 대시보드 요약(`restStopCount`, `latestSalesRankingMonth`, `lastSyncStatus`)과 함께 `recentActivityLogs` 목록을 반환한다. `recentActivityLogs`는 관리자 쓰기 작업(판매순위 업로드 2종, 전체 휴게소명 매핑, 휴게소 이미지 등록·삭제, 휴게소 정보 수정, 동기화 잠금 해제)이 성공할 때마다 기록되며, 각 항목은 `actor`(로그인 계정명), `message`(사람이 읽는 한 줄 설명), `occurredAt`(`yyyy-MM-dd HH:mm` 형식 문자열)로 구성된다. 최근 50건을 시각 내림차순으로 반환하며, 활동이 없으면 빈 배열이다.

### 관리자 휴게소 정보 편집 API

관리자 인증(`ROLE_ADMIN`)이 필요한 API 3종으로, `rest_stop`/`rest_stop_detail`의 편집 대상 필드를 조회·저장·잠금해제한다. 응답은 `ApiResponse<AdminRestStopEditableResponse>` 형식이며 필드는 다음과 같다: `serviceAreaCode`, `unitCode`(읽기 전용), `unitName`, `routeNo`, `routeName`, `xValue`, `yValue`, `telNo`, `brand`, `routeCode`, `svarAddr`, `convenience`, `maintenanceYn`, `truckSaYn`, `adminOverridden`(동기화 잠금 여부).

#### GET /api/admin/rest-stops/{serviceAreaCode}/editable

편집 대상 필드의 현재 값과 `adminOverridden` 상태를 반환한다. `rest_stop` 자체가 없으면 `404 Not Found`. `rest_stop_detail` 행이 아직 없으면(위치정보 API와 편의시설 API의 동기화 시점 차이로 발생 가능) 상세 관련 필드는 모두 `null`로 반환하고 `adminOverridden`은 `rest_stop` 기준으로만 판단한다.

#### PUT /api/admin/rest-stops/{serviceAreaCode}/editable

JSON 바디로 편집 대상 필드 전체를 받아 저장한다. `rest_stop_detail` 행이 없으면 `serviceAreaCode`만 채운 새 행을 만들어 저장한다(관리자가 상세 정보를 직접 생성하는 경로). 저장에 성공하면 `rest_stop`/`rest_stop_detail` 두 행 모두 `admin_overridden=true`로 바뀌어 이후 자동 동기화 대상에서 제외된다. `xValue`/`yValue`는 비어 있지 않다면 실수로 파싱 가능해야 하며, 실패하면 `400 Bad Request`. `rest_stop` 자체가 없으면 `404 Not Found`.

#### DELETE /api/admin/rest-stops/{serviceAreaCode}/editable/override

`admin_overridden`을 false로 되돌려 다음 자동 동기화부터 다시 갱신 대상이 되게 한다. `rest_stop` 또는 `rest_stop_detail` 행이 없으면 `404 Not Found`(저장을 한 번도 하지 않아 잠글 대상 자체가 없는 경우).

### 관리자 휴게소 음식 메뉴 관리 API

관리자 인증(`ROLE_ADMIN`)이 필요한 API로, `rest_food`의 메뉴를 조회·추가·수정·삭제하고 동기화 잠금을 해제하며, 메뉴별 이미지를 등록·삭제한다. 텍스트 응답은 `ApiResponse<AdminRestFoodResponse>`(또는 목록) 형식이며 필드는 다음과 같다: `id`, `foodName`, `foodCost`, `description`, `adminOverridden`(동기화 잠금 여부), `adminCreated`(관리자가 직접 추가한 메뉴인지 여부 — `seq`가 `ADMIN-`으로 시작하는지로 판단), `hasImage`(`rest_food_image`에 해당 메뉴 이미지가 실제로 저장되어 있는지 여부 — 관리자 화면이 등록된 이미지가 없는 메뉴에 "이미지 삭제" 버튼을 잘못 노출하지 않도록 사용).

#### GET /api/admin/rest-stops/{serviceAreaCode}/foods

해당 휴게소의 전체 메뉴 목록을 반환한다(동기화 메뉴와 관리자 추가 메뉴를 구분 없이 함께 반환하며, 각 항목의 `adminOverridden`/`adminCreated`로 구분). 휴게소가 없으면 `404 Not Found`.

#### POST /api/admin/rest-stops/{serviceAreaCode}/foods

JSON 바디(`foodName`, `foodCost`, `description`)로 새 메뉴를 추가한다. 외부 API와 겹치지 않는 `seq`(`ADMIN-` + UUID)로 생성되며 `adminOverridden=true`로 시작한다. 휴게소가 없으면 `404 Not Found`.

#### PUT /api/admin/rest-stops/{serviceAreaCode}/foods/{foodId}

JSON 바디로 기존 메뉴(동기화 메뉴 포함) 값을 수정한다. 저장에 성공하면 `admin_overridden=true`로 바뀌어 이후 자동 동기화 대상에서 제외된다. 메뉴가 없으면 `404 Not Found`.

#### DELETE /api/admin/rest-stops/{serviceAreaCode}/foods/{foodId}/override

`admin_overridden`을 false로 되돌려 다음 자동 동기화부터 다시 갱신 대상이 되게 한다. 메뉴가 없으면 `404 Not Found`.

#### DELETE /api/admin/rest-stops/{serviceAreaCode}/foods/{foodId}

관리자가 직접 추가한 메뉴(`adminCreated=true`)만 삭제한다. 동기화 메뉴는 삭제해도 다음 동기화 때 외부 API에 남아 있으면 다시 생성되어 의미가 없으므로 삭제를 허용하지 않고 `400 Bad Request`(`INVALID_PARAMETER`)를 반환한다. 메뉴가 없으면 `404 Not Found`.

#### GET /api/admin/rest-stops/{serviceAreaCode}/foods/{foodId}/image

관리자 화면이 등록된 메뉴 이미지를 미리보기로 보여주기 위한 관리자 전용 조회 API다(사용자 화면에 노출하는 공개 조회 API가 아님). 목록용(480px) WebP 바이너리를 `Content-Type: image/webp`로 반환한다. 메뉴는 있지만 이미지가 없으면 `204 No Content`, 메뉴 자체가 없으면 `404 Not Found`를 반환한다. 이 응답은 `ApiResponse<T>` 형식을 사용하지 않는다.

#### PUT /api/admin/rest-stops/{serviceAreaCode}/foods/{foodId}/image

`multipart/form-data`의 `file` 필드로 메뉴 이미지를 등록·교체한다. `rest_stop_image`와 동일한 변환 규칙(상세용 1600px·목록용 480px WebP)을 재사용하며, 성공 시 `204 No Content`를 반환한다. 이 이미지를 사용자 화면에 노출하는 공개 조회 API는 아직 없다. 메뉴가 없으면 `404 Not Found`.

#### DELETE /api/admin/rest-stops/{serviceAreaCode}/foods/{foodId}/image

등록된 메뉴 이미지를 삭제한다. 성공 시 `204 No Content`를 반환하고, 메뉴가 없으면 `404 Not Found`.

### 관리자 휴게소 주유소 연결 관리 API

관리자 인증(`ROLE_ADMIN`)이 필요한 API로, `rest_stop`↔`rest_oil_price` 자동 매칭이 놓치거나 잘못 연결한 항목을 점검·수정한다. 사용자에게 실제로 보이는 주유소 정보(업체·가격·전화번호)는 `rest_oil_price`에서만 나오고 휴게소당 정확히 0~1건이므로, 이 API의 "주유소" 단위는 `rest_oil_price` 행 하나다. `rest_oil`(휴게소 부대 편의시설 목록, 물리적 주유소 하나당 여러 행)은 화면에 노출하지 않지만, 연결/해제/잠금해제 시 같은 물리적 주유소(자연키 `standardRestCode`/`serviceAreaCode2`)의 `rest_oil` 행 전체에도 동일한 조작을 함께 반영해 두 테이블의 연결 상태가 어긋나지 않게 한다.

#### GET /api/admin/rest-stops/oil-links

전체 휴게소와 각 휴게소에 현재 연결된 주유소(0~1건)를 한 번에 반환한다(페이지네이션 없음). 응답은 `ApiResponse<List<T>>`이며 각 항목은 `serviceAreaCode`, `unitName`, `routeName`, `linkedOilStation`(연결된 주유소, 없으면 `null`)로 구성된다. `linkedOilStation`은 `id`/`standardRestName`/`routeName`/`serviceAreaAddress`/`direction`/`adminOverridden`을 담는다.

#### GET /api/admin/oil-stations/search?name=&routeName=

`rest_oil_price`를 이름(`name`, 부분·대소문자 무관 일치)과 노선명(`routeName`, 정확히 일치) 조건으로 검색한다. 둘 다 선택 파라미터이며 하나만 줘도 그 조건만으로 조회하고, 둘 다 주면 두 조건을 모두 만족하는 행만 반환한다. 응답 각 항목은 `id`/`standardRestName`/`routeName`/`serviceAreaAddress`/`direction`/`linkedRestStopName`(이미 다른 휴게소에 연결돼 있으면 그 휴게소명, 아니면 `null`)로 구성된다. `name`과 `routeName`이 모두 비어 있으면 빈 배열을 반환한다.

#### PUT /api/admin/oil-stations/{oilId}/link

JSON 바디(`serviceAreaCode`)로 해당 주유소를 지정한 휴게소에 연결한다(기존 연결이 있었다면 교체). 성공 시 `adminOverridden=true`로 바뀌어 이후 자동 매칭 배치가 이 행을 건드리지 않는다. 같은 물리적 주유소의 `rest_oil` 행 전체도 함께 연결·잠금된다. 주유소 또는 휴게소가 없으면 `404 Not Found`.

#### DELETE /api/admin/oil-stations/{oilId}/link

연결을 해제한다(`restStopServiceAreaCode`를 `null`로). 이 경우도 `adminOverridden=true`로 잠겨 다음 배치가 같은 자동 매칭 결과로 다시 연결하지 않으며, 같은 물리적 주유소의 `rest_oil` 행 전체도 함께 해제·잠금된다. 주유소가 없으면 `404 Not Found`.

#### DELETE /api/admin/oil-stations/{oilId}/override

잠금을 해제해 다음 자동 매칭 배치부터 다시 갱신 대상이 되게 한다(연결 값 자체는 즉시 바뀌지 않고 다음 배치 실행 시 재계산된다). 같은 물리적 주유소의 `rest_oil` 행 전체도 함께 잠금 해제된다. 주유소가 없으면 `404 Not Found`.

### POST /api/rest-stops/{serviceAreaCode}/oil-price/refresh

특정 휴게소의 주유소 가격을 한국도로공사 `curStateStation` API에서 단건 조회해
`rest_oil_price`에 반영하고, 갱신된 `oilInfo`를 반환한다.
동일 주유소 가격이 최근 10분 이내 갱신된 경우에는 외부 API를 호출하지 않고 DB 값을 반환한다.

#### 요청

| 위치 | 이름 | 설명 |
|---|---|---|
| Path | `serviceAreaCode` | 휴게소 위치 API의 영업부대시설코드 |

#### 응답

성공 시:

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "oilCompany": "AD",
    "gasolinePrice": "1,999원",
    "dieselPrice": "1,997원",
    "lpgPrice": "1,157원",
    "telNo": "02-573-7430",
    "lastRefreshedAt": "2026-06-16T07:30:00",
    "oilStationConveniences": [
      {
        "startTime": "00:00",
        "endTime": "24:00",
        "name": "쉼터",
        "description": "고객쉼터"
      }
    ]
  }
}
```

갱신 대상 휴게소, 주유소 매핑 또는 upstream 단건 결과가 없으면 `NOT_FOUND`를 반환한다.
최근 10분 이내 저장값이 있으면 `SUCCESS`를 반환하며, 응답 형태는 upstream 호출 성공 시와 같다.
`lastRefreshedAt`은 DB에 저장된 주유소 가격의 마지막 갱신 시각이며, 값이 없으면 `null`이다.

### GET /api/place-search

카카오 키워드 검색 결과 중 좌표를 해석할 수 있는 후보를 반환한다. 화면은 첫 번째 결과를 자동 선택하지 않고
후보의 이름과 주소를 보여준 뒤 사용자가 목적지를 선택하게 한다.

| 위치 | 이름 | 조건 | 설명 |
|---|---|---|---|
| Query | `query` | 필수 | 검색할 장소명 또는 주소 문자열 |

각 후보는 `name`, `address`, `latitude`, `longitude`를 포함한다. 검색 결과가 없으면 빈 배열을 반환한다.

### GET /api/route-rest-stops

출발 좌표와 목적지를 이용해 카카오 자동차 경로를 조회하고, 경로 반경 안에 있는 휴게소를 진행 순서대로 반환한다.

| 위치 | 이름 | 조건 | 설명 |
|---|---|---|---|
| Query | `originLat` | 필수 | 출발 위도 |
| Query | `originLng` | 필수 | 출발 경도 |
| Query | `destinationQuery` | 선택 | 목적지 검색어 |
| Query | `destinationLat` | 선택 | 사용자가 선택한 목적지 위도 |
| Query | `destinationLng` | 선택 | 사용자가 선택한 목적지 경도 |
| Query | `destinationName` | 선택 | 화면에 표시할 목적지명 |
| Query | `radiusMeters` | 선택 | 경로 포함 반경, 기본값 `1000` |

목적지 좌표가 있으면 검색 없이 해당 좌표를 사용한다. 좌표가 없으면 `destinationQuery`로 검색한 첫 결과를
사용하는 하위 호환 경로가 남아 있다. 현재 화면은 `/api/place-search`에서 사용자가 고른 목적지 좌표를 전달한다.

응답 `data`는 다음 영역으로 구성된다.

- `destination`: 목적지 이름과 좌표
- `route`: 전체 거리(m), 예상 시간(초)과 다운샘플링한 `[경도, 위도]` 경로 좌표
- `nationalOilPriceSummary`: 경로 결과 상단에 표시할 전국 평균 유가 요약. 오피넷 호출 실패 또는 필수 제품 누락 시 `null`
- `restStops`: 휴게소 코드, 이름, 노선, 좌표, 경로로부터의 거리(m), 방향 후보 여부, 비교 요약과 추천 태그

`nationalOilPriceSummary`는 오피넷 전국 평균가 중 화면에서 사용하는 3개 제품만 포함한다.

```json
{
  "tradeDate": "2026.07.07",
  "gasoline": {
    "productCode": "B027",
    "productName": "휘발유",
    "price": "1,893원",
    "dailyDiff": "-4.19"
  },
  "diesel": {
    "productCode": "D047",
    "productName": "자동차용경유",
    "price": "1,880원",
    "dailyDiff": "-4.51"
  },
  "lpg": {
    "productCode": "K015",
    "productName": "자동차용부탄",
    "price": "1,135원",
    "dailyDiff": "+0.01"
  }
}
```

`restStops[].comparisonSummary`는 기존 저장 데이터에서 조합한 화면용 요약이다.

```json
{
  "gasolinePrice": "1,650원",
  "dieselPrice": "1,550원",
  "lpgPrice": "1,100원",
  "gasolinePriceDiffFromAverage": -243,
  "dieselPriceDiffFromAverage": -330,
  "lpgPriceDiffFromAverage": -35,
  "totalParkingCount": 63,
  "foodMenuCount": 2,
  "facilityCount": 3
}
```

`*PriceDiffFromAverage`는 휴게소 가격에서 전국 평균가를 뺀 원 단위 정수다. 음수면 전국 평균보다 저렴하고,
양수면 전국 평균보다 비싸다. 휴게소 가격 또는 평균가를 숫자로 해석할 수 없으면 `null`이다.

`restStops[].recommendationTags`는 경로 결과 안에서 상대 비교하거나 정보 보유 여부를 기준으로 부여한 판단용
뱃지 목록이다.

| key | label | 기준 |
|---|---|---|
| `lowest-gasoline` | `휘발유 최저가` | 경로 결과 안에서 휘발유 가격 최저 |
| `lowest-diesel` | `경유 최저가` | 경로 결과 안에서 경유 가격 최저 |
| `lowest-lpg` | `LPG 최저가` | 경로 결과 안에서 LPG 가격 최저 |
| `largest-parking` | `주차장 큼` | 소형·대형·장애인 주차 대수 합산이 경로 결과 안에서 최대 |
| `food-available` | `먹거리 있음` | 음식 메뉴가 1개 이상 |
| `many-facilities` | `시설 많음` | 시설 개수가 3개 이상 |

가격은 숫자로 해석할 수 있는 저장값만 비교하고, 주차 수는 해석 가능한 숫자만 합산한다.

`restStops[].nearbyTraffic`은 그 휴게소와 가장 가까운 경로 구간의 실시간 소통 상태다(`null`이면 정보 없음).
카카오 길찾기 API를 `road_details=true`로 호출해 매 요청마다 새로 계산할 뿐 DB에 저장하지 않는다 — 휴게소 자체가
붐빈다는 뜻이 아니라 그 근처 도로가 지금 어떤 상태인지를 나타낸다.

| key | label | 기준(traffic_state) |
|---|---|---|
| `smooth` | `원활` | `4` |
| `slow` | `서행` | `3` |
| `jam` | `정체` | `1`, `2` |
| `accident` | `사고` | `6` |

`traffic_state`가 `0`(정보없음)이거나 위 표에 없는 값이면 `nearbyTraffic`은 `null`이다.

요청 파라미터와 에러 응답 형식은 변경되지 않는다.

목적지나 경로를 찾지 못하면 HTTP 404와 `NOT_FOUND`를 반환한다. 카카오 API 호출 자체가 실패하면
`EXTERNAL_API_UNAVAILABLE`을 반환한다.

---

## 다음 API를 추가할 때 기록할 것

새 공공 API를 연결할 때는 추정값이 아니라 실제 호출 결과 기준으로 아래 항목을 남긴다.

- 엔드포인트
- 요청 파라미터
- 응답 최상위 필드
- list 필드명
- 성공 코드
- 페이지네이션 방식
- 실제 샘플 응답

성공 코드와 응답 필드명은 API마다 다를 수 있으므로 기존 API 값을 그대로 재사용하지 않는다.

---

# 카카오 API 연동 (경로 탐색)

한국도로공사 OpenAPI와 별개 제공자(Kakao). 경로 기반 휴게소 탐색용. 인증은 헤더 `Authorization: KakaoAK {REST_API_KEY}`. 키는 서버 환경변수로만 주입하고 소스/커밋에 넣지 않는다.

## 자동차 길찾기 — Directions (카카오모빌리티)

### 엔드포인트
```
GET https://apis-navi.kakaomobility.com/v1/directions
```

### 요청 파라미터 (2026-06-17 실측)
- `origin` (필수): `경도,위도` (예: `127.039,37.484`)
- `destination` (필수): `경도,위도`
- `priority` (선택): `RECOMMEND` | `TIME` | `DISTANCE`

### 응답 (2026-06-17 실측)
- 최상위: `trans_id`, `routes`
- `routes[0].result_code` = `0` 이 성공(`result_msg`="길찾기 성공"). 실패 시 0 외 코드.
- `routes[0].summary.distance`(m), `summary.duration`(초), `summary.origin/destination`
- `routes[0].sections[].roads[].vertexes` = **[경도,위도,경도,위도,...] 평탄 배열**(경로 폴리라인). 서울→부산 실측 시 약 3,012개 좌표.

### 실측 샘플 (서울→부산)
```
HTTP 200, result_code 0, distance 382296m, duration 16572s, sections 1, path points ~3012
vertexes 예: [127.03902,37.48391, 127.03877,37.48387, ...]
```

## 장소 검색 — Local (Kakao Developers)

콘솔에서 카카오맵(OPEN_MAP_AND_LOCAL) 서비스 활성화 필요(무료). 미활성 시 403 `NotAuthorizedError`.

### 엔드포인트 (2026-06-17 실측)
```
GET https://dapi.kakao.com/v2/local/search/keyword.json?query={장소}   # 장소명
```

현재 코드는 키워드 검색 endpoint만 호출한다. 주소 검색 endpoint는 연결하지 않았다.

### 응답 (2026-06-17 실측)
- `documents[]` 배열. 각 항목 `x`(경도, 문자열), `y`(위도, 문자열), `place_name`(키워드) / `address_name`·`road_address_name`(주소).
- 실측: `해운대해수욕장`(keyword) → x=129.1598, y=35.1585.

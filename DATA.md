# 데이터 방향

이 문서는 외부 데이터의 저장 방식, 테이블 관계와 연결 키를 기록한다.
외부 API별 요청과 응답 실측 명세는 `API.md`에서 관리한다.

관련 설계 문서:

- `DATA_RELATIONSHIP_DIAGRAM.md`: 현재 DB 테이블 관계와 조회 조합 기준
- `DATA_LOOKUP_KEY_DESIGN.md`: `rest_stop.service_area_code` 기반 내부 조회 키 설계

## 현재 상태

- 한국도로공사 OpenAPI 응답을 API별 Entity와 테이블에 저장한다.
- 호출 결과 코드, 메시지, 페이지 번호 같은 전송 메타데이터는 영속 데이터에서 제외한다.
- 여러 테이블의 데이터는 조회 시 Repository에서 가져와 응답 DTO에서 조합한다.

## 합의된 목표

- 외부 API의 원본 필드를 가능한 한 유지해 API 응답과 저장 데이터를 쉽게 비교한다.
- 화면 요구에 맞춰 원본 테이블을 성급하게 하나의 내부 모델로 통합하지 않는다.
- 새로운 외부 API는 독립된 응답 객체, Entity와 테이블로 저장한다.
- 문자열 숫자와 `O/X` 같은 원본 표현은 특별한 이유가 없다면 저장 단계에서 임의 변환하지 않는다.
- 합산, null 처리와 화면용 표현 변환은 조회 응답 DTO가 담당한다.
- 데이터 연결은 실제 응답으로 검증된 식별자만 사용한다.

## 데이터 연결 기록

새 API를 추가하거나 조회 데이터를 조합할 때 아래 형식으로 연결 관계를 기록한다.

| 기준 데이터 | 연결 데이터 | 연결 조건 | 검증 상태 |
|---|---|---|---|
| `rest_stop` | `rest_stop_detail` | `service_area_code = service_area_code` | 코드 적용, 실측 기록 필요 |
| `rest_stop` | `highway_service_area_info` | `service_area_code = business_facility_code` | 코드 적용, 실측 기록 필요 |
| `rest_stop` | `rest_oil` | `route_no = route_code` + 시설명에서 `휴게소`/`주유소`와 공백을 제거한 정규화 이름 일치 | 2026-06-15 실측 |
| `rest_oil` | `rest_oil_price` | `standard_rest_code = service_area_code2` | 2026-06-16 실측 |
| `rest_stop` | `rest_food` | `std_rest_cd = std_rest_cd` | 2026-06-16 실측 |
| `rest_stop` | `rest_stop_image` | `service_area_code = service_area_code` (휴게소 1건당 이미지 0 또는 1건) | 코드 적용 |
| `rest_stop` | `rest_theme` | `std_rest_cd = std_rest_cd` (휴게소 1건당 테마 0건 이상) | 2026-07-28 실측 |
| `rest_stop` | `rest_event` | `std_rest_cd = std_rest_cd` (휴게소 1건당 이벤트 0건 이상) | 2026-07-28 실측 |

`rest_stop.std_rest_cd`와 `rest_oil.standard_rest_code`는 같은 장소의 휴게소와 주유소에도
서로 다른 시설 코드가 발급된다. 서울만남(부산)의 경우 각각 `000001`, `000002`이며,
실측한 휴게소 203건과 주유소 데이터 사이에 동일 코드 매칭은 0건이었다.

`rest_oil`은 동기화 시 `normalized_station_name`을 계산해 저장하고
`(route_code, normalized_station_name)` 일반 복합 인덱스로 상세 조회한다.
한 주유소에 여러 편의시설 행이 존재하므로 이 인덱스는 unique가 아니다.

`rest_oil_price.service_area_code2`는 주유소 코드이며 `rest_oil.standard_rest_code`와 연결된다.
서울만남(부산)주유소는 두 API에서 모두 `000002`로 내려오는 것을 확인했다.
가격 데이터는 상세 조회마다 외부 API를 호출하지 않고 3시간마다 `curStateStation` 1~3페이지를
동기화해 저장한다.

휴게소 상세 화면은 하나의 통합 응답에 모든 데이터를 넣지 않는다. 기본 정보는 `rest_stop`과
`rest_stop_detail`을 조합한 `basic-info` 응답으로 제공하고, 영업시설·주차 정보는
`facilities` 응답으로 제공한다. 주유 정보는 `rest_oil`에서 조회한 첫 번째
`standard_rest_code`로 `rest_oil_price`를 조회해 `oil-info` 응답으로 제공한다.
`oilInfo.oilStationConveniences`는 같은 `rest_oil` 조회 결과를 변환하며, 가격 데이터가 없어도
`oil-info` 응답과 편의시설 배열은 유지한다. 먹거리 정보는 `rest_food`를 `foods` 응답으로
변환한다. 프론트엔드는 이 feature 응답들을 병렬로 조회하며, 기본 정보가 있어야 상세 화면을
표시하고 나머지 응답이 없거나 외부 API를 사용할 수 없을 때는 해당 영역만 빈 상태로 처리한다.

단건 가격 갱신 API도 같은 연결 키를 사용하며, upstream에서 단건 결과가 내려오면
`rest_oil_price.service_area_code2` 기준으로 update 또는 insert한다.
`rest_oil_price.last_refreshed_at`은 가격 row가 마지막으로 갱신된 시각이다. 단건 가격 갱신 API는
이 값이 현재 시각 기준 10분 이내면 외부 API 호출 없이 저장값을 반환한다.

오피넷 전국 평균 유가는 `national_oil_price` 테이블에 일별·유종별로 저장한다.
연결 키는 `trade_date + product_code`이며, 같은 거래일의 데이터는 재조회 시 교체 저장한다.
경로 결과 조회에서 오늘 평균가가 없으면 오피넷 `avgAllPrice.do`를 호출하고, 필수 유종
휘발유(`B027`), 자동차용경유(`D047`), 자동차용부탄(`K015`)이 모두 있을 때만
`/api/national-oil-prices/summary`에서 전국 평균 유가 요약을 제공한다. 경로 응답은
`destination`과 `routes`(대안 경로 포함 배열, 각 원소가 `summary`/`restStops`를 가짐)를
반환하며, `restStops`의 `comparisonSummary`에는 전국 평균 대비 휴게소별 차이값이 포함될 수
있다. 전국 평균 유가 요약 자체는 경로 응답의 최상위 필드로 포함하지 않는다. 오피넷 평균가는
휴게소 상세 응답에도 연결하지 않는다.

휴게소 음식 메뉴(`restBestfoodList`) 연결은 2026-06-16 실측에서 `restBestfoodList.stdRestCd`가
`rest_stop.std_rest_cd`와 직접 일치함을 확인했다. 서울만남(부산)휴게소가 양쪽 모두 `000001`이며,
주유소(`rest_oil`)와 달리 정규화 이름 매칭 없이 `std_rest_cd`로 바로 조인한다.
음식 API 전용 코드 `restCd`(`S000001`)는 연결에 사용하지 않는다.

테마휴게소(`restThemeList`)·휴게소 이벤트(`restEventList`) 연결은 `rest_food`와 동일하게
`std_rest_cd`로 바로 조인하며 정규화 이름 매칭이 필요 없다. 한 휴게소에 테마·이벤트가
여러 건 붙을 수 있어(2026-07-28 실측: 서울만남(부산)휴게소에 이벤트 3건) 동기화는
`std_rest_cd` + 부속 식별자(테마는 `item_nm`, 이벤트는 `event_seq`) 복합 자연키로 upsert한다.
이벤트는 상당수가 오래전에 종료됐지만 삭제하지 않고 원본 기간(`stime`~`etime`)을 그대로
보존하며, 조회 시점에 오늘 날짜가 그 사이인 것만 걸러 노출한다(이 필터링은 아직
구현하지 않았고 프론트엔드 연동 단계에서 추가한다).

## 대표 이미지

`rest_stop_image`는 관리자 등록 휴게소 대표 이미지를 저장하는 테이블이다. 정확히
`service_area_code`, `detail_image_data`, `list_image_data` 세 컬럼으로 구성하며,
`service_area_code`는 `rest_stop.service_area_code`와 연결된다. 휴게소 하나에는 이미지 행이
0개 또는 1개만 존재한다.

업로드한 JPEG 또는 PNG는 원본을 보관하지 않고 WebP 두 변형으로 변환해 저장한다. 상세용은 긴 변을
최대 1600px, 목록용은 긴 변을 최대 480px로 제한한다. 이번 약 200장 범위에서는 두 WebP를 MySQL
`MEDIUMBLOB`으로 DB에 저장해 운영하며, 원본 보관이나 더 큰 이미지 범위는 이번 범위 밖이다.

이미지 존재 여부는 기존 JSON 응답의 nullable URL로 전달한다. `basic-info`의
`detailImageUrl`과 경로 휴게소 항목의 `listImageUrl`은 이미지가 없으면 `null`이다. 실제 BLOB은
별도 공개 바이너리 API에서만 반환하므로 목록 조회가 BLOB을 읽지 않는다.

이미지가 있는 휴게소의 상세·목록 바이너리는 각각
`GET /api/rest-stops/{serviceAreaCode}/images/detail`와
`GET /api/rest-stops/{serviceAreaCode}/images/list`에서 `image/webp`로 반환한다. 존재하는 휴게소에
이미지가 없으면 이 API는 `204 No Content`를 반환하고, 휴게소 자체가 없으면 `404`를 반환한다. 성공
응답은 이미지 데이터 기반 ETag와 `Cache-Control: public, no-cache`를 제공해 브라우저가 재검증할 수
있다.

한국도로공사, 카카오, 오피넷, 환경공단 등 외부 API의 요청·응답과 동기화 정책은 이 이미지 기능으로
변경하지 않는다.

`rest_food_image`는 관리자가 메뉴별로 등록하는 이미지를 저장하는 테이블이다. `rest_stop_image`와
컬럼 구성(`detail_image_data`/`list_image_data` MEDIUMBLOB, 동일한 WebP 변환 규칙)은 같지만 키가
`service_area_code`가 아니라 `food_id`(`rest_food.id`)다 — 휴게소 1건당 이미지는 0~1개이지만
메뉴는 휴게소 1건당 여러 개일 수 있어 메뉴 단위 키가 필요하기 때문이다. 메뉴 하나에는 이미지 행이
0개 또는 1개만 존재한다. 이번 범위는 관리자 화면에서 등록·삭제하는 데까지만 구현하며, 사용자 화면에
노출하는 공개 조회 API(`GET .../images/detail|list`에 대응하는 메뉴 버전)는 아직 만들지 않는다 —
표시 여부·크기·위치가 정해지지 않았기 때문이다.

## 관리자 활동 로그

`admin_activity_log`는 외부 API 데이터가 아니라 관리자 콘솔에서 발생한 쓰기 작업을 기록하는 내부 전용
테이블이다(그래서 위 "데이터 연결 기록" 표 형식 대신 별도 문단으로 기록한다). `id`, `actor`(로그인
계정명), `message`(사람이 읽는 완성된 한 줄 문장), `created_at` 네 컬럼만 가진다. 구조화된 액션
타입이나 대상 필드는 두지 않고, 저장 시점에 이미 완성된 문장을 그대로 저장한다 — 화면에 그대로
보여줄 뿐 다른 방식으로 재가공하거나 집계할 계획이 없기 때문이다. 삭제·보관 정책은 없고 계속
누적되며, 필요해지면 별도 과제로 다룬다.

`rest_stop_detail`과 `highway_service_area_info` 연결은 현재 상세 조회에 적용되어 있지만,
대표 표본과 전체 일치율을 확인한 실측 기록은 아직 없다. 연결 조건을 변경하기 전에 실제 응답으로 검증한다.

환경공단 전기자동차 충전소 정보는 `ev_charger`에 원본 필드를 저장한다. `stat_id + chger_id`를
자연키로 사용하며, `del_yn = 'N'`인 충전기만 active 충전기로 취급한다. `kind_detail = 'C001'`인
고속도로 휴게소 충전기만 저장 대상이다.

`ev_charger_station_mapping`은 충전소 단위 식별자인 `stat_id`와 내부 휴게소 코드인
`rest_stop_service_area_code`를 연결한다. 매핑은 충전소 좌표와 휴게소 좌표가 300m 이내이고,
휴게소 이름 또는 연결된 `rest_stop_detail.svar_addr` 주소가 일치할 때 생성한다. 같은 `stat_id`의
여러 충전기는 하나의 매핑만 만든다. 좌표·이름·주소 조건을 만족하지 않으면 매핑하지 않는다.

경로 응답의 `hasEvCharger`는 충전기 원본 테이블을 다시 세지 않고 매핑 테이블의 휴게소 코드
존재 여부로 판단한다. 휴게소 상세 응답의 `evChargerCount`는 매핑된 `stat_id`를 기준으로
`del_yn = 'N'`인 `chger_id` 개수를 계산한다. EV 전체 페이지 동기화가 실패하면 기존 충전기와
기존 매핑을 유지한다. 일부 페이지가 실패한 경우에는 성공한 페이지의 충전기만 upsert한 뒤
현재 DB 전체를 기준으로 매핑 backfill을 수행한다. 모든 페이지가 실패하거나 성공한 C001 데이터가
없으면 충전기와 매핑을 변경하지 않는다.

경로 탐색 결과는 별도 테이블에 저장하지 않는다. 저장된 `rest_stop` 좌표를 카카오 길찾기 경로와
비교하고, 기본 반경 1km 안의 휴게소를 경로 순서대로 계산해 응답한다.

## 갱신 원칙

- 정기 갱신 데이터는 API별 동기화 책임을 분리한다.
- 외부 API 호출과 DB 쓰기 트랜잭션을 한 범위에 묶지 않는다.
- 전체 교체, 부분 갱신과 실패 시 기존 데이터 보존 여부는 API별 스펙에서 결정한다.
- 휴게소 위치·상세·영업시설·주유소 편의시설·먹거리는 자연키 기준 upsert(있으면 갱신, 없으면 삽입)로 동기화하며, 모든 페이지 조회가 끝난 뒤 트랜잭션 안에서 처리한다. 외부 응답에서 사라진 행은 삭제하지 않는다(삭제 대응은 별도 후속 과제).
- 주유 가격(3시간 주기)만 모든 페이지 조회가 끝난 뒤 트랜잭션 안에서 기존 데이터를 전체 교체한다.
- 휴게소 위치·상세·영업시설·주유소 편의시설·먹거리는 매일, 주유 가격은 3시간마다 갱신한다.
- 서버 시작 시 주요 테이블이 비어 있을 때만 초기 동기화를 실행한다.
- EV 충전기 정보는 서버 시작 시 테이블이 비어 있을 때 초기화하고 매일 자정에 동기화한다.
- `rest_stop`/`rest_stop_detail`에 `admin_overridden`(boolean, 기본값 false) 컬럼을 둔다. `@Column(nullable = false, columnDefinition = "boolean default false")`로 선언해, 기존 행이 있는 테이블에 `ddl-auto=update`로 컬럼을 추가할 때도(`ALTER TABLE ... ADD COLUMN`) 기존 행이 `false`로 채워져 `NOT NULL` 제약 위반 없이 반영된다. 관리자가 `PUT /api/admin/rest-stops/{serviceAreaCode}/editable`로 편집하면 두 테이블 모두 이 값이 true가 되고, 자연키 upsert 동기화는 이 값이 true인 행을 건너뛴다(자동 갱신에서 제외). `DELETE .../editable/override`로 다시 false가 되면 다음 동기화부터 정상 갱신된다. `data.ex.co.kr` API 자체에 없는 휴게소는 `POST /api/admin/rest-stops`로 관리자가 새로 등록하며, `rest_food`의 `"ADMIN-" + UUID` 발급 방식과 동일하게 `rest_stop.service_area_code`/`unit_code`/`std_rest_cd`를 모두 `"ADMIN-" + UUID`로 채워 실제 API 자연키와 겹치지 않게 하고 생성 시점부터 `admin_overridden=true`로 시작한다.
- `rest_stop_detail`은 `rest_stop`과 별도 외부 API(위치정보/편의시설)로 동기화되어 시점이 어긋나면 `rest_stop`만 있고 `rest_stop_detail`이 없는 행이 생길 수 있다. 관리자 편집 API는 이 경우 `serviceAreaCode`만 채운 `rest_stop_detail` 행을 새로 만들어 저장한다(추후 편의시설 API가 같은 자연키로 동기화되어도 `admin_overridden=true`라 덮어쓰지 않는다).
- `rest_food`에도 동일한 이유로 `admin_overridden`(boolean, `@Column(nullable = false, columnDefinition = "boolean default false")`) 컬럼을 둔다. 관리자가 기존 동기화 메뉴 값을 고치면(`PUT /api/admin/rest-stops/{serviceAreaCode}/foods/{foodId}`) 이 값이 true가 되고, `restBestfoodList` 동기화의 `(std_rest_cd, seq)` upsert는 이 값이 true인 행을 건너뛴다. `DELETE .../foods/{foodId}/override`로 다시 false가 되면 다음 동기화부터 정상 갱신된다. 관리자가 새로 추가하는 메뉴는 외부 API와 겹치지 않도록 `seq`를 `"ADMIN-" + UUID`로 발급하고 생성 시점부터 `admin_overridden=true`로 시작한다 — 동기화 루프는 외부 응답에 실제로 존재하는 `seq`만 순회하므로 이 행은 애초에 그 루프 대상이 되지 않지만, 플래그도 이중으로 true로 맞춰둔다. 동기화는 사라진 행을 삭제하지 않는 원칙이라 동기화 메뉴 자체는 관리자가 삭제할 수 없고(삭제해도 다음 동기화에 다시 생성됨), 관리자가 직접 추가한 메뉴(`seq`가 `ADMIN-`으로 시작)만 삭제 가능하다.
- `rest_oil`과 `rest_oil_price`에도 동일한 이유로 각각 `admin_overridden`(boolean, `@Column(nullable = false, columnDefinition = "boolean default false")`) 컬럼을 둔다. `rest_stop`↔`rest_oil` 연결은 `route_no = route_code` + 정규화 이름 일치라는 fuzzy 매칭이고, `rest_stop`↔`rest_oil_price` 연결은 이렇게 채워진 `rest_oil.rest_stop_service_area_code`를 `rest_oil.standard_rest_code = rest_oil_price.service_area_code2`로 한 단계 더 거쳐 파생된다. 두 매칭 모두 `RestStopServiceAreaCodeBackfillService.backfillRestOils()`/`backfillRestOilPrices()`가 매일 자정·3시간마다·서버 시작 시 다시 계산해 덮어쓴다. `rest_oil`은 물리적 주유소 하나당 부대 편의시설(세차장·쉼터 등) 개수만큼 행이 여러 개 생기는 반면, `rest_oil_price`는 실제 사용자에게 보이는 업체·가격 정보가 담긴 테이블로 휴게소당 정확히 0~1건이다. 그래서 관리자 화면은 `rest_oil_price`를 기준으로 목록·검색·비교(주유소명·노선명·주소·방향)를 보여주고, `PUT /api/admin/oil-stations/{oilId}/link`(연결)·`DELETE .../link`(해제)·`DELETE .../override`(잠금 해제)는 대상 `rest_oil_price` 행뿐 아니라 같은 물리적 주유소(자연키 `standard_rest_code`/`service_area_code2`)를 공유하는 `rest_oil` 행 전체에도 동일한 조작을 함께 반영해 두 테이블이 항상 같은 휴게소를 가리키게 한다.

## 미결정 사항

- 외부 API별 데이터 보존 기간
- API 간 식별자가 일치하지 않을 때 사용할 별도 매핑 테이블
- 원본 응답 자체를 별도 보관할 필요가 있는지 여부

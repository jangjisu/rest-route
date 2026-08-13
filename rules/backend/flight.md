# 항공권 참조 데이터(국가/도시/공항/항공사) 소스와 재생성 방법

`flight_country`/`flight_city`/`flight_airport`/`flight_airline` 4개 테이블을 어떻게
채웠는지, 왜 런타임에 외부 API를 직접 호출하지 않는지 기록한다. 이 데이터를 다시
채워야 할 일이 생기면(신규 코드 추가, 커버리지 재검증 등) 이 문서를 먼저 읽는다.

## 왜 라이브 API 호출이 아니라 SQL 시딩인가

이 4개 테이블은 자주 바뀌지 않는 참조 데이터다(국가/공항 코드 체계가 매일 바뀌지
않는다). 그래서 배포 때마다 외부 API를 다시 부르는 대신, 미리 만들어둔
`src/main/resources/data/flight-*-seed.sql`을 앱 시작 시 읽어서 넣는 방식을 쓴다.
`FlightReferenceDataSeeder`가 매 시작마다 기존 데이터를 지우고 이 파일을 다시
읽어 넣는다(자세한 이유는 `FlightReferenceDataSeeder`의 클래스 docstring 참고 —
과거 "비어있을 때만 시딩" 방식이 스키마 마이그레이션과 겹쳐 프로덕션에 null 값이
남는 사고를 낸 적이 있다).

이 SQL 파일을 만드는 데 쓰인 외부 API 클라이언트(`IncheonClient`,
`TravelpayoutsClient.citiesData/countriesData/airlinesData`)는 런타임에서 더 이상
쓰이지 않아 코드베이스에서 삭제했다. 아래는 그 클라이언트들이 존재했을 때 어떻게
데이터를 모았는지, 그리고 앞으로 다시 모아야 할 때 어떤 순서로 하면 되는지에
대한 기록이다.

## 데이터 소스

| 테이블 | 1차 소스 | 보강 소스 | 커버리지 |
|---|---|---|---|
| `flight_country` | Travelpayouts `/data/ko/countries.json` | - | korName/engName 253개국 전량 |
| `flight_city` | Travelpayouts `/data/ko/cities.json` (취항 공항이 있는 도시만) | Wikidata SPARQL, 수동 조사 | korName/engName 전량 |
| `flight_airport` | Travelpayouts `/data/ko/airports.json` | Wikidata SPARQL(P238), 수동 조사 | korName/engName 전량 |
| `flight_airline` | 인천국제공항공사 취항 항공사 현황 API(data.go.kr, `StatusOfSrvAirlines`) | Travelpayouts `/data/airlines.json`, Wikidata SPARQL(P229), 수동 조사 | korName/engName 전량 |

- Travelpayouts `/ko/` 로케일 엔드포인트는 원본부터 한글 이름을 갖고 있지만,
  국가를 제외하면 커버리지가 100%가 아니었다(도시 약 81%, 공항 약 29%).
- 인천공항 API는 실제 취항 확인된 항공사만 다루기 때문에 전체 IATA 항공사 코드의
  일부(약 9%)만 커버한다. 코드가 겹치면 인천공항 쪽 한글명을 우선하고, 없으면
  Travelpayouts 영문명만 채운 뒤 나머지 소스로 보강한다.
- Wikidata SPARQL은 IATA 코드를 값으로 갖는 속성(공항 P238, 항공사 P229)으로
  엔티티를 찾아 한국어 label(`rdfs:label` + `@ko` 필터)을 가져오는 방식이다.
  이걸로도 못 채운 나머지는 각 코드를 하나씩 검색해서 수동으로 한글 표기를
  채워 넣었다(공식 한글 표기가 없는 소도시/소형 항공사는 관용적 음차 표기를 썼다).

## SQL 시드 파일을 다시 만들어야 할 때

새 국가/도시/공항/항공사가 추가되어 시드를 갱신해야 하면 다음 순서를 따른다.

1. Travelpayouts 소스 데이터를 다시 받는다 — `https://api.travelpayouts.com/data/ko/countries.json`,
   `/data/ko/cities.json`, `/data/ko/airports.json`, `https://api.travelpayouts.com/data/airlines.json`
   (인증 불필요, 정적 파일).
2. 항공사는 인천공항 API(`https://apis.data.go.kr/B551177/StatusOfSrvAirlines`,
   서비스키 필요 — data.go.kr 포털에서 발급)로 취항 확인 항공사의 한글명을 받아
   코드 기준으로 병합한다.
3. 위 두 소스만으로 korName이 비어있는 코드 목록을 뽑아, Wikidata SPARQL
   엔드포인트(`https://query.wikidata.org/sparql`)에 IATA 코드로 질의해 보강한다.
4. 그래도 남는 코드는 각각 검색해서 수동으로 채운다(이번 작업에서 도시 587개,
   항공사 484개, 공항 1928개를 이 방식으로 채워 4개 테이블 모두 100% 커버리지를
   달성했다).
5. 최종 병합 결과를 `INSERT INTO ... VALUES (...);` 문 목록으로 만들어
   `src/main/resources/data/flight-{country,city,airline,airport}-seed.sql`을
   덮어쓴다. 컬럼 순서는 각 Entity의 생성자 인자 순서와 맞춘다.
6. 로컬에서 앱을 재시작해 `FlightReferenceDataSeeder`가 재시딩하는지, 각
   `*NameCache`가 정상적으로 채워지는지 확인한다.

이 과정을 자동화하는 스크립트는 아직 없다 — 매번 최신 스냅샷을 받아 수작업으로
병합/검증하면서 진행했다. 반복 빈도가 높아지면 스크립트화를 고려한다.

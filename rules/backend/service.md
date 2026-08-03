# Service 수정 시 확인 규칙

## 패키지 depth — 관리자 전용 Service는 `admin/` 서브패키지

관리자 전용 Service(관리자 Controller에서만 호출되는 Service·예외)는
`service/admin/`에 둔다. 일반 조회/동기화 Service와 이름 접두사(`Admin*`)만으로
구분되는 채 같은 패키지에 flat하게 섞이지 않게 한다.

- 여러 도메인 Service가 공용으로 쓰는 클래스(예: `RestStopImageProcessor`처럼
  admin/non-admin 양쪽에서 호출되는 순수 변환기, `RestStopNotFoundException`처럼
  양쪽에서 던지는 예외)는 admin 전용이 아니므로 그 도메인 패키지(`service/image/` 등)에
  그대로 둔다. 어느 한쪽에서만 쓰는 클래스만 옮긴다 — 옮기기 전에 실제 사용처를
  `grep -rl "import com.restroute.service.xxx.ClassName;"`로 확인한다.
- `controller.md`의 같은 규칙과 대응한다 — Controller가 `admin/`으로 옮겨지면
  그 Controller가 전용으로 쓰는 Service도 함께 검토한다.

## 패키지 depth — DTO/예외/util은 `dto/`·`exception/`·`util/` 서브패키지

Service 패키지(`service/` 자체나 `service/route/` 같은 도메인 서브패키지) 안에
Service 클래스와 데이터 클래스(record/enum)·예외·순수 헬퍼가 같은 depth로 섞이면,
그 패키지를 열었을 때 어떤 게 실제 Service(진입점)이고 어떤 게 보조 클래스인지
파일 목록만 보고 구분이 안 된다. 다음 기준으로 나눈다:

- **`dto/`**: record, enum처럼 상태를 담는 데이터 클래스. 파생값 계산용 메서드가
  있어도(예: `RoutePolyline.nearest()`) 그 클래스 자체가 "값"을 표현하면 `dto/`.
- **`exception/`**: 그 도메인에서 던지는 예외.
- **`util/`**: `@Component`/`@Service` 여부와 무관하게, 이름이 Parser/Processor/
  Calculator/Matcher/Normalizer/Format처럼 "변환·계산 한 가지만 하는" 순수 헬퍼.
  Spring 빈으로 등록됐다고 해서 무조건 도메인 루트에 남기지 않는다 — 판단 기준은
  빈 여부가 아니라 "이게 이 도메인의 주요 진입점인가, 보조 도구인가"다.
- 도메인 루트에는 실제 오케스트레이션을 담당하는 `@Service`/`@Component` 클래스만
  남긴다.

이동 시 확인할 것(실제로 겪은 함정들):
- **package-private 클래스는 서브패키지로 옮기면 접근이 막힌다.** Java는 하위
  패키지를 상위 패키지의 연장으로 보지 않는다 — `service.route`와
  `service.route.dto`는 완전히 별개 패키지다. 옮기는 클래스와 그 정적 팩토리
  메서드(`of`/`from`)·접근자가 다른 패키지에서 쓰인다면 `public`으로 바꿔야 한다.
- **같은 패키지라 import 없이 쓰던 참조가 전부 깨진다.** 컴파일 에러를 기준으로
  하나씩 import를 추가하기보다, `./gradlew compileJava`를 반복 실행하며 고치는 게
  빠르다 — 대량 이동 시 수십 개 파일이 한 번에 걸린다.
- **패키지 소속을 문자열로 단언하는 테스트**(`XxxException.class.getPackageName()`
  같은)가 있는지 미리 검색한다(`grep -rln "getPackageName()" src/test`) — 있다면
  새 패키지로 값을 갱신해야 한다.
- 규칙을 적용하되, **어느 한쪽에서만 쓰는지 먼저 확인**한다(`admin/` 규칙과 동일한
  이유) — 여러 Service가 공용으로 쓰는 데이터 클래스를 한쪽 도메인의 `dto/`에
  넣으면 반대쪽이 그 도메인을 참조하게 돼 레이어 방향이 꼬인다.

## 레이어 경계

- Service A는 Service B 소유의 Repository에 직접 접근하지 않는다
- 두 Service가 서로를 참조하는 순환 의존은 금지
- Service 간 호출은 한 방향으로만 흐른다 (A → B 허용, A ↔ B 금지)

## @Transactional 범위

`@Transactional` 범위 안에서 외부 API 호출·파일 I/O 등 느린 작업을 포함하지 않는다.

- fetch (외부 API 호출) → 트랜잭션 밖
- DB 저장/수정 → 트랜잭션 안

```java
// ✅ 올바른 분리
public void refresh() {
    List<XxxItem> items = fetchFromApi();  // 트랜잭션 밖
    save(items);                           // 트랜잭션 안
}

@Transactional
void save(List<XxxItem> items) {
    repository.deleteAll();
    repository.saveAll(...);
}
```

## 즉시 계산 가능한 값은 생성 시점에 포함

불변 응답 객체에 `with*()` 류의 단계적 적용 메서드를 쓰는 경우, 그 메서드는 "전체 후보 집합에 대한 배치 조회가 끝나야 알 수 있는 값"에만 써야 한다. 개별 항목 생성 시점에 이미 알 수 있는 값(같은 반복문 안에서 계산되는 값 등)을 굳이 raw 필드로 다음 단계까지 들고 갔다가 나중에 `with*()`로 재조합하지 않는다 — 생성 시점에 바로 계산해서 넣는다.

```java
// ❌ trafficState를 raw Integer로 candidate에 얹었다가 한참 뒤 재조합
RouteRestStopItem item = RouteRestStopItem.of(..., distance);
Integer trafficState = polyline.coordinates().get(nearest.index()).trafficState();
candidates.add(RouteRestStopCandidate.of(restStop, item, index, trafficState));
// ... 수십 줄 뒤
.withNearbyTraffic(nearbyTraffic(comparison.candidate().trafficState()))

// ✅ 같은 시점에 알 수 있는 값이므로 바로 계산해서 item에 포함
RouteRestStopItem item = RouteRestStopItem.of(..., distance)
        .withNearbyTraffic(nearbyTraffic(polyline.coordinates().get(nearest.index()).trafficState()));
candidates.add(RouteRestStopCandidate.of(restStop, item, index));
```

## 이미 존재하는 조회 서비스 재사용

같은 Entity의 `repository.findAll()`(또는 동일한 조건의 조회)을 여러 Service가 각자 독립적으로 호출하고 있다면, 이미 그 조회를 감싸는 Service(예: `RestStopQueryService.findAll()`)가 있는지 먼저 확인하고 재사용한다. 없는 조회 방식(예: 코드 목록으로 필터링하는 버전)을 미리 만들어두지는 않는다 — 실제로 그 형태의 소비자가 생겼을 때 추가한다(YAGNI).

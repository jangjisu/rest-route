# Service 수정 시 확인 규칙

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

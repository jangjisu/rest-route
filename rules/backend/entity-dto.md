# Entity / DTO 수정 시 확인 규칙

## Entity

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "...")
public class XxxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ...

    public static XxxEntity from(XxxItem item) { ... }
}
```

- 외부 API Item → Entity 변환은 `from()` 정적 팩토리로 처리한다
- 서비스에서 빌더를 직접 호출하지 않는다 (테스트 데이터 구성 목적은 허용)

## DTO

- 외부 API 응답 클래스(VO)와 내부 DTO는 반드시 분리한다
- 내부 DTO 필드명에 외부 API 약어가 그대로 남아 있으면 안 된다

```java
// ❌ API 원시 필드명 그대로 복사
private final String sphlDfttNm;

// ✅ from() 에서 이름 변환
public static XxxDto from(XxxEntity entity) {
    return XxxDto.builder()
            .dayType(entity.getSphlDfttNm())
            .build();
}
```

## API 응답 → 내부 객체 변환 규칙

- 변환 로직은 `from()` / `of()` 내부에서 처리한다
- 컬렉션 수준 계산(rank, 집계 합산 등)은 서비스에서 계산 후 파라미터로 전달한다
- 서비스는 "언제 변환할지"와 "컬렉션 수준 계산"만 담당한다

## 실패/부정 조건 predicate 명명

VO/DTO의 `boolean` predicate가 컨트롤 플로우의 실패·에러 분기 조건으로 쓰인다면, 호출부에서 `!x.isY()`/`!x.hasY()`로 부정하지 말고 VO 안에 실패 쪽을 직접 이름 붙인 predicate를 추가한다.

```java
// ❌ 호출부에서 부정 — "성공"의 정의가 나중에 바뀌면 호출부를 다 찾아 고쳐야 함
if (!directions.hasSuccessfulRoute()) {
    throw new RouteRestStopNotFoundException(...);
}

// ✅ VO 안에 실패 조건을 직접 명명 — 정의 변경이 VO 내부 한 곳으로 갇힘
public boolean failedToRoute() {
    return !hasSuccessfulRoute();
}
if (directions.failedToRoute()) {
    throw new RouteRestStopNotFoundException(...);
}
```

`isEmpty()`, `isSuccess()`, `isAdminOverridden()`처럼 표준 관용구이거나 단발성 조건 체크인 predicate까지 전부 이렇게 뒤집을 필요는 없다. 이 규칙은 "성공/실패"처럼 도메인적으로 의미 있는 이분법이고, 실패 분기(예외 처리, early return)에서 반복적으로 참조되는 predicate에만 적용한다.

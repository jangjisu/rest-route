# 도메인 DTO는 `<domain>/dto`, 응답 전용 모양은 `<domain>/controller/response`에 둔다

`FuelType`/`FuelTypeSelection`/`NationalOilPriceSummary`/`AverageOilPrice`가 `route` 패키지(`route/dto`, `route/controller/response/RouteRestStopResponse` 내부 중첩 record)에 있었는데, 실제로는 `oilprice`가 만들고 `reststop`도 쓰는 타입이었다 — route를 import해야만 컴파일되는 역방향 의존이 생겨 있었다.

옮기면서 두 위치를 저울질했다: 기존 관례인 `<domain>/service/dto`(서비스 전용이라는 이름이 여러 계층이 같이 쓴다는 사실과 안 맞음)와, 아예 새 공용 kernel 패키지(지금 문제 규모 대비 과함). 대신 이렇게 정한다:

- **Service와 Controller가 함께 쓰는 도메인 개념** → `<domain>/dto` (예: `oilprice.dto.FuelType`)
- **Controller 응답에만 쓰이고 서비스가 값 자체를 다루지 않는 순수 응답 모양** → `<domain>/controller/response`

근거: `NationalOilPriceSummary`는 실제로 JSON 응답에 한 번도 실린 적이 없는데도(`RouteRestStopController` 테스트가 `nationalOilPriceSummary` 필드의 부재를 명시적으로 검증한다) route의 `controller.response` 안에 갇혀 있었다 — response 패키지에 서비스 전용 값을 두면 소유권이 흐려진다.

이 구분을 앞으로 새 도메인 DTO를 놓을 때의 기준으로 쓴다. 기존에 `<domain>/service/dto`에 있는 다른 타입들을 지금 다 옮기지는 않는다 — 새로 만들거나 다시 손대는 타입부터 이 기준을 적용한다.

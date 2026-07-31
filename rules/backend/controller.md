# Controller 수정 시 확인 규칙

## ApiResponse 구조

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": { ... }
}
```

## ResponseCode 정의

| code | HTTP Status | 의미 |
|------|-------------|------|
| `SUCCESS` | 200 | 정상 처리 |
| `INVALID_PARAMETER` | 400 | 요청 파라미터 검증 실패 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `EXTERNAL_API_UNAVAILABLE` | 200 | upstream API 일시 불가, 서버 자체는 정상 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

> `EXTERNAL_API_UNAVAILABLE` 은 HTTP 200을 반환한다.
> 서버는 정상이고, 데이터 조회 실패는 `code` 필드로 표현한다.

## GlobalExceptionHandler

`@RestControllerAdvice` (GlobalExceptionHandler) 가 모든 미처리 예외를 잡아 `INTERNAL_ERROR` 로 응답한다.

## 새 엔드포인트의 파라미터 문서화

`@RequestParam`이 3개 이상이거나 이름만으로 형식을 알기 어려운 파라미터(좌표, 코드값 등)가 있는 새 엔드포인트를 추가할 때는, 메서드 위에 Javadoc으로 각 파라미터의 의미와 예시값을 남긴다. springdoc/swagger는 이 프로젝트에 아직 없으므로 Javadoc으로 대체한다(의존성 도입은 별도 논의).

```java
/**
 * @param originLat 출발지 위도. 예: 37.5665
 * @param destinationQuery 목적지 검색어(좌표 없이 이름/주소로 지오코딩할 때). 예: "부산역"
 * @param radiusMeters 경로에서 휴게소를 포함할 반경(m). 예: 1000
 */
@GetMapping
public ResponseEntity<ApiResponse<XxxResponse>> getXxx(
        @RequestParam double originLat,
        @RequestParam(required = false) String destinationQuery,
        @RequestParam(required = false, defaultValue = "1000") int radiusMeters) { ... }
```

기존 엔드포인트를 소급 적용할 필요는 없다 — 새로 추가하거나 파라미터를 변경하는 시점에만 적용한다.

## deprecated API 체크 공백

`config/checkstyle/checkstyle.xml`은 `UnusedImports`만 검사하고, harness `04-run-code-quality-tools.sh`의 SonarQube 게이트는 `SONAR_TOKEN`/`SONAR_HOST_URL`/`sonar-project.properties` 중 하나가 있어야 실행되는데 로컬 개발 환경엔 셋 다 없어 항상 스킵된다. 즉 IDE(Qodana 등) 인스펙션에서만 deprecated API 사용이 보이고, 빌드/커밋 게이트에서는 잡히지 않는다.

`build.gradle`의 `JavaCompile`에 `-Xlint:deprecation`을 걸어뒀으므로 `./gradlew compileJava`(테스트 실행 시 항상 같이 돔) 로그에서 경고를 확인할 수 있다. SonarQube 서버 연결(SONAR_TOKEN 발급 등)은 외부 계정이 필요해 별도로 논의해야 한다.

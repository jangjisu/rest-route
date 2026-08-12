# Controller 수정 시 확인 규칙

## 패키지 depth — 관리자 전용 Controller는 `admin/` 서브패키지

관리자 전용(`@PreAuthorize`/관리자 세션 필요, URL이 `/admin/**`인) Controller는
`controller/admin/`에 둔다. 일반 사용자용 Controller와 이름 접두사(`Admin*`)만으로
구분되는 채 같은 패키지에 flat하게 섞이지 않게 한다 — 파일 목록만 보고 어디까지가
공개 API인지 즉시 구분되어야 한다.

새 Controller를 추가할 때:

- 관리자 전용이면 `com.restroute.controller.admin` 패키지에 만든다.
- 공개(비관리자) API면 기존대로 `com.restroute.controller`에 만든다.
- `request`/`response` DTO는 이번 규칙 대상이 아니다(현재 범위 밖, 필요해지면 별도 논의).

## ApiResponse 구조

컨트롤러는 항상 `ResponseEntity<XxxResponse<T>>` 형태로 응답한다 — 여기서 `XxxResponse`는
"이 도메인에 정의된 응답 봉투 타입"을 뜻하며, 기본값은 공용 `ApiResponse`다.

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": { ... }
}
```

도메인이 공용 `ApiResponse`와 다른 자기 응답 봉투를 별도로 정의했다면(예:
`flight.controller.response.FlightApiResponse`의 `{data, error}` 구조), 그 도메인
컨트롤러는 공용 `ApiResponse` 대신 그 타입을 따른다. 새 봉투 타입을 도입할 때는 그 도메인
패키지 안에 정의하고, 왜 공용 `ApiResponse`를 안 쓰는지 클래스 Javadoc에 남긴다.

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

`@RequestParam`이 있는 새 엔드포인트를 추가하거나 기존 엔드포인트의 파라미터를 변경할 때는, 파라미터가 하나뿐이어도 메서드 위에 Javadoc으로 각 파라미터의 의미와 예시값을 남긴다. 부분 일치/정확히 일치처럼 검색 동작이 이름만으로 드러나지 않는 경우 그 동작도 함께 적는다. springdoc/swagger는 이 프로젝트에 아직 없으므로 Javadoc으로 대체한다(의존성 도입은 별도 논의).

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

`build.gradle`의 `JavaCompile`에 `-Xlint:deprecation`을 걸어뒀으므로 `./gradlew compileJava`(테스트 실행 시 항상 같이 돔) 로그에서 경고를 확인할 수 있다. 단 `-Xlint:deprecation`은 javac lint 카테고리 중 `deprecation` 하나만 켠다 — `unchecked`/`rawtypes`/`cast` 등 나머지 카테고리는 별개로 켜야 하고(`-Xlint:all`), **IDE에서 보이는 "null이 될 수 있다" 경고는 애초에 javac lint 대상이 아니다**(IntelliJ/Qodana 자체 데이터플로우 분석). 이 프로젝트에서 null 안전성을 빌드 게이트로 강제하려면 SonarQube 서버 연결(SonarJava 룰셋에 null-dereference 룰 포함, `SONAR_TOKEN` 발급 등 외부 계정 필요) 또는 NullAway/SpotBugs 같은 별도 정적분석 도구 도입이 필요하며, 둘 다 이 프로젝트 규모의 별도 논의가 필요한 결정이라 "deprecated 체크 추가" 작업에 끼워 넣지 않는다.

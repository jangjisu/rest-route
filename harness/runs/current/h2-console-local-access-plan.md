# 로컬 H2 Console 접근 계획

## 범위

- 현재 관리자 인증 브랜치에서 Java/backend와 보안 테스트만 수정한다.
- `spring.h2.console.enabled=true`일 때만 `/h2-console/**`를 공개한다.
- local H2 Console에만 same-origin frame 정책과 CSRF 예외를 적용한다.
- H2 Console이 비활성화된 프로필에서는 `/h2-console/**`를 명시적으로 거부한다.
- 관리자 경로와 기존 공개 API 규칙은 유지한다.
- 문서, 관리자 기능, 프론트엔드, push는 변경하지 않는다.

## 스펙 영향도 확인

- Endpoint: local에서 `/h2-console/**`가 공개되고, 비활성 프로필에서는 거부된다.
- Request: H2 Console POST 요청은 local에서만 CSRF 검사를 예외 처리한다.
- Response: local 응답은 `X-Frame-Options: SAMEORIGIN`을 사용하며, 비활성 프로필 접근은 허용하지 않는다.
- Error: 관리자 경로의 인증·인가와 기존 공개 API 오류 흐름은 변경하지 않는다.
- Data model: 변경 없음.

## 접근 방법 비교와 선택 이유

- 선택: H2 Console enabled 프로퍼티를 SecurityFilterChain에 주입해 동일 보안 설정 안에서 조건부로 처리한다. 프로필별 보안 규칙을 중복 작성하지 않는다.
- 제외: local 전용 SecurityFilterChain을 별도로 추가한다. 필터 체인 우선순위와 공개 경로 중복으로 설정 복잡도가 커진다.
- 선택: enabled=false일 때 `/h2-console/**`를 denyAll한다. 현재의 `anyRequest().permitAll()`이 콘솔 경로를 우회 공개하지 않도록 한다.

## 검증

- local enabled 설정에서 H2 Console 접근, same-origin header, CSRF 예외를 검증한다.
- prod enabled=false 설정과 H2 Console 명시적 거부를 검증한다.
- 관리자 인증·인가와 기존 공개 API 회귀 테스트를 실행한다.

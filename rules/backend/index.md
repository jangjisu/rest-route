# 백엔드 규칙 인덱스

Java 파일 수정 시 수정 대상에 해당하는 파일만 읽는다.

기계적으로 검증 가능한 항목은 `harness/hooks/code` 와 `harness/hooks/verify` 가 판단한다.
이 문서는 레이어별 설계 판단이 필요할 때만 사용한다.

**Entity 또는 DTO 클래스를 추가하거나 수정하는 경우**
→ `entity-dto.md` 를 읽어라

**Service 클래스를 추가하거나 수정하는 경우**
→ `service.md` 를 읽어라

**새 API 흐름을 설계하거나, Service 메서드가 길어져서 나눌지 판단해야 하는 경우**
→ `module-design.md` 를 읽어라

**Controller 클래스를 추가하거나 수정하는 경우**
→ `controller.md` 를 읽어라

**PMD/SpotBugs 관련 작업(잔여 findings 처리, 규칙 조정 등)을 하는 경우**
→ `static-analysis.md` 를 읽어라

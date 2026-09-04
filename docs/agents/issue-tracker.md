# 이슈 트래커: GitHub

이 리포의 이슈와 스펙은 GitHub 이슈로 존재한다. 모든 작업에 `gh` CLI를 쓴다.

## 관례

- **이슈 생성**: `gh issue create --title "..." --body "..."`. 본문이 여러 줄이면 heredoc을 쓴다.
- **이슈 조회**: `gh issue view <번호> --comments`, 코멘트는 `jq`로 걸러보고 라벨도 같이 가져온다.
- **이슈 목록**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`에 필요한 `--label`/`--state` 필터를 붙인다.
- **이슈에 코멘트**: `gh issue comment <번호> --body "..."`
- **라벨 적용/제거**: `gh issue edit <번호> --add-label "..."` / `--remove-label "..."`
- **닫기**: `gh issue close <번호> --comment "..."`

리포는 `git remote -v`로 추론한다 — 클론 디렉터리 안에서 실행하면 `gh`가 자동으로 처리해준다.

## PR을 triage 대상으로 볼지

**요청 표면으로서의 PR: 아니오.** _(외부 PR도 기능 요청으로 취급하려면 `yes`로 바꾼다; `/triage`가 이 플래그를 읽는다.)_

`yes`로 설정하면 PR도 이슈와 같은 라벨·상태를 거치며, `gh pr` 대응 명령을 쓴다:

- **PR 조회**: `gh pr view <번호> --comments`, diff는 `gh pr diff <번호>`.
- **트리아지 대상 외부 PR 목록**: `gh pr list --state open --json number,title,body,labels,author,authorAssociation,comments`에서 `authorAssociation`이 `CONTRIBUTOR`/`FIRST_TIME_CONTRIBUTOR`/`NONE`인 것만 남기고(`OWNER`/`MEMBER`/`COLLABORATOR`는 제외) 쓴다.
- **코멘트/라벨/닫기**: `gh pr comment`, `gh pr edit --add-label`/`--remove-label`, `gh pr close`.

GitHub는 이슈와 PR이 번호 공간을 공유해서, `#42`만 봐서는 어느 쪽인지 알 수 없다 — `gh pr view 42`를 먼저 시도하고 안 되면 `gh issue view 42`로 폴백한다.

## 스킬이 "이슈 트래커에 발행하라"고 하면

GitHub 이슈를 만든다.

## 스킬이 "해당 티켓을 가져오라"고 하면

`gh issue view <번호> --comments`를 실행한다.

## Wayfinding 작업

`/wayfinder`가 사용한다. **맵(map)**은 **자식(child)** 이슈들을 티켓으로 갖는 이슈 하나다.

- **맵**: `wayfinder:map` 라벨이 붙은 이슈 하나, Notes/Decisions-so-far/Fog 본문을 담는다. `gh issue create --label wayfinder:map`.
- **자식 티켓**: 맵에 GitHub sub-issue로 연결된 이슈(`gh api`의 sub-issues 엔드포인트 사용). sub-issue가 활성화 안 돼있으면, 맵 본문의 태스크 목록에 자식을 추가하고 자식 본문 맨 위에 `Part of #<map>`을 적는다. 라벨은 `wayfinder:<type>`(`research`/`prototype`/`grilling`/`task`). 맡아지면(claimed) 담당 개발자에게 배정된다.
- **Blocking**: GitHub의 **네이티브 이슈 의존성**이 UI에서 보이는 정식 표현이다. `gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`로 엣지를 추가한다. 여기서 `<blocker-db-id>`는 블로커의 숫자 **데이터베이스 id**(`gh api repos/<owner>/<repo>/issues/<n> --jq .id`이지 `#번호`나 `node_id`가 아니다)다. GitHub는 `issue_dependencies_summary.blocked_by`로 열려있는 블로커만(살아있는 게이트) 보고한다. dependencies 기능을 못 쓰면 자식 본문 맨 위 `Blocked by: #<n>, #<n>` 줄로 대체한다. 블로커가 전부 닫히면 그 티켓은 block이 풀린다.
- **Frontier 조회**: 맵의 열린 자식들을 나열하고(`gh issue list --state open`, 맵의 sub-issue/태스크 목록 범위로 한정), 열린 블로커가 있거나(`issue_dependencies_summary.blocked_by > 0`, 또는 `Blocked by` 줄에 열린 이슈가 있음) 담당자가 이미 있는 건 제외한다 — 맵 순서상 첫 번째가 이긴다.
- **Claim**: `gh issue edit <n> --add-assignee @me`, 세션의 첫 쓰기 작업.
- **Resolve**: `gh issue comment <n> --body "<답변>"` 다음 `gh issue close <n>`, 그리고 맵의 Decisions-so-far에 컨텍스트 포인터(gist + 링크)를 덧붙인다.

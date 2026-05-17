# 변경 이력

easy-paging의 모든 주요 변경사항이 여기 기록됩니다. 포맷은 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) 기반이며, 프로젝트는 [Semantic Versioning](https://semver.org/)을 따릅니다.

원본 머신 판독용 파일은 레포의 [`CHANGELOG.md`](https://github.com/devslab-kr/easy-paging-spring-boot-starter/blob/main/CHANGELOG.md)를 참조.

---

## [0.2.0] — 2026-05-18

### 추가

- **`easy-paging.one-indexed-pages` 설정 옵션** (기본 `false`). `true`로 설정 시 요청과 응답 모두 1-based 페이지 번호 사용 — `?page=1`이 첫 페이지, 응답에도 `"page": 1`. 전체 동작은 [정렬 & 페이지 번호](guides/sorting.md#클라이언트에-1-based-페이지-번호-노출) 참조.
- `PageResponse.withOneIndexedPages()` — aspect가 호출하는 순수 변환 메서드. 수동으로 `PageResponse`를 만들 때 직접 사용도 가능.
- **API (Javadoc) 레퍼런스 페이지** — javadoc.io 링크와 주요 public 타입 빠른 지도.

### 변경

- *정렬 & 페이지 번호* 가이드가 옵션 사용법을 설명 (이전엔 "예정" 으로만 표기).
- *설정* 레퍼런스에 `one-indexed-pages` 추가.

### 메모

- 하위 호환 — 기본 동작은 0-based, v0.1.x와 동일. 기존 사용자는 opt-in 불필요.

---

## [0.1.2] — 2026-05-17

### 추가

- **문서 사이트** [easy-paging.devslab.kr](https://easy-paging.devslab.kr/) — MkDocs Material로 빌드, 14 페이지 × 2 언어, `main` 푸시마다 자동 배포.
- 양쪽 README에 문서 사이트 링크 추가.

### 변경

- POM `<url>`이 GitHub 레포가 아닌 이 문서 사이트를 가리키도록 변경. Maven Central listing의 "Project" 링크가 첫 방문자를 문서 사이트로 안내.

### 메모

- 기능 변경 없음. 의존성 도구의 새 POM URL이 필요한 경우가 아니면 업그레이드 불필요.

---

## [0.1.1] — 2026-05-15

### 추가

- **잘못된 sort에 대한 HTTP 400** — `?sort=name;DROP TABLE`은 이제 generic `500`이 아닌 `400 Bad Request`로 거부됩니다. Aspect가 `SortConverter`의 `IllegalArgumentException`을 `ResponseStatusException(BAD_REQUEST)`로 래핑.
- **JaCoCo 커버리지** — `./gradlew jacocoTestReport`로 XML + HTML 리포트 생성. CI가 `main` 푸시마다 Codecov 업로드.
- 양쪽 README에 **커버리지 + CI 배지** 추가.
- **새 통합 테스트**:
    - `AutoPaginateWebMvcIntegrationTest` — 전체 Spring MVC 스택을 통한 aspect 검증 (happy path, 다중 컬럼 정렬, 인젝션 거부, ThreadLocal 정리, 사이즈 클램핑)
    - `PageResponseFactoryIntegrationTest` — 등록된 `PageResponseFactory` 빈이 기본 봉투를 대체함을 검증

### 변경

- README (양 언어) 가 **0-based** 페이지 번호 컨벤션과 `mybatis-spring-boot-starter`가 transitive가 **아님**을 명확히 함 (사용자가 명시적으로 추가).
- Aspect 정리가 일원화됨: `SortConverter` 실패와 매퍼 예외 둘 다 단일 `finally`에서 PageHelper `ThreadLocal` 해제.
- 테스트 fixture `data.sql`이 identity 시퀀스를 리셋 (`TRUNCATE … RESTART IDENTITY`) — 테스트 실행 중 반복된 컨텍스트 시작이 deterministic한 ID를 보도록.

### 커버리지

- 라인: 86 % · 브랜치: 71 % · 43개 테스트 (단위 + 통합)

---

## [0.1.0] — 2026-05-12

### 추가

- Spring Data `Pageable` 기반 offset 페이지네이션을 위한 `@AutoPaginate` aspect, PageHelper 백엔드. 페이지 크기 클램핑, 정렬 전파, `finally`에서 PageHelper `ThreadLocal` 정리.
- `PageResponse<T>` 봉투 (Spring Data 형태, Jackson 친화적).
- 엄격한 식별자 화이트리스트를 가진 `SortConverter` (`sort` 쿼리 파라미터를 통한 SQL 인젝션 거부).
- 커서 기반 페이지네이션을 위한 `@KeysetPaginate` + `KeysetRequest` 인자 해결자 + `KeysetPage<T>` 봉투.
- `Cursor` / `CursorCodec` — Base64-URL JSON 토큰, 선택적 HMAC-SHA256 서명, 페이로드 크기 상한, 위조 감지.
- 블로킹-IO 스케줄러 위에서 Reactor 코드로부터 PageHelper를 사용하기 위한 `ReactivePagingSupport`.
- 기본 응답 봉투를 오버라이드하기 위한 `PageResponseFactory` SPI.
- Spring Boot 자동 설정:
    - `EasyPagingAutoConfiguration` (코어)
    - `EasyPagingWebMvcConfiguration` (servlet — 인자 해결자)
    - `ReactiveEasyPagingAutoConfiguration` (reactor 마커)
- IDE 자동완성을 위한 `easy-paging.*` 속성의 configuration metadata.
- CI 및 Maven Central 릴리즈용 GitHub Actions 워크플로우.

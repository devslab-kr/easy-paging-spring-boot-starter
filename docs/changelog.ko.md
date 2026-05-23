# 변경 이력

easy-paging의 모든 주요 변경사항이 여기 기록됩니다. 포맷은 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) 기반이며, 프로젝트는 [Semantic Versioning](https://semver.org/)을 따릅니다.

원본 머신 판독용 파일은 레포의 [`CHANGELOG.md`](https://github.com/devslab-kr/easy-paging-spring-boot-starter/blob/main/CHANGELOG.md)를 참조.

---

## [3.0.0] — 2026-05-23

**`0.4.0`의 재번호링** — 새 [Spring-major-정렬 버전 정책](https://github.com/devslab-kr/.github/blob/main/.github/VERSIONING.md#한국어)에 따름. API / 동작 / 의존성 변경 전혀 없음 — 메이저 숫자를 `0.4` → `3.0`으로 올려서 이 maintenance 라인의 타겟 Spring Boot 메이저 (Spring Boot 3)와 일치시킴. 발행된 JAR 바이트는 `0.4.0`과 동일 (POM의 버전 좌표만 다름).

앞으로 SB3 maintenance 라인은 `3.x.y` 라인. SB4 active 라인도 같은 웨이브에서 `4.x.y`로 재번호링. 기존 `0.4.0` 아티팩트는 historical reference로 Maven Central에 잔존.

### `0.4.0`에서 올라오기

```diff
- implementation("kr.devslab:easy-paging-spring-boot-starter:0.4.0")
+ implementation("kr.devslab:easy-paging-spring-boot-starter:3.0.0")
- implementation("kr.devslab:easy-paging-spring-boot-starter-reactive:0.4.0")
+ implementation("kr.devslab:easy-paging-spring-boot-starter-reactive:3.0.0")
```

다른 변경 없음. 이 라인은 Spring Boot 3이 upstream에서 지원받는 동안 보안 패치를 계속 제공.

---

## [0.4.0] — 2026-05-20

### 추가

- **새 옵션 아티팩트 `easy-paging-spring-boot-starter-reactive`** — 네이티브 R2DBC + WebFlux 지원, core 스타터의 자매 모듈. 기존 MyBatis 사용자는 추가 작업 없음 — 순수 additive. 세 가지 제공:
    - `R2dbcOffsetPagingSupport.paginate(template, entity, criteria, pageable)` — 페이지 행 + count 쿼리를 `Mono.zip`으로 병렬화, MyBatis 쪽과 동일한 `PageResponse` 봉투.
    - `R2dbcKeysetSupport.paginate(template, entity, baseFilter, keys, request, keyExtractor, codec)` — R2DBC keyset/커서 페이지네이션. `Instant`, `LocalDateTime`, `OffsetDateTime`, `LocalDate`, `UUID`, primitive wrapper에 대한 내장 타입 강제 변환.
    - `ReactiveKeysetRequestArgumentResolver` — servlet `KeysetRequestArgumentResolver`의 WebFlux 버전. WebFlux 클래스패스 발견 시 자동 등록.
- `PageResponse.of(rows, pageable, total)` — total을 이미 아는 경우(R2DBC의 일반적 패턴)를 위한 새 팩토리. 기존 `PageResponse.from(list, pageable)`(PageHelper 래핑된 list에서 total 추론)과 상보.
- **Dialect-compat 테스트 레이어** — 새 `./gradlew testDialect` task가 `@Tag("dialect-compat")` 태그된 테스트들을 Testcontainers로 실제 PostgreSQL + MySQL 컨테이너에 대해 실행 (기존 H2 fast path와 별도). PageHelper의 dialect-rewriting 경로와 R2DBC driver-specific 타입 바인딩 동작 검증.

### 변경

- **Gradle 빌드가 멀티-모듈 구조로 마이그레이션** (`core/` + `reactive/`). 게시된 `kr.devslab:easy-paging-spring-boot-starter` 좌표는 byte-identical 유지; R2DBC 스택이 필요한 사용자만 새 `…-reactive` 라인 추가.
- 멀티-모듈 레이아웃에 맞춰 CI/릴리스 워크플로우 경로 갱신; 릴리스 asset glob을 `**/build/libs/*.jar`로 변경해 미래 모듈 jar 자동 첨부.

### 노트

- 기존 v0.3 사용자에게는 엄격하게 additive — `easy-paging-spring-boot-starter-reactive` 추가는 옵트인이고, core 아티팩트의 좌표와 동작은 그대로.
- 새 reactive auto-configuration들은 `@ConditionalOnClass(ServerWebExchange.class)` 등으로 자체 게이팅되므로 WebFlux/R2DBC가 컨슈머 클래스패스에 없을 때 죽은 코드가 돌지 않음.

## [0.3.0] — 2026-05-18

### 추가

- **Keyset 역방향 지원.** `KeysetPage.prevCursor`가 더 이상 항상 `null`이 아님 — 첫 페이지가 아닌 모든 페이지에서 `BACKWARD` 방향으로 인코딩되어 채워짐. 커서 필드 의미는 **방향-불변**: `nextCursor`는 항상 "더 오래된 항목 로드", `prevCursor`는 항상 "더 새로운 항목 로드". 클라이언트는 스캔 방향을 추적할 필요가 없음. 컨슈머 측 패턴(미러 `findBefore` 매퍼 + `request.direction()` dispatch)은 [양방향 스크롤](guides/keyset.md#양방향-스크롤) 참조.

### 변경

- **Spring Boot 베이스라인 bump 3.3.5 → 3.5.3.** 내부 빌드/테스트 베이스라인만 변경 — Spring 의존성을 `api(...)`로 버전 핀 없이 선언하므로 Spring Boot 3.3+ 사용자는 그대로 동작. 3.3과 3.4는 이번 릴리즈 시점에 OSS 지원이 종료됐고, 3.5만 현재 OSS 지원 라인.
- **PageHelper bump 2.1.0 → 2.1.1.** MyBatis 3.5.19 + PageHelper 엔진 6.1.1 (업스트림 버그 픽스 패치) 적용.
- **`mybatis-spring-boot-starter:3.0.4`이 이제 transitive로 제공** — 컨슈머가 직접 추가하지 않아도 됨. 이전에는 PageHelper의 transitive Spring Boot 2.7 라인 MyBatis starter가 override를 깜빡한 앱에 누출될 수 있었지만, 이제 라이브러리가 Boot 3 호환 버전을 직접 고정. 다른 MyBatis 라인이 필요하면 `exclude(group = "org.mybatis.spring.boot")`로 override.
- 문서: `installation` 페이지 업데이트 (두 의존성 변경 반영); `keyset` 가이드에 "양방향 스크롤" 섹션 추가.

### 노트

- `KeysetPage.build` API 시그니처는 그대로지만 동작이 방향 인지 방식으로 변경. FORWARD 전용 컨슈머는 변화 없음 — 유일한 관찰 가능한 차이는 첫 페이지 이후 페이지의 `prevCursor`가 더 이상 항상 `null`이 아니라는 점.
- Spring Boot 3.5가 가져오는 JUnit Jupiter 5.11+는 Gradle 8.10.x 프로젝트에서 명시적 `junit-platform-launcher` 선언이 필요. 이 라이브러리의 빌드 한정 이슈로 컨슈머에는 노출되지 않음.

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

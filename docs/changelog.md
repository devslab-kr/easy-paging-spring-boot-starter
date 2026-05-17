# Changelog

All notable changes to easy-paging are documented here. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows [Semantic Versioning](https://semver.org/).

For the canonical, machine-readable source see [`CHANGELOG.md`](https://github.com/devslab-kr/easy-paging-spring-boot-starter/blob/main/CHANGELOG.md) in the repository.

---

## [0.3.0] — 2026-05-18

### Added

- **Keyset reverse direction.** `KeysetPage.prevCursor` is no longer always `null` — it's populated whenever a page that isn't the first is returned, encoded with `BACKWARD` direction. Cursor field semantics are **direction-invariant**: `nextCursor` always means "load older items", `prevCursor` always means "load newer items". The client never has to track which way they're scanning. See [Bidirectional scrolling](guides/keyset.md#bidirectional-scrolling) for the consumer-side pattern (mirror `findBefore` mapper + `request.direction()` dispatch).

### Changed

- **Spring Boot baseline bumped 3.3.5 → 3.5.3.** Internal build/test baseline only; consumers on Spring Boot 3.3+ continue to work because our Spring deps are declared via `api(...)` without version pinning. 3.3 and 3.4 reached OSS end-of-life before this release, so 3.5 is the only currently OSS-supported line.
- **PageHelper bumped 2.1.0 → 2.1.1.** Picks up MyBatis 3.5.19 + PageHelper engine 6.1.1 (upstream bug-fix patches).
- **`mybatis-spring-boot-starter:3.0.4` is now provided transitively** — consumers no longer have to add it themselves. Previously, PageHelper's transitive Spring Boot 2.7 line MyBatis starter could leak into apps that forgot to override it; now the library pins the Boot 3-compatible version directly. Override via `exclude(group = "org.mybatis.spring.boot")` if you need a different MyBatis line.
- Docs: `installation` pages updated to reflect both dependency changes; `keyset` guide gains the "Bidirectional scrolling" section.

### Notes

- `KeysetPage.build` API signature is unchanged, but its behavior is now direction-aware. Forward-only consumers see no change; the only observable difference is that `prevCursor` is no longer always `null` on pages past the first.
- JUnit Jupiter 5.11+ (shipped by Spring Boot 3.5) needs an explicit `junit-platform-launcher` declaration in projects on Gradle 8.10.x. Build-only concern for this library; not exposed to consumers.

## [0.2.0] — 2026-05-18

### Added

- **`easy-paging.one-indexed-pages` configuration option** (default `false`). When `true`, page numbers are 1-based on both request and response — `?page=1` is the first page, response shows `"page": 1`. See [Sorting & Page Numbering](guides/sorting.md#exposing-1-based-page-numbers-to-clients) for the full contract.
- `PageResponse.withOneIndexedPages()` — pure transform invoked by the aspect; also usable directly when constructing `PageResponse` manually.
- **API (Javadoc) reference page** linking to javadoc.io with a quick map of the main public types.

### Changed

- *Sorting & Page Numbering* guide now documents the new option instead of describing it as a future plan.
- *Configuration* reference lists `one-indexed-pages` alongside the other tunables.

### Notes

- Backward compatible — default behavior is 0-based, identical to v0.1.x. Existing consumers do not need to opt in.

---

## [0.1.2] — 2026-05-17

### Added

- This **documentation site** at [easy-paging.devslab.kr](https://easy-paging.devslab.kr/) — built with MkDocs Material, 14 pages × 2 languages, auto-deployed on every push to `main`.
- README link to the docs site (both languages).

### Changed

- POM `<url>` now points here (the docs site) instead of the GitHub repository. The "Project" link on Maven Central listings will lead first-time visitors here.

### Notes

- No functional changes. Consumers do not need to upgrade unless they want the new POM URL in their dependency tooling.

---

## [0.1.1] — 2026-05-15

### Added

- **HTTP 400 for invalid sort** — `?sort=name;DROP TABLE` is now rejected with `400 Bad Request` instead of bubbling up as a generic `500`. The aspect wraps `IllegalArgumentException` from `SortConverter` in `ResponseStatusException(BAD_REQUEST)`.
- **JaCoCo coverage** — `./gradlew jacocoTestReport` emits XML + HTML reports. CI uploads to Codecov on every push to `main`.
- **Coverage and CI badges** in both READMEs.
- **New integration tests**:
    - `AutoPaginateWebMvcIntegrationTest` — exercises the aspect through the full Spring MVC stack (happy path, multi-column sort, injection rejection, ThreadLocal cleanup, oversize clamping).
    - `PageResponseFactoryIntegrationTest` — verifies that a registered `PageResponseFactory` bean replaces the default envelope.

### Changed

- README (both languages) clarifies the **0-based** page numbering convention and the fact that `mybatis-spring-boot-starter` is **not** transitive (consumers add it explicitly).
- Aspect cleanup is unified: both `SortConverter` failures and mapper exceptions release the PageHelper `ThreadLocal` through a single `finally`.
- Test fixture `data.sql` resets the identity sequence (`TRUNCATE … RESTART IDENTITY`) so repeated context starts during a test run see deterministic IDs.

### Coverage

- Line: 86 % · Branch: 71 % · 43 tests across unit + integration.

---

## [0.1.0] — 2026-05-12

### Added

- `@AutoPaginate` aspect for offset pagination driven by Spring Data `Pageable`, backed by PageHelper. Clamps page size, propagates sort, cleans up the PageHelper `ThreadLocal` in `finally`.
- `PageResponse<T>` envelope (Spring Data-shaped, Jackson-friendly).
- `SortConverter` with a strict identifier whitelist (rejects SQL injection via the `sort` query parameter).
- `@KeysetPaginate` + `KeysetRequest` argument resolver + `KeysetPage<T>` envelope for cursor-based pagination.
- `Cursor` / `CursorCodec` — Base64-URL JSON tokens with optional HMAC-SHA256 signing, payload size cap, tamper detection.
- `ReactivePagingSupport` for using PageHelper from Reactor code on a blocking-IO scheduler.
- `PageResponseFactory` SPI for overriding the default response envelope.
- Spring Boot auto-configuration:
    - `EasyPagingAutoConfiguration` (core)
    - `EasyPagingWebMvcConfiguration` (servlet — argument resolver)
    - `ReactiveEasyPagingAutoConfiguration` (reactor marker)
- Configuration metadata for IDE auto-completion of `easy-paging.*` properties.
- GitHub Actions workflows for CI and Maven Central release.

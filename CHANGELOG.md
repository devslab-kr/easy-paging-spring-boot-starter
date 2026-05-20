# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **New optional artifact `easy-paging-spring-boot-starter-reactive`** —
  native R2DBC + WebFlux support, intended for projects on Spring Data
  R2DBC. Existing MyBatis users add nothing; the new starter is purely
  additive. Three pieces ship:
  - `R2dbcOffsetPagingSupport.paginate(template, entity, criteria,
    pageable)` — runs the page-rows query and the count query in
    parallel via `Mono.zip` and produces the same `PageResponse`
    envelope as the MyBatis side.
  - `R2dbcKeysetSupport.paginate(template, entity, baseFilter, keys,
    request, keyExtractor, codec)` — keyset/cursor pagination on
    R2DBC. Builds the lexicographic `WHERE` clause, flips `ORDER BY`
    for backward scans, runs the `size + 1` query, and assembles a
    `KeysetPage`. Built-in type coercion for `Instant`,
    `LocalDateTime`, `OffsetDateTime`, `LocalDate`, `UUID`, primitive
    wrappers, and `String` cursor values.
  - `ReactiveKeysetRequestArgumentResolver` — WebFlux counterpart of
    the servlet `KeysetRequestArgumentResolver`. Auto-registered when
    WebFlux is on the classpath; consumers declare `KeysetRequest`
    parameters on WebFlux handlers without manual wiring.
- `PageResponse.of(rows, pageable, total)` — new core factory for the
  "known total" case (the typical R2DBC pattern). Complements the
  existing `PageResponse.from(list, pageable)` which infers the total
  from a PageHelper-wrapped list. Rejects negative totals to surface
  the "did you mean `from()`?" case loudly.

### Changed
- **Gradle build migrated to multi-module structure.** Source files
  moved from `src/` to `core/src/` (no code changes; rename detection
  preserves history). The published artifact coordinates are
  byte-identical (`kr.devslab:easy-paging-spring-boot-starter`); the
  on-disk jar filename and POM `artifactId` are both pinned explicitly
  via `base.archivesName` and `mavenPublishing.coordinates(...)`. The
  new `reactive/` subproject lives alongside `core/` and publishes
  separately as `kr.devslab:easy-paging-spring-boot-starter-reactive`.
- CI / release workflow paths updated for the new layout: jacoco
  coverage report now resolves under `core/build/...`; release asset
  glob now `**/build/libs/*.jar` so future modules' jars attach
  automatically.

### Notes
- Strictly additive for existing v0.3 consumers — adding the
  `easy-paging-spring-boot-starter-reactive` dependency is opt-in and
  the existing `easy-paging-spring-boot-starter` coordinates and
  behavior are unchanged.

## [0.3.0] - 2026-05-18

### Added
- **Keyset reverse direction is now fully wired up.** `KeysetPage.prevCursor`
  is no longer always `null` — it's populated whenever a page that isn't
  the first is returned, encoded with `BACKWARD` direction so the client
  can blindly use `?cursor=prevCursor` to navigate toward newer items.
  The cursor field semantics are **direction-invariant**: `nextCursor`
  always means "load more in display order" (older items when sorted
  DESC by time), `prevCursor` always means "load newer items". The
  client never has to track which way they're currently scanning — the
  cursor token carries the direction and the resolver decodes it.
- `KeysetPage.build` now handles `BACKWARD` scans correctly: it expects
  the mapper to return rows in reverse display order (e.g. `ORDER BY
  time ASC` when the user view is DESC) and reverses them back to
  display order automatically, so the returned `content` list always
  matches what the user expects to see.
- Consumer-side requirement for backward navigation: write a mirror
  `findBefore` mapper query (flip comparisons, flip `ORDER BY`) and
  dispatch on `request.direction()` in the controller. The keyset
  [guide](https://easy-paging.devslab.kr/guides/keyset/#bidirectional-scrolling)
  shows the full pattern.

### Changed
- **Spring Boot baseline bumped from 3.3.5 to 3.5.3.** Both the
  `org.springframework.boot` Gradle plugin and the
  `spring-boot-dependencies` BOM are now pinned to 3.5.3, which is the
  currently OSS-supported line (3.3 and 3.4 reached OSS end-of-life
  before this release). This is an *internal* build baseline change —
  consumers on Spring Boot 3.3+ continue to work because the library
  declares its Spring dependencies via `api(...)` without version
  pinning, so the consumer's BOM wins at resolution time. Anyone on
  3.5+ gets the freshest transitive dependency graph automatically.
- **PageHelper bumped from 2.1.0 to 2.1.1** — picks up MyBatis 3.5.19
  and PageHelper engine 6.1.1 (both patch-level bug fixes upstream).
- **`mybatis-spring-boot-starter:3.0.4` is now an `api` dependency.**
  Previously consumers had to add it themselves because PageHelper
  2.1.x still ships its own transitive `mybatis-spring-boot-starter:2.3.2`
  (Spring Boot 2.7 line) and we relied on the consumer to override it.
  Now the library declares 3.0.4 directly, so Gradle's conflict
  resolution forces the Boot-3-compatible starter onto every consumer's
  classpath automatically. Consumers who need a different MyBatis line
  can exclude the group and declare their own — see the *Installation*
  guide for the exact snippet.
- Docs: `installation` pages updated to reflect both changes (3.5
  baseline, transitive MyBatis starter).

## [0.2.0] - 2026-05-18

### Added
- **`easy-paging.one-indexed-pages` configuration option** (default `false`).
  When `true`, page numbers are 1-based on both incoming requests and
  outgoing responses: `?page=1` is the first page, and the response's
  `page` field starts at `1`. Internally, Spring's
  `PageableHandlerMethodArgumentResolver` is configured with
  `setOneIndexedParameters(true)` (input half) and the aspect shifts the
  response `page` field by `+1` (output half) — both halves are wired by
  the auto-configuration when the property is on. Keyset endpoints are
  unaffected (cursors don't use page numbers).
- `PageResponse.withOneIndexedPages()` — pure transform that shifts the
  `page` field by `+1`. The aspect calls it; manual callers can use it
  too if they construct `PageResponse` directly.
- Docs site: **API (Javadoc)** reference page linking to javadoc.io for
  the auto-mirrored Javadoc, with a quick-reference table of the main
  public types.

### Changed
- Docs: `Sorting & Page Numbering` no longer says "1-based is planned"
  — it documents the new `one-indexed-pages` option.
- Docs: `Configuration` reference adds the new property with the same
  detail level as the other entries.

### Notes
- Backward compatible: default behavior is unchanged (0-based). Existing
  consumers do not need to opt in.

## [0.1.2] - 2026-05-17

### Added
- **Documentation site** at [easy-paging.devslab.kr](https://easy-paging.devslab.kr/),
  built with MkDocs Material. 14 pages in English and Korean covering
  getting-started, feature guides, configuration reference, changelog,
  and contributing. Auto-deployed via GitHub Actions on every push
  to `main`.
- README badges + prominent docs-site link in both languages.

### Changed
- POM `<url>` now points to the docs site (`https://easy-paging.devslab.kr`)
  instead of the GitHub repository. The repo URL remains in `<scm>` and
  `<issueManagement>`. As a result, the "Project" link on Maven Central
  and mvnrepository.com listings will lead first-time visitors to the
  docs site rather than the source code.

### Notes
- No functional changes to the library itself — this release is
  documentation-only. Consumers do not need to upgrade unless they want
  the new docs link in their dependency tooling.

## [0.1.1] - 2026-05-14

### Added
- HTTP 400 response for SQL-injection-like sort parameters
  (`?sort=name;DROP TABLE`). The aspect now wraps `IllegalArgumentException`
  from `SortConverter` in a `ResponseStatusException(BAD_REQUEST)` so clients
  get a proper status instead of a generic 500.
- JaCoCo coverage reporting (`./gradlew jacocoTestReport`) with XML + HTML
  output. Wired into CI and uploaded to Codecov on every push to `main`.
- Coverage badge + CI badge in both READMEs.
- New integration tests:
  - `AutoPaginateWebMvcIntegrationTest` — exercises the aspect through the
    full Spring MVC stack (5 scenarios: happy path, multi-column sort,
    injection rejection, ThreadLocal cleanup after error, oversize clamping).
  - `PageResponseFactoryIntegrationTest` — verifies that a registered
    `PageResponseFactory` bean replaces the default envelope.

### Changed
- README (English and Korean) clarifies the **0-based** page numbering
  convention and the fact that `mybatis-spring-boot-starter` is **not**
  transitive (consumers add it explicitly, which matches existing
  MyBatis project setups).
- Aspect cleanup is now unified: both `SortConverter` failures and mapper
  exceptions release the PageHelper `ThreadLocal` through a single `finally`
  block.
- Test fixture `data.sql` resets the identity sequence (`TRUNCATE … RESTART
  IDENTITY`) so repeated context starts during a test run see deterministic
  IDs.

### Coverage
- Line: 86 % · Branch: 71 % (43 tests across unit + integration)

## [0.1.0] - 2026-05-12

### Added
- `@AutoPaginate` aspect for offset pagination driven by Spring Data `Pageable`,
  backed by PageHelper. Clamps page size, propagates sort, cleans up the
  PageHelper `ThreadLocal` in `finally`.
- `PageResponse<T>` envelope (Spring Data-shaped, Jackson-friendly).
- `SortConverter` with a strict identifier whitelist (rejects SQL injection via
  the `sort` query parameter).
- `@KeysetPaginate` + `KeysetRequest` argument resolver + `KeysetPage<T>`
  envelope for cursor-based pagination.
- `Cursor` / `CursorCodec` — Base64-URL JSON tokens with optional HMAC-SHA256
  signing, payload size cap, tamper detection.
- `ReactivePagingSupport` for using PageHelper from Reactor code on a
  blocking-IO scheduler.
- `PageResponseFactory` SPI for overriding the default response envelope.
- Spring Boot auto-configuration:
  - `EasyPagingAutoConfiguration` (core)
  - `EasyPagingWebMvcConfiguration` (servlet — argument resolver)
  - `ReactiveEasyPagingAutoConfiguration` (reactor marker)
- Configuration metadata for IDE auto-completion of `easy-paging.*` properties.
- GitHub Actions workflows for CI and Maven Central release.

[Unreleased]: https://github.com/devslab-kr/easy-paging-spring-boot-starter/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/devslab-kr/easy-paging-spring-boot-starter/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/devslab-kr/easy-paging-spring-boot-starter/compare/v0.1.2...v0.2.0
[0.1.2]: https://github.com/devslab-kr/easy-paging-spring-boot-starter/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/devslab-kr/easy-paging-spring-boot-starter/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/devslab-kr/easy-paging-spring-boot-starter/releases/tag/v0.1.0

# Changelog

All notable changes to easy-paging are documented here. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows [Semantic Versioning](https://semver.org/).

For the canonical, machine-readable source see [`CHANGELOG.md`](https://github.com/devslab-kr/easy-paging-spring-boot-starter/blob/main/CHANGELOG.md) in the repository.

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

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/devslab-kr/easy-paging-spring-boot-starter/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/devslab-kr/easy-paging-spring-boot-starter/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/devslab-kr/easy-paging-spring-boot-starter/releases/tag/v0.1.0

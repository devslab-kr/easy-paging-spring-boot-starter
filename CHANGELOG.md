# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/devslab-kr/easy-paging-spring-boot-starter/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/devslab-kr/easy-paging-spring-boot-starter/releases/tag/v0.1.0

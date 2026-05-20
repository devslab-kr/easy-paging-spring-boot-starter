---
title: easy-paging — Annotation-driven pagination for Spring Boot + MyBatis
---

# easy-paging

> **Annotation-driven pagination for Spring Boot + MyBatis.**
> Offset (PageHelper) and keyset/cursor in one starter.

[:fontawesome-solid-rocket: Get Started](getting-started/installation.md){ .md-button .md-button--primary }
[:fontawesome-brands-github: GitHub](https://github.com/devslab-kr/easy-paging-spring-boot-starter){ .md-button }
[:fontawesome-brands-java: Maven Central](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter){ .md-button }

---

## At a glance

Drop one annotation on a controller method and get a JSON-ready paginated response. With the standard Controller → Service → Mapper layering, the controller stays thin:

```java
@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/reports")
    @AutoPaginate(maxSize = 50)                          // (1)!
    public PageResponse<Report> list(Pageable pageable) {
        return PageResponse.from(reports.findAll(), pageable);
    }
}
```

1.  The aspect sets up PageHelper's per-thread state, validates the sort parameter, clamps the page size, then cleans up after the mapper call returns.

A request to `GET /reports?page=0&size=20&sort=createdAt,desc` returns:

```json
{
  "content": [ /* 20 rows */ ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "first": true,
  "last": false,
  "empty": false
}
```

No `PageHelper.startPage(...)` calls scattered through your codebase, no `ThreadLocal` cleanup to remember, no per-controller boilerplate.

## What you get

<div class="grid cards" markdown>

-   :material-shield-check: **Safe by default**

    `?sort=name;DROP TABLE` rejected with HTTP 400 before reaching the database. Page size clamped at endpoint + global level.

-   :material-tag-arrow-up: **Spring Data shaped**

    JSON response matches Spring Data `Page` for client compatibility. Drop-in for teams already using `Pageable`.

-   :material-cursor-pointer: **Offset *and* keyset**

    `@AutoPaginate` for traditional lists, `@KeysetPaginate` for time-series and unbounded tables where `OFFSET`/`COUNT(*)` start to hurt.

-   :material-format-list-numbered: **0-based, consistent**

    Spring Data convention throughout — request, response, and internal `Pageable`. PageHelper's 1-based indexing handled transparently.

-   :material-cog-outline: **Pluggable response shape**

    Replace the default envelope with your company's standard response type via `PageResponseFactory`, or just define your own type with a static `from()` method.

-   :material-lightning-bolt: **Virtual Threads safe**

    Internal state cleaned up on every request, regardless of exception path. No `ThreadLocal` leaks.

-   :material-water: **Reactive: WebFlux + R2DBC**

    Optional `…-starter-reactive` artifact: `Mono<PageResponse<T>>` from `R2dbcEntityTemplate`, keyset helper for cursor scrolls, and a WebFlux `KeysetRequest` argument resolver. Same envelope as the MyBatis side.

</div>

## Quick install

```kotlin title="build.gradle.kts"
dependencies {
    implementation("kr.devslab:easy-paging-spring-boot-starter:0.4.0")
    // Optional — native R2DBC + WebFlux helpers:
    // implementation("kr.devslab:easy-paging-spring-boot-starter-reactive:0.4.0")
}
```

See [Installation](getting-started/installation.md) for prerequisites and full setup, or jump to the [Tutorial](getting-started/tutorial.md) for a 5-minute walkthrough.

## Where to go next

-   :material-school: **New to easy-paging?** → [Tutorial](getting-started/tutorial.md)
-   :material-book-open: **Looking for a specific feature?** → [Guides](guides/offset.md)
-   :material-table: **Want every configuration knob?** → [Reference](reference/configuration.md)
-   :material-history: **Migrating versions?** → [Changelog](changelog.md)

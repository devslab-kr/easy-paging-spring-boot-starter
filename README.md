# easy-paging-spring-boot-starter

**English** · [한국어](README.ko.md)

> Annotation-driven pagination for Spring Boot + MyBatis. Offset and keyset/cursor in one starter.

[![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/easy-paging-spring-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter)
[![Java](https://img.shields.io/badge/Java-21+-007396?logo=openjdk&logoColor=white)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0+-6db33f?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![CI](https://github.com/devslab-kr/easy-paging-spring-boot-starter/actions/workflows/ci.yml/badge.svg)](https://github.com/devslab-kr/easy-paging-spring-boot-starter/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/devslab-kr/easy-paging-spring-boot-starter/branch/main/graph/badge.svg)](https://codecov.io/gh/devslab-kr/easy-paging-spring-boot-starter)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

📖 **[Documentation → easy-paging.devslab.kr](https://easy-paging.devslab.kr/)**

> 💬 Questions, ideas, sharing your application? Head to [**devslab-examples Discussions**](https://github.com/devslab-kr/devslab-examples/discussions) — bilingual, maintained by the same folks who write the libraries.

## At a glance

Drop one annotation on a controller method and get a JSON-ready paginated response. With the standard Controller → Service → Mapper layering, the controller stays thin:

```java
// Controller
@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/reports")
    @AutoPaginate(maxSize = 50)
    public PageResponse<Report> list(Pageable pageable) {
        return PageResponse.from(reports.findAll(), pageable);
    }
}

// Service
@Service
class ReportService {

    private final ReportMapper mapper;

    ReportService(ReportMapper mapper) {
        this.mapper = mapper;
    }

    public List<Report> findAll() {
        return mapper.findAll();   // pagination is injected by the aspect at the controller level
    }
}
```

(Imports omitted for brevity — the next section shows the full file with every import, plus the MyBatis mapper.)

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

### Without this starter

The same endpoint, written by hand against PageHelper, looks roughly like this:

```java
@GetMapping("/reports")
public Map<String, Object> list(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String sort
) {
    // 1. Validate page size — clients can otherwise DoS you with ?size=999999.
    if (size <= 0 || size > 100) size = 20;

    // 2. Parse the ?sort= parameter and reject SQL-injection attempts
    //    (?sort=name;DROP TABLE users would otherwise reach the database).
    String orderBy = parseAndValidateSort(sort);   // ~30 lines of code somewhere

    // 3. Push pagination state onto PageHelper's per-thread stack.
    PageHelper.startPage(page + 1, size);          // 1-indexed, not 0-indexed
    if (!orderBy.isEmpty()) {
        PageHelper.orderBy(orderBy);
    }

    try {
        // 4. Run the query — PageHelper now intercepts and rewrites the SQL.
        PageInfo<Report> info = new PageInfo<>(reportMapper.findAll());

        // 5. Hand-build the response JSON.
        return Map.of(
            "content",       info.getList(),
            "page",          page,
            "size",          size,
            "totalElements", info.getTotal(),
            "totalPages",    info.getPages(),
            "first",         info.isIsFirstPage(),
            "last",          info.isIsLastPage()
        );
    } finally {
        // 6. Critical: clear the per-thread state. Forget this and the next
        //    request that lands on the same thread (or Virtual Thread carrier)
        //    will inherit stale pagination settings, paginating queries that
        //    were never meant to be paginated.
        PageHelper.clearPage();
    }
}
```

The starter collapses all six concerns into the four-line controller at the top of this section. Steps 1, 2, 5, and 6 disappear entirely; step 3 becomes the annotation; step 4 stays the same plain mapper call.

## What you get

- **Spring Data-shaped JSON** out of the box (or [override the response format](#custom-response-format) if you have a company standard).
- **0-based pagination** following Spring Data convention (`?page=0` is the first page); PageHelper's 1-based indexing is handled transparently underneath.
- **Safe `?sort=…`** — the sort parameter is validated before it reaches the database; injection attempts are rejected with HTTP 400.
- **Page-size clamping** at both the per-endpoint and global level — clients can't ask for `?size=999999`.
- **Sensible defaults** — page size, max size, and "reasonable" out-of-range handling are all configurable.
- **Keyset (cursor) pagination** for time-series or unbounded tables where `OFFSET` and `COUNT(*)` become slow ([details](#keyset--cursor-pagination---keysetpaginate)).
- **WebFlux/Reactor support** for blocking MyBatis on `Schedulers.boundedElastic()` ([details](#reactive-webflux-support)).
- **Native R2DBC + WebFlux** via the optional `…-starter-reactive` companion artifact — `Mono<PageResponse<T>>` from `R2dbcEntityTemplate`, lexicographic keyset `WHERE` builder, reactive `KeysetRequest` argument resolver. Same envelope shape as the MyBatis side so clients see one contract.
- **Safe under Virtual Threads** — internal state is cleaned up on every request, no leaks.

## Install

```kotlin
// build.gradle.kts
dependencies {
    implementation("kr.devslab:easy-paging-spring-boot-starter:4.0.0")
    // Optional — only if you also need native R2DBC + WebFlux helpers:
    // implementation("kr.devslab:easy-paging-spring-boot-starter-reactive:4.0.0")
}
```

You bring:
- Spring Boot 4.0+ on Java 21+ (built/tested against 4.0.6)
- a JDBC driver (or an R2DBC driver if using the reactive starter)

> Still on Spring Boot 3.3–3.5? Use the [`3.x` maintenance line](https://github.com/devslab-kr/easy-paging-spring-boot-starter/tree/3.x) (`kr.devslab:easy-paging-spring-boot-starter:3.0.0`). Same API, certified against the SB3 BOM, still receives security patches. The library major matches the Spring Boot major — see the [versioning policy](https://github.com/devslab-kr/.github/blob/main/.github/VERSIONING.md).

(MyBatis Spring Boot Starter is provided transitively — see the [installation guide](https://easy-paging.devslab.kr/getting-started/installation/) if you need a different MyBatis line.)

The core starter pulls in `spring-boot-starter-aspectj` (renamed from `spring-boot-starter-aop` in SB4), `spring-data-commons`, `pagehelper-spring-boot-starter`, and `mybatis-spring-boot-starter`. **You do not need Spring Data JPA** — only the lightweight `spring-data-commons` (which provides `Pageable`, `Page`, `Sort`) comes along automatically.

The reactive starter declares `spring-boot-starter-webflux` and `spring-boot-starter-data-r2dbc` as `compileOnly`, so you only pay for what you actually use.

## Runnable examples

Standalone Spring Boot projects that exercise every feature documented below — clone, `./gradlew bootRun`, curl. No copy-paste from this README into your own project; the examples are already wired up end to end (test classes included).

| Demo | Showcases |
| --- | --- |
| [`easy-paging-demo`](https://github.com/devslab-kr/devslab-examples/tree/main/easy-paging-demo) | `@AutoPaginate` against H2 — also covers the advanced [custom envelope](#custom-response-format) section (`/reports/company` and `/reports/auto-envelope`) |
| [`easy-paging-keyset-demo`](https://github.com/devslab-kr/devslab-examples/tree/main/easy-paging-keyset-demo) | `@KeysetPaginate` cursor pagination over a 300-row time-series (H2). Test asserts the cursor walk covers every row exactly once |
| [`easy-paging-postgres-demo`](https://github.com/devslab-kr/devslab-examples/tree/main/easy-paging-postgres-demo) | Same starter against real PostgreSQL — Docker Compose for `bootRun`, Testcontainers + `@ServiceConnection` for tests, no local Postgres install |
| [`easy-paging-reactive-demo`](https://github.com/devslab-kr/devslab-examples/tree/main/easy-paging-reactive-demo) | The reactive companion artifact via `R2dbcOffsetPagingSupport` (WebFlux + R2DBC + Docker PostgreSQL) |

Full index at [github.com/devslab-kr/devslab-examples](https://github.com/devslab-kr/devslab-examples).

## Offset pagination — `@AutoPaginate`

The default strategy. Best for traditional paginated lists where you want a total count and the data fits comfortably in `LIMIT/OFFSET`.

A complete, layered implementation:

```java
// src/main/java/com/example/report/ReportController.java
package com.example.report;

import kr.devslab.easypaging.annotation.AutoPaginate;
import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    @AutoPaginate(maxSize = 50)
    public PageResponse<Report> list(Pageable pageable) {
        // The aspect calls PageHelper.startPage(...) before this body runs,
        // so the mapper call inside reports.findAll() is automatically paginated.
        return PageResponse.from(reports.findAll(), pageable);
    }
}
```

```java
// src/main/java/com/example/report/ReportService.java
package com.example.report;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
class ReportService {

    private final ReportMapper mapper;

    ReportService(ReportMapper mapper) {
        this.mapper = mapper;
    }

    public List<Report> findAll() {
        // A real service typically applies authorization, tenant filtering,
        // and domain rules here before reaching the mapper.
        return mapper.findAll();
    }
}
```

The MyBatis mapper stays a plain `List` query — no pagination logic, no `LIMIT/OFFSET`. With the XML mapping style typical in enterprise codebases, the interface is just a method signature:

```java
// src/main/java/com/example/report/ReportMapper.java
package com.example.report;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportMapper {
    List<Report> findAll();   // aspect injects pagination at runtime
}
```

```xml
<!-- src/main/resources/mapper/ReportMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.report.ReportMapper">

    <select id="findAll" resultType="com.example.report.Report">
        SELECT id, title, created_at AS createdAt
        FROM reports
    </select>

</mapper>
```

Point MyBatis at the XML file via `application.yml`:

```yaml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

The `@AutoPaginate` aspect intercepts the controller call, sets up PageHelper's per-thread state, and the very next MyBatis query runs with the correct `LIMIT/OFFSET` and `ORDER BY` injected — your XML stays mercifully boring.

### Annotation options

| Attribute     | Default | Meaning                                                                       |
|---------------|---------|-------------------------------------------------------------------------------|
| `count`       | `true`  | Run the `COUNT(*)` query for `totalElements`/`totalPages`. Disable for time-series or log tables where the count would dominate query time. |
| `maxSize`     | `100`   | Upper bound on caller-supplied page size.                                     |
| `reasonable`  | `true`  | When `true`, out-of-range page numbers are silently clamped instead of returning empty. |

### Using every option together

```java
// Controller
@GetMapping("/audit-events")
@AutoPaginate(
    count       = false,    // audit log has 100M+ rows; COUNT(*) would dominate
    maxSize     = 200,      // power users need bigger pages for export
    reasonable  = false     // strict mode: page > totalPages returns empty
)
public PageResponse<AuditEvent> events(Pageable pageable) {
    return PageResponse.from(auditEvents.findAll(), pageable);
}

// Service
@Service
class AuditEventService {
    private final AuditEventMapper mapper;
    // ... constructor omitted

    public List<AuditEvent> findAll() {
        return mapper.findAll();
    }
}
```

### Return-type choices

| Declared return type      | Behavior                                                                         |
|---------------------------|----------------------------------------------------------------------------------|
| `PageResponse<T>`         | Wrapped envelope with content + pagination metadata. **Recommended for REST.**   |
| `Object`                  | Wrapped envelope — also routes through a [custom factory](#custom-response-format) if one is registered. Use this when you've replaced the default response shape. |
| `List<T>`                 | Plain list (sliced and sorted, but no envelope or totals).                       |

### Page numbering

Page numbers are **0-based** throughout — request, response, and your `Pageable`-bound code all use the Spring Data convention. PageHelper internally uses 1-based numbering, but the aspect translates between them transparently so your mapper SQL and the rest of your code only ever see 0-based values.

```
GET /reports?page=0&size=20  →  first page
GET /reports?page=1&size=20  →  second page
```

For APIs that want to expose 1-based numbering to clients (a common preference in some teams), set `easy-paging.one-indexed-pages: true` — `?page=1` then becomes the first page and the response's `page` field starts at `1`. Keyset endpoints are unaffected.

### Sorting

Pageable picks up Spring Data's standard sort syntax. Multi-column sort works out of the box:

```
GET /reports?page=0&size=20&sort=createdAt,desc&sort=name,asc
```

The aspect translates this to `ORDER BY created_at desc, name asc` and hands it to PageHelper. Property names are validated against `[A-Za-z_][A-Za-z0-9_.]*` — semicolons, parentheses, and spaces are rejected with `IllegalArgumentException`, so a malicious `?sort=…;DROP TABLE…` never reaches your database.

For null handling, configure it programmatically:

```java
import org.springframework.data.domain.Sort;

Pageable pageable = PageRequest.of(0, 20, Sort.by(
    Sort.Order.desc("createdAt").with(Sort.NullHandling.NULLS_LAST),
    Sort.Order.asc("name")
));
// Translates to: ORDER BY created_at desc nulls last, name asc
```

## Keyset / cursor pagination — `@KeysetPaginate`

For unbounded streams — logs, location tracks, audit events — where both `COUNT(*)` and large `OFFSET`s start to hurt.

```java
// src/main/java/com/example/location/LocationController.java
package com.example.location;

import java.util.UUID;
import kr.devslab.easypaging.annotation.KeysetPaginate;
import kr.devslab.easypaging.core.KeysetPage;
import kr.devslab.easypaging.core.KeysetRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/locations")
class LocationController {

    private final LocationService locations;

    LocationController(LocationService locations) {
        this.locations = locations;
    }

    @GetMapping
    @KeysetPaginate(
        keys        = {"time", "id"},   // composite key — timestamp + id tiebreaker
        direction   = "DESC",            // newest first
        defaultSize = 50,
        maxSize     = 200
    )
    public KeysetPage<Location> stream(KeysetRequest req, @RequestParam UUID workerId) {
        // KeysetRequest is filled by the argument resolver from
        // ?cursor=…&size=…&direction=… — see the @KeysetPaginate annotation above
        // for defaults. The controller delegates straight to the service.
        return locations.stream(workerId, req);
    }
}
```

```java
// src/main/java/com/example/location/LocationService.java
package com.example.location;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.core.KeysetPage;
import kr.devslab.easypaging.core.KeysetRequest;
import org.springframework.stereotype.Service;

@Service
class LocationService {

    private final LocationMapper mapper;
    private final CursorCodec codec;

    LocationService(LocationMapper mapper, CursorCodec codec) {
        this.mapper = mapper;
        this.codec = codec;
    }

    public KeysetPage<Location> stream(UUID workerId, KeysetRequest req) {
        // Fetch size + 1 rows so we can detect whether a next page exists.
        List<Location> rows = mapper.findAfter(
            workerId,
            req.keyAsInstant("time"),
            req.keyAsLong("id"),
            req.size() + 1);

        // The keyExtractor lambda tells the helper which fields of the last
        // visible row to encode as the next cursor.
        return KeysetPage.build(rows, req, r -> Map.of(
            "time", r.getTime(),
            "id",   r.getId()
        ), codec);
    }
}
```

The corresponding MyBatis mapper writes the keyset `WHERE` clause explicitly — the cursor values flow in as parameters:

```java
// src/main/java/com/example/location/LocationMapper.java
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LocationMapper {
    List<Location> findAfter(
        @Param("workerId") UUID workerId,
        @Param("time")     Instant time,    // null for the first page
        @Param("id")       Long id,         // null for the first page
        @Param("limit")    int limit);
}
```

```xml
<!-- src/main/resources/mapper/LocationMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.location.LocationMapper">

    <select id="findAfter" resultType="com.example.location.Location">
        SELECT id, time, lat, lng
        FROM locations
        WHERE worker_id = #{workerId}
          AND (
              #{time} IS NULL
              OR time &lt; #{time}
              OR (time = #{time} AND id &lt; #{id})
          )
        ORDER BY time DESC, id DESC
        LIMIT #{limit}
    </select>

</mapper>
```

> **XML escaping reminder**: `<` must be written as `&lt;` inside MyBatis XML — using a raw `<` makes the file fail to parse. (Some teams wrap the whole `<select>` body in `<![CDATA[ ... ]]>` to avoid this.)

A request to `GET /locations?cursor=<token>&size=50` returns:

```json
{
  "content": [ /* up to 50 rows */ ],
  "size": 50,
  "nextCursor": "eyJrIjp7InRpbWUiOi...",
  "prevCursor": null,
  "hasNext": true,
  "hasPrev": false
}
```

The client passes `nextCursor` back as `?cursor=…` for the next page. No `OFFSET`, no `COUNT(*)`.

### Cursor signing (do this in production)

Set `easy-paging.keyset.cursor-secret` in production. Without a secret, cursors are Base64-encoded but **not authenticated** — a malicious client can forge a cursor that targets rows they shouldn't see (e.g. via tenant-key tampering). With a secret, every cursor is HMAC-SHA256 signed and forgeries are rejected.

## Reactive (WebFlux) support

For WebFlux / Reactor apps that need to call blocking MyBatis. The same layered structure applies — the only change from the offset section is the return type and that the service is what calls `ReactivePagingSupport`:

```java
// src/main/java/com/example/report/ReportController.java
package com.example.report;

import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/reports")
    public Mono<PageResponse<Report>> list(Pageable pageable) {
        return reports.list(pageable);
    }
}
```

```java
// src/main/java/com/example/report/ReportService.java
package com.example.report;

import kr.devslab.easypaging.core.PageResponse;
import kr.devslab.easypaging.reactive.ReactivePagingSupport;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
class ReportService {

    private final ReportMapper mapper;

    ReportService(ReportMapper mapper) {
        this.mapper = mapper;
    }

    public Mono<PageResponse<Report>> list(Pageable pageable) {
        return ReactivePagingSupport.paginate(
            pageable,
            () -> mapper.findAll(),     // blocking MyBatis call, wrapped onto a worker thread
            /* maxSize */ 100,
            /* count   */ true);
    }
}
```

The mapper and XML are identical to the offset section — `ReactivePagingSupport` only changes how the call is dispatched, not what the query looks like.

The blocking work runs on `Schedulers.boundedElastic()` by default. If you already have a dedicated database scheduler, pass it in:

```java
import reactor.core.scheduler.Scheduler;

return ReactivePagingSupport.paginate(
    pageable,
    () -> mapper.findAll(),
    /* maxSize    */ 100,
    /* count      */ true,
    /* scheduler  */ databaseScheduler);
```

## Custom response format

The recommended pattern is to define your own response type with a static factory method, mirroring how the built-in `PageResponse.from()` works. The aspect handles PageHelper setup; your type owns the shape:

```java
// Your company's standard paginated response shape — type-safe, reused
// across every paginated endpoint.
public record CompanyPage<T>(
        boolean ok,
        List<T> data,
        PageMeta meta) {

    /** Build a CompanyPage from a mapper result + the request pageable. */
    public static <T> CompanyPage<T> from(List<T> list, Pageable pageable) {
        // Reuse the starter's metadata extraction; remap into your shape.
        PageResponse<T> p = PageResponse.from(list, pageable);
        return new CompanyPage<>(
            true,
            p.content(),
            new PageMeta(p.page(), p.size(), p.totalElements(), p.totalPages())
        );
    }
}

public record PageMeta(int page, int size, long total, int pages) {}
```

Use it directly from the controller — no `Object`, no special return type, no annotation:

```java
@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/reports")
    @AutoPaginate(maxSize = 50)
    public CompanyPage<Report> list(Pageable pageable) {
        return CompanyPage.from(reports.findAll(), pageable);
    }
}
```

You get full type safety, the JSON shape is whatever `CompanyPage` serializes to, and the aspect is still responsible for PageHelper lifecycle. The starter doesn't need to know about your type at all.

### Alternative: centralized factory bean

If you want the response shape applied automatically — without every controller calling `CompanyPage.from(...)` explicitly — register a `PageResponseFactory` bean and use `Object` as the controller's return type:

```java
import java.util.List;
import kr.devslab.easypaging.spi.PageResponseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PagingConfig {

    @Bean
    PageResponseFactory companyEnvelope() {
        return (content, pageable, totalElements, totalPages) ->
            new CompanyPage<>(
                true,
                content,
                new PageMeta(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    totalElements,
                    totalPages));
    }
}
```

```java
@GetMapping("/reports")
@AutoPaginate(maxSize = 50)
public Object list(Pageable pageable) {
    return reports.findAll();   // aspect routes the List through the factory
}
```

Trade-offs:

| | Custom type + `from()` (recommended) | `Object` + factory bean |
|---|---|---|
| Type safety | full — return type is `CompanyPage<Report>` | none — return type is `Object` |
| DRY | each controller calls `.from(...)` | factory is defined once |
| Mocking in tests | trivial — pure static method | requires factory bean in context |
| Best when | you only have one or two response shapes | every endpoint must use the same shape |

Both patterns coexist — pick per endpoint as needed. The factory only fires when the aspect itself is building the response (i.e. controller returned `List` or `Object`). Explicit `PageResponse<T>` or `CompanyPage<T>` values pass through untouched.

## Configuration

```yaml
easy-paging:
  enabled: true                # master switch
  default-page-size: 20        # used when caller omits ?size=
  max-page-size: 500           # global cap (never exceeded, even if @AutoPaginate maxSize is higher)
  auto-wrap-list: true         # set false to disable PageResponse wrapping globally
  keyset:
    cursor-secret: ${EASY_PAGING_CURSOR_SECRET:}   # HMAC secret; empty = unsigned (dev only)
    max-cursor-bytes: 2048                          # anti-DoS cap on decoded cursor payload
```

## License

[Apache License 2.0](LICENSE). Bug reports and pull requests welcome — see [CONTRIBUTING.md](CONTRIBUTING.md).

# Reactive (WebFlux + R2DBC) Support

easy-paging supports two distinct reactive patterns:

| Scenario | What's needed | Where to look |
|---|---|---|
| WebFlux app calling blocking MyBatis | nothing extra — `ReactivePagingSupport` ships with the core starter | [MyBatis on Reactor](#mybatis-on-reactor) below |
| WebFlux app on **Spring Data R2DBC** (native non-blocking SQL) | additional `easy-paging-spring-boot-starter-reactive` artifact | [Native R2DBC + WebFlux](#native-r2dbc--webflux) below |

The two layer together: a project on both MyBatis and R2DBC adds both starters and uses whichever helper fits the query at hand.

---

## MyBatis on Reactor

For WebFlux / Reactor applications that need to call blocking MyBatis. The core starter exposes `ReactivePagingSupport` — a small bridge that runs the entire `startPage → mapper.call → clearPage` cycle inside a single `Mono.fromCallable` on a blocking-IO scheduler, so PageHelper's `ThreadLocal` stays consistent across the call.

### Basic usage

```java
@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) { this.reports = reports; }

    @GetMapping("/reports")
    public Mono<PageResponse<Report>> list(Pageable pageable) {
        return reports.list(pageable);
    }
}
```

```java
@Service
class ReportService {

    private final ReportMapper mapper;

    ReportService(ReportMapper mapper) { this.mapper = mapper; }

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

### Custom scheduler

The blocking work runs on `Schedulers.boundedElastic()` by default. If you already have a dedicated database scheduler (e.g. with a sized thread pool matching your connection pool), pass it in:

```java
import reactor.core.scheduler.Scheduler;

return ReactivePagingSupport.paginate(
    pageable,
    () -> mapper.findAll(),
    /* maxSize    */ 100,
    /* count      */ true,
    /* scheduler  */ databaseScheduler);
```

### Why not use `@AutoPaginate` directly?

`@AutoPaginate` works through Spring AOP and PageHelper's `ThreadLocal`. Reactor's scheduler model means the thread that calls your method might not be the same thread that ultimately executes the mapper call — PageHelper's `ThreadLocal` would be set on the wrong thread.

`ReactivePagingSupport.paginate` solves this by capturing the pageable + flags at call time, subscribing on `boundedElastic` (or your custom scheduler), and doing the entire PageHelper setup / mapper call / cleanup on the same subscribed thread.

---

## Native R2DBC + WebFlux

For applications on **Spring Data R2DBC** (no MyBatis on the request path). Add the optional starter:

```kotlin title="build.gradle.kts"
dependencies {
    implementation("kr.devslab:easy-paging-spring-boot-starter:0.5.0")
    implementation("kr.devslab:easy-paging-spring-boot-starter-reactive:0.5.0")
}
```

(The optional reactive line is also published on the SB3 maintenance line at `0.4.0` — see [Installation](../getting-started/installation.md#choose-your-release-line) for the picker.)

You bring (same as a stock Spring Data R2DBC project): an R2DBC driver, `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`.

### Offset / limit pagination

`R2dbcOffsetPagingSupport.paginate(...)` wires two parallel R2DBC queries — the page rows and the total count — into a single `Mono<PageResponse<T>>`. Same envelope shape as the MyBatis side, so clients see one contract regardless of which back end serves the request.

```java
@RestController
@RequestMapping("/users")
class UserController {

    private final R2dbcEntityTemplate template;

    UserController(R2dbcEntityTemplate template) { this.template = template; }

    @GetMapping
    public Mono<PageResponse<User>> list(Pageable pageable) {
        return R2dbcOffsetPagingSupport.paginate(
            template,
            User.class,
            Criteria.where("active").isTrue(),    // any extra filter you want
            pageable);
    }
}
```

The `Pageable`'s sort is honoured — pass `PageRequest.of(0, 20, Sort.by("createdAt").descending())` to control ordering. Pass `Criteria.empty()` if you don't need extra filtering. The same `Criteria` is applied to the count query, so `totalElements` matches what users can actually scroll through.

### Keyset (cursor) pagination

`R2dbcKeysetSupport.paginate(...)` is the R2DBC equivalent of `KeysetPage.build` on the MyBatis side. Give it the key column metadata + a `KeysetRequest`, and it builds the lexicographic `WHERE` clause, applies the right `ORDER BY` (flipped for backward scans), runs the `size + 1` query, and assembles a `KeysetPage`.

```java
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport.KeyColumn;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport.SortDirection;

@RestController
@RequestMapping("/events")
class EventController {

    // Composite key: timestamp first (non-unique), id as tiebreaker.
    private static final List<KeyColumn> KEYS = List.of(
        new KeyColumn("created_at", "createdAt", Instant.class, SortDirection.DESC),
        new KeyColumn("id",         "id",        Long.class,    SortDirection.DESC));

    private final R2dbcEntityTemplate template;
    private final CursorCodec codec;

    EventController(R2dbcEntityTemplate template, CursorCodec codec) {
        this.template = template;
        this.codec = codec;
    }

    @GetMapping
    @KeysetPaginate(keys = {"createdAt", "id"}, direction = "DESC", defaultSize = 50)
    public Mono<KeysetPage<Event>> stream(KeysetRequest request) {
        return R2dbcKeysetSupport.paginate(
            template,
            Event.class,
            Criteria.empty(),
            KEYS,
            request,
            e -> Map.of("createdAt", e.getCreatedAt(), "id", e.getId()),
            codec);
    }
}
```

`KeyColumn` parameters: `column` (DB column name), `cursorKey` (name used in the cursor token JSON), `javaType` (used to coerce cursor values back to the right type — `Instant`, `Long`, `UUID`, etc.), `naturalDirection` (the column's intended sort, used to derive `WHERE` and `ORDER BY`).

The cursor token carries the scan direction internally, so on the client side `?cursor=nextCursor` and `?cursor=prevCursor` are enough — no explicit `?direction=` needed.

### Built-in type support

`R2dbcKeysetSupport` knows how to coerce cursor values to: `String`, primitive wrappers (`Long`, `Integer`, `Double`, etc.), `Instant`, `LocalDateTime`, `OffsetDateTime`, `LocalDate`, `UUID`. For other types, build the keyset `Criteria` manually and skip the helper.

### WebFlux argument resolver

`KeysetRequest` parameters are resolved automatically on WebFlux controllers — same query parameters as the servlet side (`?cursor=...&size=...&direction=...`). No manual wiring needed; the starter's auto-configuration registers `ReactiveKeysetRequestArgumentResolver` whenever WebFlux is on the classpath.

### Cursor signing (production)

Same as the servlet side — set `easy-paging.keyset.cursor-secret` in production. Without a secret, cursors are Base64-only and a malicious client can forge them. See the [Keyset guide](keyset.md#cursor-signing-production) for the full reasoning.

---

## See also

- [Offset pagination](offset.md) — synchronous `@AutoPaginate`
- [Keyset pagination](keyset.md) — synchronous `@KeysetPaginate` and the consumer-side `findBefore` pattern (mirrors what `R2dbcKeysetSupport` does internally)
- [Custom response format](custom-response.md) — wrap `PageResponse` / `KeysetPage` in your own envelope

# Reactive (WebFlux) Support

For WebFlux / Reactor applications that need to call blocking MyBatis. The starter exposes `ReactivePagingSupport` — a small bridge that runs the entire `startPage → mapper.call → clearPage` cycle inside a single `Mono.fromCallable` on a blocking-IO scheduler, so PageHelper's `ThreadLocal` stays consistent across the call.

## Basic usage

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

## Custom scheduler

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

## Why not use `@AutoPaginate` directly?

`@AutoPaginate` works through Spring AOP and PageHelper's `ThreadLocal`. Reactor's scheduler model means the thread that calls your method might not be the same thread that ultimately executes the mapper call — and PageHelper's `ThreadLocal` would be set on the wrong thread.

`ReactivePagingSupport.paginate` solves this by:

1. Capturing the pageable, max size, and count flag at call time.
2. Subscribing on `boundedElastic` (or your custom scheduler).
3. Inside the subscribed thread: setting up PageHelper, calling the mapper, wrapping the response, and clearing PageHelper — all on the same thread.

This makes the operation atomic with respect to PageHelper's ThreadLocal, regardless of upstream scheduler hops.

## See also

- [Offset pagination](offset.md) — synchronous `@AutoPaginate`
- [Custom response format](custom-response.md) — wrap the `PageResponse` in your own envelope

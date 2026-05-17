# Offset Pagination — `@AutoPaginate`

The default strategy. Best for traditional paginated lists where you want a total count and the data fits comfortably in `LIMIT/OFFSET`.

## Basic usage

```java
@RestController
@RequestMapping("/reports")
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) { this.reports = reports; }

    @GetMapping
    @AutoPaginate(maxSize = 50)
    public PageResponse<Report> list(Pageable pageable) {
        return PageResponse.from(reports.findAll(), pageable);
    }
}
```

The aspect resolves `Pageable` from the method args, sets up PageHelper for the next query, validates the `?sort=` parameter, clamps `?size=`, and cleans up on exit. See the [Tutorial](../getting-started/tutorial.md) for the complete service + mapper.

## Annotation options

| Attribute     | Default | Meaning                                                                       |
|---------------|---------|-------------------------------------------------------------------------------|
| `count`       | `true`  | Run the `COUNT(*)` query for `totalElements`/`totalPages`. Disable for time-series or log tables where the count would dominate query time. |
| `maxSize`     | `100`   | Upper bound on caller-supplied page size.                                     |
| `reasonable`  | `true`  | When `true`, out-of-range page numbers are silently clamped instead of returning empty. |

### Using every option together

```java
@GetMapping("/audit-events")
@AutoPaginate(
    count       = false,   // audit log has 100M+ rows; COUNT(*) would dominate
    maxSize     = 200,     // power users need bigger pages for export
    reasonable  = false    // strict mode: page > totalPages returns empty
)
public PageResponse<AuditEvent> events(Pageable pageable) {
    return PageResponse.from(auditEvents.findAll(), pageable);
}
```

## Return-type choices

The aspect inspects your method's declared return type and adapts:

| Declared return type      | Behavior                                                                         |
|---------------------------|----------------------------------------------------------------------------------|
| `PageResponse<T>`         | Wrapped envelope with content + pagination metadata. **Recommended for REST.**   |
| `Object`                  | Wrapped envelope — also routes through a [custom factory](custom-response.md) if one is registered. |
| `List<T>`                 | Plain list (sliced and sorted, but no envelope or totals).                       |

!!! note "Why not always wrap?"
    If your service method is consumed by both a REST endpoint and an internal caller, the internal caller may not want a JSON-shaped envelope. Declaring `List<T>` keeps the method usable in both contexts; declaring `PageResponse<T>` is explicit about the HTTP intent.

## What happens under the hood

1. **Resolve `Pageable`** from method arguments (Spring MVC's `PageableHandlerMethodArgumentResolver` populates it from `?page=`, `?size=`, `?sort=`).
2. **Clamp page size** against the smaller of the annotation's `maxSize` and the global `easy-paging.max-page-size` property.
3. **Translate `Sort`** into a SQL-safe `ORDER BY` (see [Sorting](sorting.md)).
4. **Call `PageHelper.startPage(...)`** — the next MyBatis query is paginated.
5. **Invoke method body** — your mapper call returns a `Page<T>` (which is also a `List<T>`).
6. **Wrap** based on declared return type (see table above).
7. **Clear PageHelper state** in `finally` — even if anything threw.

## Common patterns

### Filter then paginate

The service is the natural place for authorization, tenant filtering, or domain rules. The mapper stays a pure query:

```java
@Service
class ReportService {
    public List<Report> findAll() {
        var tenantId = currentTenant();      // your tenant resolver
        return mapper.findByTenant(tenantId);
    }
}
```

The aspect's PageHelper setup applies to **whichever** mapper call happens next — so filtering logic in the service is automatically paginated.

### Aggregating across multiple mappers

PageHelper's `startPage` applies to a single query. If your service composes results from multiple mappers, only paginate the one that drives the page:

```java
@AutoPaginate
public PageResponse<EnrichedReport> list(Pageable pageable) {
    List<Report> page = reportMapper.findAll();           // ← paginated
    Map<Long, Author> authors = authorMapper.findAll();   // ← NOT paginated (fetched fully)
    List<EnrichedReport> enriched = page.stream()
        .map(r -> new EnrichedReport(r, authors.get(r.authorId())))
        .toList();
    return PageResponse.from(enriched, pageable);
}
```

Wait — `PageResponse.from(enriched, pageable)` won't have the PageHelper metadata because `enriched` is a fresh `ArrayList`, not a `Page`. Use this pattern instead:

```java
List<Report> page = reportMapper.findAll();
PageResponse<Report> base = PageResponse.from(page, pageable);
List<EnrichedReport> enriched = base.content().stream()
    .map(...).toList();
return new PageResponse<>(enriched, base.page(), base.size(),
    base.totalElements(), base.totalPages(),
    base.first(), base.last(), enriched.isEmpty());
```

## See also

- [Sorting & page numbering](sorting.md) — `?sort=` syntax, SQL-injection protection
- [Custom response format](custom-response.md) — replace the default envelope
- [Configuration reference](../reference/configuration.md) — global tunables

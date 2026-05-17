# Custom Response Format

The default `PageResponse<T>` envelope matches Spring Data `Page` for client compatibility. When your team has a different standard — `{ ok, data, meta }`, `{ status, payload }`, anything — you have two patterns to switch.

## Pattern 1 — Custom type with static `from()` (recommended)

Define your own response type with a static factory method, mirroring how the built-in `PageResponse.from()` works. The aspect handles PageHelper setup; your type owns the shape:

```java
// Your company's standard paginated response shape — type-safe, reused
// across every paginated endpoint.
public record CompanyPage<T>(
        boolean ok,
        List<T> data,
        PageMeta meta) {

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

Use it directly from the controller — no `Object`, no special return type, no annotation change:

```java
@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) { this.reports = reports; }

    @GetMapping("/reports")
    @AutoPaginate(maxSize = 50)
    public CompanyPage<Report> list(Pageable pageable) {
        return CompanyPage.from(reports.findAll(), pageable);
    }
}
```

You get full type safety, the JSON shape is whatever `CompanyPage` serializes to, and the aspect is still responsible for PageHelper lifecycle. The starter doesn't need to know about your type at all.

## Pattern 2 — Centralized factory bean

If you want the response shape applied automatically — without every controller calling `CompanyPage.from(...)` explicitly — register a `PageResponseFactory` bean and use `Object` as the controller's return type:

```java
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

## Comparison

|                            | Custom type + `from()` (recommended) | `Object` + factory bean |
|----------------------------|--------------------------------------|-------------------------|
| Type safety                | full — return type is `CompanyPage<Report>` | none — return type is `Object` |
| DRY                        | each controller calls `.from(...)` | factory defined once |
| Mocking in tests           | trivial — pure static method | requires factory bean in context |
| Best when                  | one or two response shapes | every endpoint must use the same shape |

Both patterns coexist — pick per endpoint as needed. The factory only fires when the aspect itself is building the response (i.e. controller returned `List` or `Object`). Explicit `PageResponse<T>` or `CompanyPage<T>` values pass through untouched.

## Class-based factory implementation

For factories that need dependencies (logger, metrics, etc.) or are easier to unit-test in isolation, implement the SPI as a class:

```java
@Component
class CompanyEnvelope implements PageResponseFactory {

    @Override
    public Object create(List<?> content, Pageable pageable, long totalElements, int totalPages) {
        return new CompanyPage<>(
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

Functionally equivalent to the lambda form — pick based on testing/wiring preferences.

## See also

- [Offset pagination](offset.md) — `@AutoPaginate` return-type decision
- [Configuration reference](../reference/configuration.md) — `easy-paging.auto-wrap-list` to disable all wrapping globally

# Sorting & Page Numbering

This page covers two topics that often confuse first-time users: the `?sort=` query parameter syntax, and the 0-based page numbering convention.

## Page numbering

Page numbers are **0-based** throughout — request, response, and your `Pageable`-bound code all use the Spring Data convention. PageHelper internally uses 1-based numbering, but the aspect translates between them transparently so your mapper SQL and the rest of your code only ever see 0-based values.

```
GET /reports?page=0&size=20  →  first page
GET /reports?page=1&size=20  →  second page
```

In the JSON response:

```json
{
  "page": 0,        ← first page
  "totalPages": 7,
  "first": true,
  "last": false
}
```

### Exposing 1-based page numbers to clients

If your team or API contract prefers human-friendly 1-based page numbering ("page 1" is the first page), flip a single property:

```yaml
easy-paging:
  one-indexed-pages: true
```

With this on:

- Clients send `?page=1&size=20` for the first page (and the response shows `"page": 1`).
- Internally, Spring's `PageableHandlerMethodArgumentResolver` translates the incoming `?page=1` to `Pageable(pageNumber=0)`, and the aspect adds `+1` when serializing the response.
- Keyset / cursor endpoints are unaffected — cursors don't use page numbers.
- `totalPages`, `first`, and `last` are unchanged — only the `page` index shifts.

## Sorting — basic

`Pageable` picks up Spring Data's standard sort syntax. A single column:

```
GET /reports?sort=createdAt,desc
```

Translates to `ORDER BY created_at desc`.

## Multi-column sort

Pass the `sort` parameter multiple times:

```
GET /reports?page=0&size=20&sort=createdAt,desc&sort=name,asc
```

The aspect translates this to `ORDER BY created_at desc, name asc` and hands it to PageHelper.

## Null handling

For explicit null ordering, configure programmatically (this isn't expressible in the URL syntax):

```java
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

Pageable pageable = PageRequest.of(0, 20, Sort.by(
    Sort.Order.desc("createdAt").with(Sort.NullHandling.NULLS_LAST),
    Sort.Order.asc("name")
));
// Translates to: ORDER BY created_at desc nulls last, name asc
```

Defaults to the database's native null ordering when not specified.

## SQL injection protection

Property names are validated against the pattern `[A-Za-z_][A-Za-z0-9_.]*` — semicolons, parentheses, spaces, and other punctuation are rejected with HTTP 400 before reaching the database.

```
GET /reports?sort=name;DROP%20TABLE%20users  →  400 Bad Request
GET /reports?sort=(SELECT 1)                 →  400 Bad Request
GET /reports?sort=name'OR'1'='1              →  400 Bad Request
```

This validation happens at the aspect layer, before the SQL is ever built. No malformed property reaches PageHelper or the database.

!!! warning "Mapper column names must be JPA-style"
    Because the aspect passes the property name straight into `ORDER BY` after validation, the property name must match the column name (or column alias) used in your `SELECT` clause. For snake_case columns mapped to camelCase Java fields, enable `mybatis.configuration.map-underscore-to-camel-case: true` and use the **camelCase** property name in `?sort=`.

## Disabling sort

To accept only specific sort properties (recommended for stable API contracts), filter at the controller layer:

```java
private static final Set<String> ALLOWED_SORT = Set.of("createdAt", "id", "name");

@GetMapping("/reports")
@AutoPaginate
public PageResponse<Report> list(Pageable pageable) {
    Sort filtered = Sort.by(pageable.getSort().stream()
        .filter(o -> ALLOWED_SORT.contains(o.getProperty()))
        .toList());
    Pageable safe = PageRequest.of(
        pageable.getPageNumber(),
        pageable.getPageSize(),
        filtered);
    return PageResponse.from(reports.findAll(), safe);
}
```

This is a defense-in-depth pattern on top of the aspect's syntactic validation — restricting *which* columns are sortable, not just which characters are allowed.

## See also

- [Configuration reference](../reference/configuration.md) — global sort defaults
- [Offset pagination](offset.md) — where `Pageable` flows into the aspect

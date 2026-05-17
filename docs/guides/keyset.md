# Keyset / Cursor Pagination — `@KeysetPaginate`

For unbounded streams — logs, location tracks, audit events, infinite scroll — where both `COUNT(*)` and large `OFFSET`s start to hurt.

## When to use keyset over offset

| Scenario | Recommended |
|---|---|
| Admin dashboards, small/medium tables, "go to page 5" UX | `@AutoPaginate` (offset) |
| Time-series logs, IoT events, audit trails | `@KeysetPaginate` |
| Infinite scroll, mobile feeds | `@KeysetPaginate` |
| Tables with > 10M rows where `COUNT(*)` is slow | `@KeysetPaginate` |

Offset works fine until your table grows past a few hundred thousand rows. After that, `OFFSET 1000000` makes the database scan and discard the first million rows on every page. Keyset uses a `WHERE` clause based on the last seen value, so the database can use an index and stop after `LIMIT` rows.

## Basic usage

```java
@RestController
@RequestMapping("/locations")
class LocationController {

    private final LocationService locations;

    LocationController(LocationService locations) { this.locations = locations; }

    @GetMapping
    @KeysetPaginate(
        keys        = {"time", "id"},   // composite key — timestamp + id tiebreaker
        direction   = "DESC",            // newest first
        defaultSize = 50,
        maxSize     = 200
    )
    public KeysetPage<Location> stream(KeysetRequest req, @RequestParam UUID workerId) {
        return locations.stream(workerId, req);
    }
}
```

```java
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

        return KeysetPage.build(rows, req, r -> Map.of(
            "time", r.getTime(),
            "id",   r.getId()
        ), codec);
    }
}
```

The mapper writes the keyset `WHERE` clause explicitly:

```xml
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
```

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

## Annotation options

| Attribute      | Default | Meaning                                                                  |
|----------------|---------|--------------------------------------------------------------------------|
| `keys`         | (required) | The cursor key columns, in order. Typically a timestamp + ID tiebreaker. |
| `direction`    | `"DESC"`   | Default direction for the keys. Client can override via `?direction=ASC`. |
| `defaultSize`  | `20`     | Page size when caller omits `?size=`.                                    |
| `maxSize`      | `100`    | Upper bound on caller-supplied page size.                                |

## Cursor signing (production)

Set `easy-paging.keyset.cursor-secret` in production. Without a secret, cursors are Base64-encoded but **not authenticated** — a malicious client can forge a cursor that targets rows they shouldn't see (e.g. via tenant-key tampering). With a secret, every cursor is HMAC-SHA256 signed and forgeries are rejected.

```yaml title="application.yml"
easy-paging:
  keyset:
    cursor-secret: ${EASY_PAGING_CURSOR_SECRET}   # 32+ bytes random
    max-cursor-bytes: 2048
```

Generate a secret:

```bash
openssl rand -base64 32
```

Store it as an environment variable, secret manager entry, or `application-prod.yml` (never commit to git).

## The "+1 row" trick

The mapper queries `size + 1` rows. The `KeysetPage.build` helper detects:

- **`size + 1` rows returned** → there's a next page; trim the extra row, encode the last visible row's keys as `nextCursor`.
- **`<= size` rows returned** → this is the last page; `nextCursor` is `null`, `hasNext` is `false`.

This avoids a second `COUNT(*)` query just to know if more rows exist.

## Composite keys explained

When sorting by a non-unique column (like a timestamp), you need a tiebreaker. Two rows with the same `time` would be ambiguous as cursors.

The keyset `WHERE` becomes a lexicographic comparison:

```sql
WHERE time < @last_time
   OR (time = @last_time AND id < @last_id)
```

The first clause handles "strictly earlier in time". The second handles "same time, but earlier id". Together they give a deterministic order across pages.

Use `id` (or any guaranteed-unique column) as the last key.

## See also

- [Custom response format](custom-response.md) — wrap `KeysetPage` in a company envelope
- [Configuration reference](../reference/configuration.md) — `easy-paging.keyset.*` settings

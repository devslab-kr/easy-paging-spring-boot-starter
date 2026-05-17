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
        // Dispatch to findBefore on BACKWARD scans (see "Bidirectional scrolling" below).
        List<Location> rows = (req.direction() == Cursor.Direction.BACKWARD)
            ? mapper.findBefore(workerId, req.keyAsInstant("time"), req.keyAsLong("id"), req.size() + 1)
            : mapper.findAfter (workerId, req.keyAsInstant("time"), req.keyAsLong("id"), req.size() + 1);

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
  "prevCursor": "eyJrIjp7InRpbWUiOi...",
  "hasNext": true,
  "hasPrev": true
}
```

The client passes `nextCursor` back as `?cursor=…` to load older items, or `prevCursor` to load newer items — see [Bidirectional scrolling](#bidirectional-scrolling) for the full contract. No `OFFSET`, no `COUNT(*)`.

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

## Bidirectional scrolling

`nextCursor` and `prevCursor` use **user-perspective semantics** that don't change with the request's scan direction:

- **`nextCursor`** → load the next page in display order (older items when the natural sort is DESC by time). Always encodes `direction=FORWARD` inside the token.
- **`prevCursor`** → load the previous page in display order (newer items). Always encodes `direction=BACKWARD`.

The client doesn't need to track "which way am I currently scanning". They click *load more below* → `?cursor=nextCursor`. Click *load more above* → `?cursor=prevCursor`. The cursor token itself carries the direction; the resolver decodes it and the controller dispatches to the right mapper query.

### Mirror mapper for backward scans

```xml
<select id="findBefore" resultType="com.example.location.Location">
    SELECT id, time, lat, lng
    FROM locations
    WHERE worker_id = #{workerId}
      AND ( time &gt; #{time}
            OR (time = #{time} AND id &gt; #{id}) )
    ORDER BY time ASC, id ASC
    LIMIT #{limit}
</select>
```

Two changes from `findAfter`:

1. **Flip the comparisons**: `<` becomes `>` (we want rows that are *newer* than the cursor, not older).
2. **Flip the `ORDER BY`**: `DESC` becomes `ASC` so the rows nearest the cursor come back first. `KeysetPage.build` reverses them back to display order automatically — the returned `content` list always matches the user-facing view.

### Controller dispatch

```java
@KeysetPaginate(keys = {"time", "id"}, direction = "DESC", defaultSize = 50)
public KeysetPage<Location> stream(KeysetRequest req, @RequestParam UUID worker) {
    List<Location> rows = (req.direction() == Cursor.Direction.BACKWARD)
        ? mapper.findBefore(worker, req.keyAsInstant("time"), req.keyAsLong("id"), req.size() + 1)
        : mapper.findAfter (worker, req.keyAsInstant("time"), req.keyAsLong("id"), req.size() + 1);
    return KeysetPage.build(rows, req, r -> Map.of("time", r.getTime(), "id", r.getId()), codec);
}
```

### What `hasNext` / `hasPrev` mean

| Field | Meaning (regardless of scan direction) |
|---|---|
| `hasNext` | More rows exist *past this page* in display order (older items). |
| `hasPrev` | More rows exist *before this page* in display order (newer items). |

For a first-page FORWARD request, `hasPrev` is `false` because nothing is newer than the dataset's natural start. For any page that was loaded with a cursor — FORWARD or BACKWARD — at least one row exists on the opposite side, so the corresponding `has*` is `true`.

### Edge cases

- **Empty cursor + `?direction=BACKWARD`** is treated the same as `direction=FORWARD` — there's no anchor row, so the scan starts at the natural beginning. To start from the oldest end of the stream, the client should pass an explicit cursor pointing past the dataset (or just let the natural DESC order surface the newest items first).
- **The explicit `?direction=` query parameter still overrides the cursor's encoded direction.** This is intentional — it lets a client re-purpose a cursor without re-encoding server-side (e.g. for testing or for "go back to where this cursor was, then walk the other way" flows).

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

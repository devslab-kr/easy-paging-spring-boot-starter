# Configuration Reference

All properties live under the `easy-paging` prefix in `application.yml` / `application.properties`. Defaults shown below — you only need to set what differs.

## Full property tree

```yaml title="application.yml"
easy-paging:
  enabled: true                  # master switch; set false to disable the starter entirely
  default-page-size: 20          # used when caller omits ?size=
  max-page-size: 500             # global hard cap (never exceeded, even if @AutoPaginate maxSize is higher)
  auto-wrap-list: true           # set false to disable PageResponse wrapping globally
  one-indexed-pages: false       # set true for 1-based page numbers (client + response)
  keyset:
    cursor-secret: ""            # HMAC-SHA256 secret for signing cursors; empty = unsigned (dev only)
    max-cursor-bytes: 2048       # anti-DoS cap on decoded cursor payload size
```

## Property details

### `easy-paging.enabled`

| | |
|---|---|
| **Type** | `boolean` |
| **Default** | `true` |
| **Effect** | Master switch for the entire starter |

When `false`, the auto-configuration is skipped. `@AutoPaginate` and `@KeysetPaginate` become no-ops (the methods still run, but no PageHelper setup happens and no request argument resolution fires).

Useful for disabling pagination during testing, or in environments where MyBatis isn't initialized.

### `easy-paging.default-page-size`

| | |
|---|---|
| **Type** | `int` |
| **Default** | `20` |
| **Effect** | Fallback page size when the request omits `?size=` |

Spring MVC's `PageableHandlerMethodArgumentResolver` has its own default (20 as well). This property is consulted by the aspect when the resolved `Pageable` reports a non-positive `pageSize`.

### `easy-paging.max-page-size`

| | |
|---|---|
| **Type** | `int` |
| **Default** | `500` |
| **Effect** | Absolute upper bound on page size, regardless of per-annotation `maxSize` |

Used as a defense-in-depth cap. Even if a controller declares `@AutoPaginate(maxSize = 10000)`, the effective page size will be clamped to `min(annotation.maxSize, easy-paging.max-page-size)`.

Set lower for stricter DoS protection across the entire application.

### `easy-paging.auto-wrap-list`

| | |
|---|---|
| **Type** | `boolean` |
| **Default** | `true` |
| **Effect** | Whether the aspect wraps `List` returns into `PageResponse` envelopes |

When `false`, methods declaring `PageResponse` or `Object` return type that actually return a `List` will have the list returned as-is, with no envelope wrapping. The aspect still calls `PageHelper.startPage(...)` — only the wrapping step is skipped.

Useful when you want to handle all response wrapping manually (e.g. via a `ResponseBodyAdvice`).

### `easy-paging.one-indexed-pages`

| | |
|---|---|
| **Type** | `boolean` |
| **Default** | `false` (Spring Data 0-based convention) |
| **Effect** | When `true`, page numbers are 1-based on both request and response |

With `true`:

- Client sends `?page=1` for the first page; the response shows `"page": 1`.
- Spring's `PageableHandlerMethodArgumentResolver` is configured with `setOneIndexedParameters(true)`, so the incoming 1-based value is translated to a 0-based `Pageable` internally.
- The aspect shifts the response `page` field by `+1` on the way out.
- Other metadata (`totalPages`, `first`, `last`) is unchanged.
- Keyset (cursor) endpoints are unaffected — cursors don't use page numbers.

Useful when your API contract or front-end convention is human-friendly 1-based numbering.

### `easy-paging.keyset.cursor-secret`

| | |
|---|---|
| **Type** | `String` |
| **Default** | `""` (empty) |
| **Effect** | HMAC-SHA256 signing key for keyset cursors |

When non-empty, every cursor is signed and forgery attempts are rejected with HTTP 400. When empty, cursors are still Base64-encoded but unauthenticated — a malicious client can forge cursors that target rows they shouldn't see.

**Production setup**: Generate a 32+ byte random secret, store as environment variable, never commit:

```bash
openssl rand -base64 32
```

```yaml
easy-paging:
  keyset:
    cursor-secret: ${EASY_PAGING_CURSOR_SECRET}
```

### `easy-paging.keyset.max-cursor-bytes`

| | |
|---|---|
| **Type** | `int` |
| **Default** | `2048` |
| **Effect** | Maximum size of the decoded cursor payload, in bytes |

Cursors larger than this are rejected with HTTP 400. Prevents memory amplification attacks where an attacker submits a giant cursor token. Adjust upward only if your cursor keys legitimately require it (rare).

## IDE auto-completion

The starter ships configuration metadata, so IntelliJ IDEA (and other IDEs with Spring Boot support) provide:

- Auto-completion for the `easy-paging.*` keys
- Inline documentation
- Default value hints
- Validation warnings for unknown keys

You should see all keys above as suggestions when you type `easy-paging.` in `application.yml`.

# Roadmap

A living document of what's planned for easy-paging. Items are loose commitments — concrete enough to set expectations, loose enough to react to user feedback. Open an issue if you want to nudge anything up or down the list.

The canonical version lives at [`ROADMAP.md`](https://github.com/devslab-kr/easy-paging-spring-boot-starter/blob/main/ROADMAP.md). For released features see the [Changelog](changelog.md).

---

## Next minor — v0.3.0

Smaller features that fit cleanly into the existing surface.

### Keyset reverse direction

Activate the currently-deferred `prevCursor` field on `KeysetPage`. The mapper side needs a mirrored `findBefore` query, and the codec needs to track scan direction in the cursor token (already wired in `Cursor.Direction`, just not surfaced).

**Use case**: true bidirectional infinite scroll — "load more above" as well as "load more below". Most mobile feeds want this.

### Spring Boot baseline upgrade

Bump the Spring Boot BOM from 3.3.5 → current LTS (3.5 / 3.7, whichever is OSS-supported at release time). Keeps the dependency graph fresh and shakes out any deprecation breakage early.

Backward compatible at the consumer level — anyone on Spring Boot 3.3+ will continue to work, this just shifts our internal baseline.

---

## Medium term — v0.4.x

Larger features that warrant their own minor versions.

### Native WebFlux + R2DBC support

Today the WebFlux story is `ReactivePagingSupport.paginate(...)` — a static helper that bridges blocking MyBatis onto `Schedulers.boundedElastic()`. That's fine for MyBatis users on WebFlux, but doesn't reach actual reactive databases.

v0.4 adds first-class support for:

- Spring Data R2DBC repositories returning `Mono<Page<T>>` / `Flux<T>`
- A WebFlux-shaped `KeysetRequest` argument resolver
- `Mono<PageResponse<T>>` as a recognized return type from `@AutoPaginate`

Probably ships as a separate optional starter (`easy-paging-spring-boot-starter-reactive`) to keep the core JAR small.

---

## v1.0 — API freeze

After v0.x stabilizes and we have real user feedback in hand. v1.0 commits to API stability under SemVer — breaking changes only at major bumps.

### Multi-tenant context awareness

Hook into Spring Security / a tenant resolver to apply per-tenant defaults:

```yaml
easy-paging:
  tenants:
    tenant-a:
      max-page-size: 100
    tenant-b:
      max-page-size: 1000
```

- Per-tenant cursor secret rotation
- Per-tenant rate limit hooks (counters, not enforcement)

**Direct motivation**: any SaaS using the library with row-level multi-tenancy wants to cap export sizes per customer plan, not globally.

### Other pagination engine adapters

Today the engine is hard-wired to PageHelper. v1.0 abstracts the engine boundary so the same `@AutoPaginate` annotation can drive:

| Engine | For |
|---|---|
| **JPA Specification** | projects already on Spring Data JPA |
| **jOOQ** | type-safe SQL fans |
| **Plain JDBC** with `NamedParameterJdbcTemplate` | minimum-dependency setups |

The `@AutoPaginate` annotation stays the same; the consumer picks the engine via `easy-paging.engine: pagehelper | jpa | jooq | jdbc`.

---

## Considering — not committed

On the radar. Open an issue if you have strong opinions, or if any of these is the one thing blocking adoption for you.

| Idea | Why interesting | Why hesitating |
|---|---|---|
| Cursor expiration / TTL | Limits damage from stolen cursors | Adds complexity, most teams won't notice |
| Per-request cursor secret rotation | Defense-in-depth for high-security domains | Niche; existing HMAC already strong |
| GraphQL Relay-style connection pagination | Aligns with widely-used spec | Requires GraphQL stack on consumer side |
| Streaming export (chunked `Transfer-Encoding`) | Solves the "export 1M rows" use case | Different problem from pagination; might belong in a separate library |
| Built-in metrics (Micrometer) | Observability of pagination latency, query counts | Easy enough for users to add themselves |
| Native Spring Native / GraalVM support | Cloud-native deployment story | PageHelper's reflection patterns need verification |

---

## Out of scope

Things explicitly considered and decided against.

- **NoSQL pagination adapters** (Mongo, DynamoDB, Elasticsearch). These have idiomatic pagination patterns that don't translate well to PageHelper/JPA mental models. Better served by dedicated libraries.
- **Frontend pagination components**. This is a server-side library. Pair it with whatever client library makes sense.
- **Custom SQL dialect support** beyond what PageHelper already provides. PageHelper covers the common databases (MySQL, PostgreSQL, Oracle, SQL Server, H2, DB2, MariaDB, etc.); if yours isn't one of those, PageHelper is the right place to add it.

---

## How to influence the roadmap

- **Vote** on an item: :material-thumb-up: the relevant GitHub issue, or open one if it doesn't exist yet.
- **Use case beats opinion**: tell us *why* you need an item, with a concrete scenario. That makes prioritization easy.
- **PRs welcome** for any item under "Next minor" or "Considering" — drop a comment first so we can sketch the API together.

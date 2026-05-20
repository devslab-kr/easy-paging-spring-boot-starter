# Roadmap

A living document of what's planned for easy-paging. Items are loose commitments — concrete enough to set expectations, loose enough to react to user feedback. Open an issue if you want to nudge anything up or down the list.

For released features see [CHANGELOG.md](CHANGELOG.md).

---

## Recently shipped

| Release | Highlights |
|---|---|
| **v0.4.0** | Native R2DBC + WebFlux support via the new optional `easy-paging-spring-boot-starter-reactive` artifact. Gradle build migrated to multi-module structure. Testcontainers dialect-compat test layer (PostgreSQL + MySQL). |
| **v0.3.0** | Keyset reverse direction (`prevCursor` activated). Spring Boot baseline 3.3.5 → 3.5.3. PageHelper 2.1.1 + transitive MyBatis starter promoted to `api`. |

See [CHANGELOG.md](CHANGELOG.md) for the full notes.

---

## Next minor — TBD

Shape will be driven by post-0.4.0 user feedback. Candidates likely to land here come from the [Considering](#considering--not-committed) section — open an issue to vote on what you want next.

---

## v1.0 — API freeze

After v0.x stabilizes and we have real user feedback in hand. v1.0 commits to API stability under SemVer — breaking changes only at major bumps.

### Multi-tenant context awareness

Hook into Spring Security / a tenant resolver to apply per-tenant defaults:

- `easy-paging.tenants.tenant-a.max-page-size: 100`
- Per-tenant cursor secret rotation
- Per-tenant rate limit hooks (counters, not enforcement)

Direct motivation: any SaaS using the library with row-level multi-tenancy wants to cap export sizes per customer plan, not globally.

### Other pagination engine adapters

Today the engine is hard-wired to PageHelper. v1.0 abstracts the engine boundary so the same `@AutoPaginate` annotation can drive:

- **JPA Specification** — for projects already on Spring Data JPA
- **jOOQ** — for type-safe SQL fans
- **Plain JDBC** with `NamedParameterJdbcTemplate`

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

Things we have explicitly considered and decided against.

- **NoSQL pagination adapters** (Mongo, DynamoDB, Elasticsearch). These have idiomatic pagination patterns that don't translate well to PageHelper/JPA mental models. Better served by dedicated libraries.
- **Frontend pagination components**. This is a server-side library. Pair it with whatever client library makes sense.
- **Custom SQL dialect support** beyond what PageHelper already provides. PageHelper covers the common databases (MySQL, PostgreSQL, Oracle, SQL Server, H2, DB2, MariaDB, etc.); if yours isn't one of those, PageHelper is the right place to add it.

---

## How to influence the roadmap

- **Vote** on an item: 👍 the relevant GitHub issue, or open one if it doesn't exist yet.
- **Use case beats opinion**: tell us *why* you need an item, with a concrete scenario. That makes prioritization easy.
- **PRs welcome** for any item under "Next minor" or "Considering" — drop a comment first so we can sketch the API together.

# API Reference (Javadoc)

Each release publishes a `javadoc.jar` for both the core artifact and the optional reactive artifact. Two ways to read it:

## Online (javadoc.io)

[javadoc.io](https://javadoc.io/) mirrors Javadoc for every artifact on Maven Central — no setup required.

**Core (`easy-paging-spring-boot-starter`):**

- Latest: <https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest>
- Specific version: `https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/<version>` (e.g. [0.4.0](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/3.0.0))

**Reactive (`easy-paging-spring-boot-starter-reactive`, since 0.4.0):**

- Latest: <https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/latest>
- Specific version: `https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/<version>` (e.g. [0.4.0](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/3.0.0))

The first visit to a new version takes a moment while javadoc.io fetches the jar from Maven Central; subsequent visits are cached.

## In your IDE

Once you've added the dependency, IntelliJ IDEA, Eclipse, and VS Code (with Java extensions) all fetch the `javadoc.jar` from Maven Central automatically. Hover over any `@AutoPaginate`, `@KeysetPaginate`, `PageResponse`, `KeysetPage`, etc. to see the Javadoc inline.

If your IDE doesn't show docs, force a refresh:

=== "Gradle"

    ```bash
    ./gradlew dependencies --refresh-dependencies
    ```

=== "Maven"

    ```bash
    mvn dependency:resolve -Dclassifier=javadoc
    ```

## Key entry points

A short map of where to start when reading the API:

| Class | What it does |
|---|---|
| [`@AutoPaginate`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/annotation/AutoPaginate.html) | Controller-method annotation for offset pagination |
| [`@KeysetPaginate`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/annotation/KeysetPaginate.html) | Controller-method annotation for cursor pagination |
| [`PageResponse`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/core/PageResponse.html) | Offset response envelope |
| [`KeysetPage`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/core/KeysetPage.html) | Keyset response envelope |
| [`KeysetRequest`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/core/KeysetRequest.html) | Resolved cursor + page-size for a keyset request |
| [`CursorCodec`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/core/CursorCodec.html) | Encode/decode keyset cursors (HMAC-SHA256 signing) |
| [`PageResponseFactory`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/spi/PageResponseFactory.html) | SPI for replacing the default response envelope |
| [`ReactivePagingSupport`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/reactive/ReactivePagingSupport.html) | Reactor helper for blocking MyBatis calls (core artifact) |
| [`EasyPagingProperties`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/autoconfigure/EasyPagingProperties.html) | All `easy-paging.*` configuration keys |

For the optional reactive artifact (`easy-paging-spring-boot-starter-reactive`):

| Class | What it does |
|---|---|
| [`R2dbcOffsetPagingSupport`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/latest/kr/devslab/easypaging/r2dbc/R2dbcOffsetPagingSupport.html) | R2DBC offset/limit pagination → `Mono<PageResponse<T>>` |
| [`R2dbcKeysetSupport`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/latest/kr/devslab/easypaging/r2dbc/R2dbcKeysetSupport.html) | R2DBC keyset/cursor pagination — lexicographic `WHERE` builder + helper |
| [`ReactiveKeysetRequestArgumentResolver`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/latest/kr/devslab/easypaging/webflux/ReactiveKeysetRequestArgumentResolver.html) | WebFlux `KeysetRequest` argument resolver (auto-registered) |

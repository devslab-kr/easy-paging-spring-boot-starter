# Installation

## Choose your release line

easy-paging ships two parallel lines so apps don't have to upgrade Spring Boot just to use it:

| Spring Boot version | easy-paging line | Use |
| --- | --- | --- |
| **Spring Boot 4.0+** | **`0.5.x`** (active line on `main`) | `kr.devslab:easy-paging-spring-boot-starter:0.5.0` |
| **Spring Boot 3.3–3.5** | **`0.4.x`** ([maintenance branch](https://github.com/devslab-kr/easy-paging-spring-boot-starter/tree/0.4.x)) | `kr.devslab:easy-paging-spring-boot-starter:0.4.0` |

The public API surface (`@AutoPaginate`, `@KeysetPaginate`, `PageResponse<T>`, `KeysetPage<T>`, `R2dbcOffsetPagingSupport`, ...) is identical on both lines. Only the underlying runtime BOM differs.

The rest of this page covers the **`0.5.x` / Spring Boot 4** line — the recommended path for new apps.

## Requirements

- **Java 21+**
- **Spring Boot 4.0+** (built/tested against `4.0.6`)
- **Gradle 8.14+** if you build with Gradle (the SB4 plugin refuses older Gradle)
- A JDBC driver (your choice — `mysql-connector-j`, `postgresql`, `h2`, etc.)

## Adding the dependency

=== "Gradle (Kotlin DSL)"

    ```kotlin
    dependencies {
        implementation("kr.devslab:easy-paging-spring-boot-starter:0.5.0")
    }
    ```

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
        implementation 'kr.devslab:easy-paging-spring-boot-starter:0.5.0'
    }
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>kr.devslab</groupId>
        <artifactId>easy-paging-spring-boot-starter</artifactId>
        <version>0.5.0</version>
    </dependency>
    ```

## What the starter pulls in

The starter transitively brings these for you (versions managed by the Spring Boot 4 BOM):

- `spring-boot-starter-aspectj` (the aspect engine — renamed from `spring-boot-starter-aop` in SB4; same artifact contents, new ID)
- `spring-data-commons` (just `Pageable`, `Page`, `Sort` types — **not** Spring Data JPA)
- `pagehelper-spring-boot-starter` `4.0.0` (the underlying SQL rewriter, SB4-compatible release)
- `mybatis-spring-boot-starter` `4.0.1` (wires up `DataSource`, `SqlSessionFactory`, `@MapperScan`) — pinned by this library for conflict-resolution stability

## What you bring yourself

- A JDBC driver for your database

`spring-boot-starter-web` / `webflux` are also **not** transitive — bring whichever your app already uses (or neither, if you're using the starter from a non-HTTP context).

## Optional — reactive companion artifact

If your app uses Spring Data R2DBC + WebFlux instead of (or alongside) MyBatis, add the optional reactive starter:

```kotlin
dependencies {
    implementation("kr.devslab:easy-paging-spring-boot-starter:0.5.0")
    implementation("kr.devslab:easy-paging-spring-boot-starter-reactive:0.5.0")
}
```

You bring (same as a stock Spring Data R2DBC project): an R2DBC driver, `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`. The reactive starter pulls in the core starter transitively, so no double declaration needed if you only use R2DBC.

See the [Reactive guide](../guides/reactive.md#native-r2dbc--webflux) for the full helper API.

!!! tip "Need a different MyBatis line?"
    The MyBatis Spring Boot Starter version is pinned by this library so that any other transitive doesn't override the SB4-line version that PageHelper expects. If you need a different MyBatis line, exclude it and declare your own:

    ```kotlin
    implementation("kr.devslab:easy-paging-spring-boot-starter:0.5.0") {
        exclude(group = "org.mybatis.spring.boot")
    }
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:your.version")
    ```

## Verifying the setup

After adding the dependency, the starter auto-registers via Spring Boot's `AutoConfiguration` mechanism. A minimal proof:

```yaml title="application.yml"
easy-paging:
  enabled: true            # default true
  default-page-size: 20
  max-page-size: 500
```

If the starter is loaded, the `easy-paging.*` keys above will be syntax-highlighted in IntelliJ IDEA (configuration metadata is shipped in the jar) and Spring Boot will accept them at startup.

Continue to the [Tutorial](tutorial.md) for a 5-minute walkthrough of a paginated endpoint.

## Staying on Spring Boot 3.3–3.5?

The `0.4.x` maintenance line is the same code at the time of the SB4 split, kept on the SB3 BOM and receiving security patches. Use `0.4.0`:

```kotlin
dependencies {
    implementation("kr.devslab:easy-paging-spring-boot-starter:0.4.0")
}
```

Requirements for the `0.4.x` line: Java 21+, Spring Boot 3.3–3.5, Gradle 8.10+. Same API surface as `0.5.x` — your code doesn't change when the day comes to bump to SB4 + `0.5.x`.

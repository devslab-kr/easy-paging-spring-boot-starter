# Installation

## Requirements

- **Java 21+** (Spring Boot 3.5 baseline)
- **Spring Boot 3.3+** (we build/test against 3.5; 3.3 and 3.4 should still work but aren't covered by CI)
- A JDBC driver (your choice — `mysql-connector-j`, `postgresql`, `h2`, etc.)
- `mybatis-spring-boot-starter` 3.x

## Adding the dependency

=== "Gradle (Kotlin DSL)"

    ```kotlin
    dependencies {
        implementation("kr.devslab:easy-paging-spring-boot-starter:0.2.0")
    }
    ```

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
        implementation 'kr.devslab:easy-paging-spring-boot-starter:0.2.0'
    }
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>kr.devslab</groupId>
        <artifactId>easy-paging-spring-boot-starter</artifactId>
        <version>0.2.0</version>
    </dependency>
    ```

## What the starter pulls in

The starter transitively brings these for you:

- `spring-boot-starter-aop` (the aspect engine)
- `spring-data-commons` (just `Pageable`, `Page`, `Sort` types — **not** Spring Data JPA)
- `pagehelper-spring-boot-starter` (the underlying SQL rewriter)

## What you bring yourself

These are intentionally **not** transitive so your project owns the version:

- `mybatis-spring-boot-starter` — wires up `DataSource`, `SqlSessionFactory`, and `@MapperScan`
- A JDBC driver for your database

!!! tip "Why aren't these transitive?"
    Almost every MyBatis project already declares these explicitly with a chosen version. Forcing a version through this starter would cause conflicts. Same reasoning applies to `spring-boot-starter-web` / `webflux` — you bring what your app already uses.

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

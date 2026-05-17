# Installation

## Requirements

- **Java 21+** (Spring Boot 3.5 baseline)
- **Spring Boot 3.3+** (we build/test against 3.5; 3.3 and 3.4 should still work but aren't covered by CI)
- A JDBC driver (your choice — `mysql-connector-j`, `postgresql`, `h2`, etc.)

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
- `mybatis-spring-boot-starter` 3.x (wires up `DataSource`, `SqlSessionFactory`, `@MapperScan`) — pinned to the Spring Boot 3-compatible line because PageHelper itself still ships the older Boot 2.7 transitive

## What you bring yourself

- A JDBC driver for your database

`spring-boot-starter-web` / `webflux` are also **not** transitive — bring whichever your app already uses (or neither, if you're using the starter from a non-HTTP context).

!!! tip "Need a different MyBatis line?"
    The MyBatis Spring Boot Starter version is pinned by this library so that PageHelper's older transitive doesn't leak into your app. If you need a different MyBatis line, exclude it and declare your own:

    ```kotlin
    implementation("kr.devslab:easy-paging-spring-boot-starter:0.2.0") {
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

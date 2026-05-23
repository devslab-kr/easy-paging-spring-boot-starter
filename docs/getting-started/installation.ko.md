# 설치

## 요구사항

- **Java 21+** (Spring Boot 3.5 기준)
- **Spring Boot 3.3+** (빌드·테스트는 3.5 기준으로 진행. 3.3 / 3.4도 동작은 하지만 CI 검증 대상은 아님)
- JDBC 드라이버 (`mysql-connector-j`, `postgresql`, `h2` 등 본인 선택)

## 의존성 추가

=== "Gradle (Kotlin DSL)"

    ```kotlin
    dependencies {
        implementation("kr.devslab:easy-paging-spring-boot-starter:3.0.0")
    }
    ```

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
        implementation 'kr.devslab:easy-paging-spring-boot-starter:3.0.0'
    }
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>kr.devslab</groupId>
        <artifactId>easy-paging-spring-boot-starter</artifactId>
        <version>3.0.0</version>
    </dependency>
    ```

## 스타터가 자동으로 가져오는 것

스타터가 transitive로 다음을 가져옵니다:

- `spring-boot-starter-aop` (aspect 엔진)
- `spring-data-commons` (`Pageable`, `Page`, `Sort` 타입만 — **Spring Data JPA 아님**)
- `pagehelper-spring-boot-starter` (SQL 리라이팅 엔진)
- `mybatis-spring-boot-starter` 3.x (`DataSource`, `SqlSessionFactory`, `@MapperScan` 자동 설정) — PageHelper가 여전히 Boot 2.7 라인 transitive를 끌고 오기 때문에 Boot 3 호환 라인으로 우리 쪽에서 고정해서 제공합니다

## 본인이 추가해야 하는 것

- DB에 맞는 JDBC 드라이버

`spring-boot-starter-web` / `webflux`도 **transitive가 아닙니다** — 본인 앱이 쓰는 걸 그대로 사용하세요 (HTTP 컨텍스트가 아닌 곳에서 쓴다면 둘 다 안 넣어도 됩니다).

## 옵션 — reactive 동반 아티팩트

앱이 MyBatis 대신 (또는 함께) Spring Data R2DBC + WebFlux를 쓴다면 옵션 reactive 스타터 추가:

```kotlin
dependencies {
    implementation("kr.devslab:easy-paging-spring-boot-starter:3.0.0")
    implementation("kr.devslab:easy-paging-spring-boot-starter-reactive:3.0.0")
}
```

본인이 추가 (Spring Data R2DBC 프로젝트의 일반 셋업): R2DBC 드라이버, `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`. reactive 스타터가 core 스타터를 transitive로 가져오므로 R2DBC만 쓰면 core 라인은 생략 가능.

헬퍼 API 전체는 [Reactive 가이드](../guides/reactive.md#네이티브-r2dbc--webflux) 참조.

!!! tip "다른 MyBatis 라인이 필요하다면"
    PageHelper의 옛 transitive가 앱에 누출되지 않도록 MyBatis Spring Boot Starter 버전을 라이브러리가 고정합니다. 다른 MyBatis 라인이 필요하면 exclude 후 직접 선언:

    ```kotlin
    implementation("kr.devslab:easy-paging-spring-boot-starter:3.0.0") {
        exclude(group = "org.mybatis.spring.boot")
    }
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:원하는버전")
    ```

## 셋업 확인

의존성 추가 후, 스타터는 Spring Boot `AutoConfiguration` 메커니즘으로 자동 등록됩니다. 간단한 확인:

```yaml title="application.yml"
easy-paging:
  enabled: true            # 기본 true
  default-page-size: 20
  max-page-size: 500
```

스타터가 로드되면 IntelliJ IDEA에서 위 `easy-paging.*` 키들이 syntax highlighting되고(설정 메타데이터가 jar에 포함됨), Spring Boot도 정상적으로 인식합니다.

5분 안에 페이지네이션 엔드포인트를 만드는 실습은 [튜토리얼](tutorial.md)로.

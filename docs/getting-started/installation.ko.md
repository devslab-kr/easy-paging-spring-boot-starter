# 설치

## 요구사항

- **Java 21+** (Spring Boot 3.3 기준)
- **Spring Boot 3.3+**
- JDBC 드라이버 (`mysql-connector-j`, `postgresql`, `h2` 등 본인 선택)
- `mybatis-spring-boot-starter` 3.x

## 의존성 추가

=== "Gradle (Kotlin DSL)"

    ```kotlin
    dependencies {
        implementation("kr.devslab:easy-paging-spring-boot-starter:0.1.1")
    }
    ```

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
        implementation 'kr.devslab:easy-paging-spring-boot-starter:0.1.1'
    }
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>kr.devslab</groupId>
        <artifactId>easy-paging-spring-boot-starter</artifactId>
        <version>0.1.1</version>
    </dependency>
    ```

## 스타터가 자동으로 가져오는 것

스타터가 transitive로 다음을 가져옵니다:

- `spring-boot-starter-aop` (aspect 엔진)
- `spring-data-commons` (`Pageable`, `Page`, `Sort` 타입만 — **Spring Data JPA 아님**)
- `pagehelper-spring-boot-starter` (SQL 리라이팅 엔진)

## 본인이 추가해야 하는 것

다음은 의도적으로 transitive가 아닙니다 — 본인 프로젝트가 버전을 결정:

- `mybatis-spring-boot-starter` — `DataSource`, `SqlSessionFactory`, `@MapperScan` 자동 설정
- DB에 맞는 JDBC 드라이버

!!! tip "왜 transitive로 안 가져오나요?"
    거의 모든 MyBatis 프로젝트가 이미 명시적 버전으로 선언하고 있어서, 우리가 강제하면 충돌이 발생합니다. `spring-boot-starter-web` / `webflux`도 같은 이유 — 본인 프로젝트가 이미 쓰고 있는 걸 그대로 활용하라는 의도입니다.

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

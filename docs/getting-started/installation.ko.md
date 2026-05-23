# 설치

## 릴리스 라인 선택

easy-paging은 두 라인을 병행 운영해서, Spring Boot major를 올리지 않더라도 사용할 수 있도록 합니다:

| Spring Boot 버전 | easy-paging 라인 | 좌표 |
| --- | --- | --- |
| **Spring Boot 4.0+** | **`4.x.y`** (active 라인 — `main` 브랜치) | `kr.devslab:easy-paging-spring-boot-starter:4.0.0` |
| **Spring Boot 3.3–3.5** | **`3.x.y`** ([maintenance 브랜치](https://github.com/devslab-kr/easy-paging-spring-boot-starter/tree/3.x)) | `kr.devslab:easy-paging-spring-boot-starter:3.0.0` |

라이브러리 메이저 숫자는 타겟 Spring Boot 메이저와 일치 — [버전 정책](https://github.com/devslab-kr/.github/blob/main/.github/VERSIONING.md#한국어) 참조. 공개 API (`@AutoPaginate`, `@KeysetPaginate`, `PageResponse<T>`, `KeysetPage<T>`, `R2dbcOffsetPagingSupport`, ...)는 양쪽 라인 동일, 런타임 BOM만 다릅니다.

이 페이지의 나머지 내용은 **`4.x` / Spring Boot 4** 라인 기준 — 신규 앱이라면 이쪽 추천.

## 요구사항

- **Java 21+**
- **Spring Boot 4.0+** (빌드·테스트는 `4.0.6` 기준)
- **Gradle 8.14+** (Gradle을 쓴다면 — SB4 plugin이 그 이전 Gradle을 거부)
- JDBC 드라이버 (`mysql-connector-j`, `postgresql`, `h2` 등 본인 선택)

## 의존성 추가

=== "Gradle (Kotlin DSL)"

    ```kotlin
    dependencies {
        implementation("kr.devslab:easy-paging-spring-boot-starter:4.0.0")
    }
    ```

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
        implementation 'kr.devslab:easy-paging-spring-boot-starter:4.0.0'
    }
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>kr.devslab</groupId>
        <artifactId>easy-paging-spring-boot-starter</artifactId>
        <version>4.0.0</version>
    </dependency>
    ```

## 스타터가 자동으로 가져오는 것

스타터가 transitive로 다음을 가져옵니다 (버전은 Spring Boot 4 BOM이 관리):

- `spring-boot-starter-aspectj` (aspect 엔진 — SB4에서 `spring-boot-starter-aop`에서 이름이 변경됨, 내용은 동일)
- `spring-data-commons` (`Pageable`, `Page`, `Sort` 타입만 — **Spring Data JPA 아님**)
- `pagehelper-spring-boot-starter` `4.0.0` (SQL 리라이팅 엔진, SB4 호환 release)
- `mybatis-spring-boot-starter` `4.0.1` (`DataSource`, `SqlSessionFactory`, `@MapperScan` 자동 설정) — 충돌 해결 안정성을 위해 라이브러리에서 직접 pin

## 본인이 추가해야 하는 것

- DB에 맞는 JDBC 드라이버

`spring-boot-starter-web` / `webflux`도 **transitive가 아닙니다** — 본인 앱이 쓰는 걸 그대로 사용 (HTTP 컨텍스트가 아닌 곳에서 쓴다면 둘 다 안 넣어도 됩니다).

## 옵션 — reactive 동반 아티팩트

앱이 MyBatis 대신 (또는 함께) Spring Data R2DBC + WebFlux를 쓴다면 옵션 reactive 스타터 추가:

```kotlin
dependencies {
    implementation("kr.devslab:easy-paging-spring-boot-starter:4.0.0")
    implementation("kr.devslab:easy-paging-spring-boot-starter-reactive:4.0.0")
}
```

본인이 추가 (Spring Data R2DBC 프로젝트의 일반 셋업): R2DBC 드라이버, `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`. reactive 스타터가 core 스타터를 transitive로 가져오므로 R2DBC만 쓰면 core 라인은 생략 가능.

헬퍼 API 전체는 [Reactive 가이드](../guides/reactive.md#네이티브-r2dbc--webflux) 참조.

!!! tip "다른 MyBatis 라인이 필요하다면"
    SB4 라인 PageHelper가 기대하는 MyBatis 버전이 다른 transitive에 의해 덮이지 않도록 라이브러리가 직접 pin합니다. 다른 MyBatis 라인이 필요하면 exclude 후 직접 선언:

    ```kotlin
    implementation("kr.devslab:easy-paging-spring-boot-starter:4.0.0") {
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

## Spring Boot 3.3–3.5 사용 중이라면?

`3.x` maintenance 라인은 SB4 분기 시점의 동일한 코드를 SB3 BOM 기준으로 유지하면서 보안 패치를 계속 받습니다. `3.0.0` 사용:

```kotlin
dependencies {
    implementation("kr.devslab:easy-paging-spring-boot-starter:3.0.0")
}
```

`3.x` 라인 요구사항: Java 21+, Spring Boot 3.3–3.5, Gradle 8.10+. API 표면은 `4.x`와 동일 — 추후 SB4 + `4.x`로 올릴 때 코드 변경 없음.
